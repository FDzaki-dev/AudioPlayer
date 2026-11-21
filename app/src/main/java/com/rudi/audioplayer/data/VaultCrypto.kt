package com.rudi.audioplayer.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Batch 596 — INTI enkripsi biner untuk Vault (adaptasi paket refactor user "Vault AES-256 GCM + Android Keystore").
 * BELUM terhubung ke alur Vault/pemutaran (itu Batch berikutnya, lihat PROJECT_STATE.md): batch ini hanya
 * menyediakan mesin enkripsinya + tes JVM, tanpa mengubah perilaku aplikasi.
 *
 * KENAPA BUKAN `Cipher` GCM satu-aliran seperti di paket usulan: (1) satu pesan GCM tidak bisa di-seek — pemutar
 * butuh seek; (2) setahu saya GCM-decrypt di AndroidKeyStore menahan SEMUA keluaran sampai `doFinal` sehingga berkas
 * FLAC ratusan MB menumpuk di heap (belum diuji di device); (3) tag GCM baru diverifikasi di akhir. Maka dipakai
 * pola AEAD berpotongan (chunked) seperti Tink/STREAM:
 *
 *  - Kunci master AES-256 hidup di Android Keystore (tak pernah keluar), HANYA dipakai membungkus kunci data
 *    per-berkas (1 operasi Keystore per berkas, bukan per potongan -> cepat).
 *  - Kunci data AES-256 acak per berkas membungkus isi di perangkat lunak (Conscrypt), potongan [DEFAULT_CHUNK_BYTES]
 *    masing-masing AES-256-GCM, nonce 12 byte = 8 byte acak per-berkas + indeks potongan 4 byte.
 *  - AAD tiap potongan = seluruh header + penanda "potongan terakhir": mengubah header, menukar urutan, atau
 *    memotong ekor berkas membuat tag gagal (terdeteksi), tanpa perlu menyimpan panjang di header.
 *  - Akses acak: potongan ke-i ada di offset tetap -> [Reader] mendekripsi hanya potongan yang diminta (dasar
 *    DataSource Media3 yang bisa seek di batch pemutaran nanti), tanpa berkas plaintext di disk.
 *
 * Format berkas: `"SNXVLT01"`(8) | chunkSize Int BE (4) | noncePrefix (8) | wrappedKeyLen UShort BE (2) | wrappedKey |
 * potongan[ ciphertext + tag 16 byte ]... Panjang plaintext diturunkan dari ukuran berkas.
 */
interface VaultMasterKey {
    /** Membungkus [dataKey] (rahasia) -> bytes bungkusan (IV + ciphertext + tag). */
    fun wrap(dataKey: ByteArray): ByteArray

    /** Membuka bungkusan hasil [wrap]; melempar [java.security.GeneralSecurityException] bila kunci salah/rusak. */
    fun unwrap(wrapped: ByteArray): ByteArray
}

/** Kegagalan enkripsi/dekripsi Vault: berkas rusak/diubah/terpotong, bukan berkas brankas, atau kunci tak cocok. */
class VaultCryptoException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Kunci master di Android Keystore (AES-256-GCM, tak bisa diekspor). Dibuat malas saat pertama dipakai —
 *  panggil dari thread IO. Catatan: kunci TIDAK terikat otentikasi pengguna; PIN/biometrik Vault tetap gerbang UI. */
class KeystoreVaultMasterKey(private val alias: String = DEFAULT_ALIAS) : VaultMasterKey {

    override fun wrap(dataKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return cipher.iv + cipher.doFinal(dataKey)
    }

    override fun unwrap(wrapped: ByteArray): ByteArray {
        if (wrapped.size <= GCM_IV_BYTES) throw VaultCryptoException("Kunci data berkas brankas tidak valid")
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, wrapped, 0, GCM_IV_BYTES))
        return cipher.doFinal(wrapped, GCM_IV_BYTES, wrapped.size - GCM_IV_BYTES)
    }

    @Synchronized
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER)
        keyStore.load(null)
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "audioplayer_vault_master_v1"
        private const val PROVIDER = "AndroidKeyStore"
    }
}

