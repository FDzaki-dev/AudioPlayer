package com.rudi.audioplayer.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultCryptoTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Pengganti Keystore untuk JVM: format bungkusan sama (IV 12 byte + ciphertext + tag). */
    private class SoftwareMasterKey(secret: ByteArray) : VaultMasterKey {
        private val key = SecretKeySpec(secret, "AES")
        private val random = SecureRandom()

        override fun wrap(dataKey: ByteArray): ByteArray {
            val iv = ByteArray(12)
            random.nextBytes(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            return iv + cipher.doFinal(dataKey)
        }

        override fun unwrap(wrapped: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, wrapped, 0, 12))
            return cipher.doFinal(wrapped, 12, wrapped.size - 12)
        }
    }

    private fun masterKey(seed: Int = 1) = SoftwareMasterKey(ByteArray(32) { (it + seed).toByte() })

    private fun crypto(chunk: Int = 16, seed: Int = 1) = VaultCrypto(masterKey(seed), chunk)

    private fun randomBytes(size: Int, seed: Long = 42L): ByteArray {
        val data = ByteArray(size)
        java.util.Random(seed).nextBytes(data)
        return data
    }

    private fun encrypted(crypto: VaultCrypto, plain: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        crypto.encrypt(ByteArrayInputStream(plain), out)
        return out.toByteArray()
    }

    private fun decrypted(crypto: VaultCrypto, sealed: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        crypto.decrypt(ByteArrayInputStream(sealed), out)
        return out.toByteArray()
    }

    @Test
    fun `round trip berbagai ukuran termasuk batas potongan dan kosong`() {
        val crypto = crypto(chunk = 16)
        for (size in listOf(0, 1, 15, 16, 17, 31, 32, 33, 1000)) {
            val plain = randomBytes(size, seed = size.toLong())
            assertArrayEquals("ukuran $size", plain, decrypted(crypto, encrypted(crypto, plain)))
        }
    }

    @Test
    fun `round trip dengan ukuran potongan bawaan 64 KiB di atas satu potongan`() {
        val crypto = VaultCrypto(masterKey())
        val plain = randomBytes(VaultCrypto.DEFAULT_CHUNK_BYTES * 2 + 123)

        assertArrayEquals(plain, decrypted(crypto, encrypted(crypto, plain)))
    }

    @Test
    fun `sidik jari enkripsi dan dekripsi sama dan cocok dengan panjang asli`() {
        val crypto = crypto()
        val plain = randomBytes(500)
        val sealed = ByteArrayOutputStream()

        val fromEncrypt = crypto.encrypt(ByteArrayInputStream(plain), sealed)
        val fromDecrypt = crypto.decrypt(ByteArrayInputStream(sealed.toByteArray()), ByteArrayOutputStream())

        assertEquals(500L, fromEncrypt.length)
        assertEquals(fromEncrypt, fromDecrypt)
        assertEquals(64, fromEncrypt.sha256.length)
    }

    @Test
    fun `isi terenkripsi tidak mengandung plaintext dan dua enkripsi berbeda`() {
        val crypto = crypto(chunk = 64)
        val plain = ByteArray(256) { 'A'.code.toByte() }

        val first = encrypted(crypto, plain)
        val second = encrypted(crypto, plain)

        assertFalse(String(first, Charsets.ISO_8859_1).contains("A".repeat(16)))
        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun `kunci master berbeda gagal dibuka`() {
        val sealed = encrypted(crypto(seed = 1), randomBytes(100))

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto(seed = 2), sealed) }
    }

    @Test
    fun `satu byte isi diubah terdeteksi`() {
        val crypto = crypto()
        val sealed = encrypted(crypto, randomBytes(200))
        sealed[sealed.size - 20] = (sealed[sealed.size - 20].toInt() xor 0x01).toByte()

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, sealed) }
    }

    @Test
    fun `header diubah terdeteksi`() {
        val crypto = crypto()
        val sealed = encrypted(crypto, randomBytes(200))
        sealed[14] = (sealed[14].toInt() xor 0x01).toByte() // byte noncePrefix

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, sealed) }
    }

    @Test
    fun `berkas dipotong di batas potongan terdeteksi`() {
        val crypto = crypto(chunk = 16)
        val sealed = encrypted(crypto, randomBytes(16 * 5))
        val truncated = sealed.copyOf(sealed.size - (16 + 16)) // buang potongan terakhir utuh

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, truncated) }
    }

    @Test
    fun `potongan ditukar urutannya terdeteksi`() {
        val crypto = crypto(chunk = 16)
        val plain = randomBytes(16 * 4)
        val sealed = encrypted(crypto, plain)
        val stride = 16 + 16
        val headerLen = sealed.size - 4 * stride
        val swapped = sealed.copyOf()
        System.arraycopy(sealed, headerLen, swapped, headerLen + stride, stride)
        System.arraycopy(sealed, headerLen + stride, swapped, headerLen, stride)

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, swapped) }
    }

    @Test
    fun `ekor sampah ditambahkan terdeteksi dan bukan berkas brankas ditolak`() {
        val crypto = crypto()
        val sealed = encrypted(crypto, randomBytes(100))

        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, sealed + ByteArray(7) { 9 }) }
        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, ByteArray(50) { 1 }) }
        assertThrows(VaultCryptoException::class.java) { decrypted(crypto, ByteArray(3)) }
    }

    @Test
    fun `reader membaca acak sama dengan plaintext termasuk lintas potongan`() {
        val crypto = crypto(chunk = 32)
        val plain = randomBytes(1000)
        val file = tmp.newFile("a.vlt").also { it.writeBytes(encrypted(crypto, plain)) }

        crypto.openReader(file).use { reader ->
            assertEquals(1000L, reader.plainLength)
            val rnd = java.util.Random(7)
            repeat(200) {
                val position = rnd.nextInt(1000)
                val length = 1 + rnd.nextInt(100)
                val buffer = ByteArray(length)
                val got = reader.read(position.toLong(), buffer, 0, length)
                assertTrue(got > 0)
                assertArrayEquals(plain.copyOfRange(position, position + got), buffer.copyOf(got))
            }
        }
    }

    @Test
    fun `reader membaca berurutan seluruh berkas dan -1 setelah akhir`() {
        val crypto = crypto(chunk = 16)
        val plain = randomBytes(77)
        val file = tmp.newFile("b.vlt").also { it.writeBytes(encrypted(crypto, plain)) }

        crypto.openReader(file).use { reader ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(10)
            var position = 0L
            while (true) {
                val n = reader.read(position, buffer, 0, buffer.size)
                if (n < 0) break
                out.write(buffer, 0, n)
                position += n
            }
            assertArrayEquals(plain, out.toByteArray())
            assertEquals(-1, reader.read(77L, buffer, 0, 1))
            assertEquals(-1, reader.read(-1L, buffer, 0, 1))
        }
    }

    @Test
    fun `reader pada berkas kosong dan pada berkas terpotong`() {
        val crypto = crypto(chunk = 16)
        val empty = tmp.newFile("c.vlt").also { it.writeBytes(encrypted(crypto, ByteArray(0))) }
        crypto.openReader(empty).use { reader ->
            assertEquals(0L, reader.plainLength)
            assertEquals(-1, reader.read(0L, ByteArray(4), 0, 4))
        }

        val sealed = encrypted(crypto, randomBytes(100))
        val cut = tmp.newFile("d.vlt").also { it.writeBytes(sealed.copyOf(sealed.size - 5)) }
        assertThrows(VaultCryptoException::class.java) {
            crypto.openReader(cut).use { reader -> reader.read(96L, ByteArray(4), 0, 4) }
        }
    }

    @Test
    fun `reader mendeteksi isi yang diubah saat potongan itu dibaca`() {
        val crypto = crypto(chunk = 16)
        val sealed = encrypted(crypto, randomBytes(100))
        sealed[sealed.size - 3] = (sealed[sealed.size - 3].toInt() xor 0x01).toByte()
        val file: File = tmp.newFile("e.vlt").also { it.writeBytes(sealed) }

        crypto.openReader(file).use { reader ->
            assertThrows(VaultCryptoException::class.java) { reader.read(99L, ByteArray(1), 0, 1) }
        }
    }
}