/** Sidik jari plaintext (panjang + SHA-256 heksa) — dipakai alur Vault untuk MEMVERIFIKASI hasil enkripsi
 *  (dekripsi lalu bandingkan) sebelum berkas asli boleh disentuh. */
data class VaultFingerprint(val length: Long, val sha256: String)

class VaultCrypto(
    private val masterKey: VaultMasterKey,
    private val chunkSize: Int = DEFAULT_CHUNK_BYTES
) {
    init {
        require(chunkSize in MIN_CHUNK_BYTES..MAX_CHUNK_BYTES) { "chunkSize di luar batas: $chunkSize" }
    }

    /** Mengenkripsi seluruh [input] ke [output] (tidak menutup keduanya). Panjang input tak perlu diketahui. */
    fun encrypt(input: InputStream, output: OutputStream): VaultFingerprint {
        val dataKey = ByteArray(DATA_KEY_BYTES)
        RANDOM.nextBytes(dataKey)
        try {
            val prefix = ByteArray(NONCE_PREFIX_BYTES)
            RANDOM.nextBytes(prefix)
            val header = buildHeader(chunkSize, prefix, wrapKey(dataKey))
            output.write(header)
            val key = SecretKeySpec(dataKey, "AES")
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var current = readFully(input, chunkSize)
            var index = 0
            while (true) {
                // Baca satu potongan ke depan: hanya dengan begitu potongan terakhir bisa ditandai saat disegel.
                val next = if (current.size == chunkSize) readFully(input, chunkSize) else EMPTY
                val last = next.isEmpty()
                digest.update(current)
                total += current.size
                output.write(seal(key, header, prefix, index, last, current))
                if (last) break
                current = next
                index++
            }
            return VaultFingerprint(total, toHex(digest.digest()))
        } finally {
            dataKey.fill(0)
        }
    }

    /** Mendekripsi seluruh [input] ke [output] (tidak menutup keduanya); melempar [VaultCryptoException] bila
     *  berkas rusak/diubah/terpotong atau kunci tak cocok. Keluaran SEBAGIAN mungkin sudah tertulis saat gagal. */
    fun decrypt(input: InputStream, output: OutputStream): VaultFingerprint {
        val header = readHeader(input)
        val dataKey = unwrapKey(header.wrappedKey)
        try {
            val key = SecretKeySpec(dataKey, "AES")
            val digest = MessageDigest.getInstance("SHA-256")
            val stride = header.chunkSize + GCM_TAG_BYTES
            var total = 0L
            var current = readFully(input, stride)
            if (current.size < GCM_TAG_BYTES) throw VaultCryptoException("Berkas brankas terpotong")
            var index = 0
            while (true) {
                val next = if (current.size == stride) readFully(input, stride) else EMPTY
                val last = next.isEmpty()
                val plain = unseal(key, header, index, last, current)
                digest.update(plain)
                total += plain.size
                output.write(plain)
                if (last) break
                current = next
                index++
            }
            return VaultFingerprint(total, toHex(digest.digest()))
        } finally {
            dataKey.fill(0)
        }
    }

    /** Membuka [file] untuk baca acak (seek) tanpa menulis plaintext ke disk. Tutup [Reader] setelah selesai. */
    fun openReader(file: File): Reader {
        val header = FileInputStream(file).use { readHeader(it) }
        val body = file.length() - header.bytes.size
        val stride = header.chunkSize.toLong() + GCM_TAG_BYTES
        if (body < GCM_TAG_BYTES) throw VaultCryptoException("Berkas brankas terpotong")
        val chunkCount = (body + stride - 1) / stride
        val lastCipherLen = body - (chunkCount - 1) * stride
        if (lastCipherLen < GCM_TAG_BYTES) throw VaultCryptoException("Berkas brankas terpotong")
        val plainLength = (chunkCount - 1) * header.chunkSize + (lastCipherLen - GCM_TAG_BYTES)
        val dataKey = unwrapKey(header.wrappedKey)
        val key = SecretKeySpec(dataKey, "AES")
        dataKey.fill(0)
        return Reader(RandomAccessFile(file, "r"), header, key, stride, chunkCount, lastCipherLen, plainLength)
    }

    private fun wrapKey(dataKey: ByteArray): ByteArray = try {
        masterKey.wrap(dataKey)
    } catch (e: GeneralSecurityException) {
        throw VaultCryptoException("Kunci brankas tidak dapat dipakai (Keystore)", e)
    } catch (e: ProviderException) {
        throw VaultCryptoException("Kunci brankas tidak dapat dipakai (Keystore)", e)
    }

    private fun unwrapKey(wrapped: ByteArray): ByteArray = try {
        masterKey.unwrap(wrapped)
    } catch (e: VaultCryptoException) {
        throw e
    } catch (e: GeneralSecurityException) {
        throw VaultCryptoException("Kunci brankas tidak cocok dengan berkas ini (kunci hilang/berubah?)", e)
    } catch (e: ProviderException) {
        throw VaultCryptoException("Kunci brankas tidak dapat dipakai (Keystore)", e)
    }

    /** Akses acak ke plaintext satu berkas brankas. TIDAK dirancang untuk dipakai bersamaan dari banyak thread
     *  tanpa sinkronisasi — [read] sudah `@Synchronized`. Menyimpan satu potongan terdekripsi di memori (cache). */
    class Reader internal constructor(
        private val file: RandomAccessFile,
        private val header: Header,
        private val key: SecretKey,
        private val stride: Long,
        private val chunkCount: Long,
        private val lastCipherLen: Long,
        val plainLength: Long
    ) : Closeable {
        private var cachedIndex = -1L
        private var cached: ByteArray = EMPTY

        /** Mengisi [dst] mulai [offset] sebanyak maksimal [length] byte dari posisi plaintext [position];
         *  mengembalikan jumlah byte terbaca, atau -1 bila [position] sudah di luar akhir berkas. */
        @Synchronized
        fun read(position: Long, dst: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position < 0 || position >= plainLength) return -1
            val index = position / header.chunkSize
            val chunk = chunkAt(index)
            val inChunk = (position - index * header.chunkSize).toInt()
            val count = minOf(length, chunk.size - inChunk)
            System.arraycopy(chunk, inChunk, dst, offset, count)
            return count
        }

        private fun chunkAt(index: Long): ByteArray {
            if (index == cachedIndex) return cached
            val last = index == chunkCount - 1
            val sealed = ByteArray((if (last) lastCipherLen else stride).toInt())
            file.seek(header.bytes.size + index * stride)
            file.readFully(sealed)
            val plain = unseal(key, header, index.toInt(), last, sealed)
            cached = plain
            cachedIndex = index
            return plain
        }

        override fun close() {
            cached = EMPTY
            cachedIndex = -1L
            file.close()
        }
    }

    internal class Header(
        val bytes: ByteArray,
        val chunkSize: Int,
        val prefix: ByteArray,
        val wrappedKey: ByteArray
    )

    companion object {
        const val DEFAULT_CHUNK_BYTES = 64 * 1024
        const val MIN_CHUNK_BYTES = 16
        const val MAX_CHUNK_BYTES = 4 * 1024 * 1024

        private val MAGIC = "SNXVLT01".toByteArray(Charsets.US_ASCII)
        private const val NONCE_PREFIX_BYTES = 8
        private const val DATA_KEY_BYTES = 32
        private const val FIXED_HEADER_BYTES = 8 + 4 + NONCE_PREFIX_BYTES + 2
        private const val MAX_WRAPPED_KEY_BYTES = 512
        internal val EMPTY = ByteArray(0)
        private val FLAG_LAST = byteArrayOf(1)
        private val FLAG_MORE = byteArrayOf(0)
        private val RANDOM = SecureRandom()
        private const val HEX = "0123456789abcdef"

        private fun buildHeader(chunkSize: Int, prefix: ByteArray, wrapped: ByteArray): ByteArray {
            require(wrapped.size in 1..MAX_WRAPPED_KEY_BYTES) { "Ukuran kunci terbungkus tidak valid: ${wrapped.size}" }
            return ByteBuffer.allocate(FIXED_HEADER_BYTES + wrapped.size)
                .put(MAGIC)
                .putInt(chunkSize)
                .put(prefix)
                .putShort(wrapped.size.toShort())
                .put(wrapped)
                .array()
        }

        private fun readHeader(input: InputStream): Header {
            val fixed = readFully(input, FIXED_HEADER_BYTES)
            if (fixed.size < FIXED_HEADER_BYTES) throw VaultCryptoException("Berkas brankas terlalu pendek")
            val buffer = ByteBuffer.wrap(fixed)
            val magic = ByteArray(MAGIC.size)
            buffer.get(magic)
            if (!magic.contentEquals(MAGIC)) throw VaultCryptoException("Bukan berkas brankas yang dikenal")
            val chunkSize = buffer.int
            if (chunkSize !in MIN_CHUNK_BYTES..MAX_CHUNK_BYTES) throw VaultCryptoException("Header brankas tidak valid")
            val prefix = ByteArray(NONCE_PREFIX_BYTES)
            buffer.get(prefix)
            val wrappedLen = buffer.short.toInt() and 0xFFFF
            if (wrappedLen < 1 || wrappedLen > MAX_WRAPPED_KEY_BYTES) throw VaultCryptoException("Header brankas tidak valid")
            val wrapped = readFully(input, wrappedLen)
            if (wrapped.size < wrappedLen) throw VaultCryptoException("Berkas brankas terpotong")
            return Header(fixed + wrapped, chunkSize, prefix, wrapped)
        }

        private fun chunkNonce(prefix: ByteArray, index: Int): ByteArray =
            ByteBuffer.allocate(NONCE_PREFIX_BYTES + 4).put(prefix).putInt(index).array()

        private fun seal(
            key: SecretKey,
            header: ByteArray,
            prefix: ByteArray,
            index: Int,
            last: Boolean,
            plain: ByteArray
        ): ByteArray {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, chunkNonce(prefix, index)))
            cipher.updateAAD(header)
            cipher.updateAAD(if (last) FLAG_LAST else FLAG_MORE)
            return cipher.doFinal(plain)
        }

        internal fun unseal(key: SecretKey, header: Header, index: Int, last: Boolean, sealed: ByteArray): ByteArray = try {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, chunkNonce(header.prefix, index)))
            cipher.updateAAD(header.bytes)
            cipher.updateAAD(if (last) FLAG_LAST else FLAG_MORE)
            cipher.doFinal(sealed)
        } catch (e: GeneralSecurityException) {
            throw VaultCryptoException("Berkas brankas rusak atau telah diubah (potongan ke-$index)", e)
        }

        /** Membaca sampai [size] byte (atau akhir aliran); hasil lebih pendek dari [size] = akhir aliran tercapai. */
        private fun readFully(input: InputStream, size: Int): ByteArray {
            val buffer = ByteArray(size)
            var filled = 0
            while (filled < size) {
                val n = input.read(buffer, filled, size - filled)
                if (n < 0) break
                filled += n
            }
            return if (filled == size) buffer else buffer.copyOf(filled)
        }

        private fun toHex(bytes: ByteArray): String {
            val sb = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                val v = b.toInt()
                sb.append(HEX[(v shr 4) and 0xF]).append(HEX[v and 0xF])
            }
            return sb.toString()
        }
    }
}

private const val AES_GCM = "AES/GCM/NoPadding"
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BYTES = 16
private const val GCM_TAG_BITS = GCM_TAG_BYTES * 8
