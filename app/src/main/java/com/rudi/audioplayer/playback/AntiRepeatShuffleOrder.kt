package com.rudi.audioplayer.playback

import androidx.media3.common.C
import androidx.media3.exoplayer.source.ShuffleOrder
import kotlin.random.Random

/**
 * Batch 498 — Gap #3 roadmap (shuffle anti-repeat-nearby). Jawaban eksplisit user (diminta
 * dulu sebelum coding, 0 diasumsikan):
 *  1. "Anti-repeat-nearby" = KEDUA makna sekaligus — (a) lagu yang SAMA PERSIS tidak boleh
 *     bersebelahan langsung dalam urutan shuffle, DAN (b) lagu yang baru saja diputar (window
 *     "baru-baru diputar" dari [com.rudi.audioplayer.data.PlayStatsStore]) tidak boleh muncul
 *     lagi terlalu cepat di urutan yang sama.
 *  2. Ukuran window "baru-baru diputar" diserahkan ke penilaian terbaik (user: "sesuai
 *     preferensi terbaik aja") — lihat [AntiRepeatShuffleOrder.windowSizeFor]: skala mengikuti
 *     ukuran antrean, dibatasi 5..25 lagu, 0 (off) kalau antrean terlalu pendek utk bermakna.
 *  3. Berlaku di KEDUA jalur shuffle yang ada di project ini (tombol Shuffle di player —
 *     `PlayerViewModel.toggleShuffle()` — dan "Shuffle All" satu-tap dari Home —
 *     `PlayerViewModel.shuffleAll()`). Keduanya sama-sama cuma flip `Player.shuffleModeEnabled`
 *     di controller yang sama, jadi diterapkan lewat SATU listener reaktif di
 *     `PlaybackService.onCreate()` (lihat `onShuffleModeEnabledChanged`), bukan disentuh
 *     terpisah 2x di `PlayerViewModel.kt` — 0 perubahan diperlukan di file itu.
 *
 * Kenapa custom [ShuffleOrder] (bukan cuma acak ulang `List<Song>` lalu `setMediaItems()`):
 * Media3 sendiri, begitu `shuffleModeEnabled = true`, SELALU memutuskan next/previous/auto-
 * advance lewat `ShuffleOrder` internalnya sendiri (`DefaultShuffleOrder`, murni acak, 0
 * memori lagu apa yang barusan diputar) — urutan `MediaItem` yang kita set lewat
 * `setMediaItems()` sendiri TIDAK dipakai untuk traversal selama shuffle ON, cuma untuk
 * urutan "linear" kalau shuffle OFF. Satu-satunya cara constraint ini benar-benar berlaku ke
 * next/previous/auto-advance nyata (bukan cuma kosmetik item pertama) adalah mengganti
 * `ShuffleOrder`-nya sendiri lewat `ExoPlayer.setShuffleOrder()` — API ExoPlayer inti yang
 * sudah ada sejak sebelum project ini pin media3 1.3.1 pun (jauh lebih tua & stabil dibanding
 * `setMaxSeekToPreviousPositionMs` yang sempat gagal kompilasi Batch 493 — bukan kelas
 * kesalahan yang sama), makanya dipakai langsung TANPA perlu bump versi/konfirmasi user lagi.
 *
 * `cloneAndInsert`/`cloneAndRemove` WAJIB benar (dipanggil OTOMATIS oleh ExoPlayer sendiri
 * setiap `addMediaItem(s)`/`removeMediaItem`/`moveMediaItem` dipanggil selagi shuffle ON, yang
 * di project ini artinya "Play Next"/"Tambah ke Antrean"/reorder queue manual — SEMUA jalur
 * itu TETAP harus jalan tanpa crash walau shuffle sedang aktif, lihat PlayerViewModel.kt baris
 * addMediaItem/removeMediaItem/moveMediaItem). Strategi di sini SENGAJA paling sederhana yang
 * masih benar: item baru ditaruh di EKOR urutan shuffle (bukan disisip acak seperti
 * `DefaultShuffleOrder` asli) — behavior valid, 0 risiko off-by-one dari logika sisip acak.
 *
 * NOT VERIFIED — 0 CI/device fisik sesi ini (lihat PROJECT_STATE.md § RESUME POINT Batch 498).
 *
 * Batch 717 — perburuan bug algoritma pemutaran. 3 cacat nyata di urutan shuffle:
 *  1. Antrean DIGANTI selagi Shuffle ON (`setMediaItems`): ExoPlayer mengosongkan lalu menyisipkan lewat
 *     [cloneAndInsert] ke urutan kosong -> dulu hasilnya identitas 0,1,2,... (Shuffle ON tetapi berurutan). Kini
 *     urutan kosong yang diisi = antrean baru: permutasi acak + [onQueueReplaced] supaya pemilik menyusun ulang
 *     dengan lagu aktif sebagai jangkar.
 *  2. Lagu aktif bisa berada di posisi acak mana pun dalam permutasi; next/auto-advance hanya menjelajah posisi
 *     SETELAH-nya, jadi lagu di posisi sebelumnya tak terjangkau (Repeat OFF: antrean habis lebih awal). Lihat
 *     parameter `anchorIndex` pada [buildAntiRepeatNearbyOrder]: lagu aktif dipaku di posisi pertama.
 *  3. "Putar Berikutnya" (sisip tepat setelah lagu aktif) dulu masuk EKOR urutan Shuffle -> dimainkan paling
 *     akhir. Kini sisipan di `indeksAktif + 1` (bukan penambahan di ujung antrean) ditaruh tepat setelah lagu
 *     aktif di urutan Shuffle. [currentIndexProvider] dibaca di thread aplikasi pemain (ExoPlayer memanggil
 *     clone di thread itu); gagal baca = jatuh ke perilaku lama (ekor). Batas: "Putar Berikutnya" ketika lagu
 *     aktif adalah indeks TERAKHIR antrean sama dengan penambahan di ujung, tetap masuk ekor.
 */
class AntiRepeatShuffleOrder(
    private val shuffled: IntArray,
    private val currentIndexProvider: () -> Int = { C.INDEX_UNSET },
    private val onQueueReplaced: () -> Unit = {},
    private val random: Random = Random.Default
) : ShuffleOrder {

    private fun derive(order: IntArray): AntiRepeatShuffleOrder =
        AntiRepeatShuffleOrder(order, currentIndexProvider, onQueueReplaced, random)

    // Reverse lookup: original media-item index -> posisinya di dalam [shuffled]. O(1) utk
    // getNextIndex/getPreviousIndex, sama pola dgn ExoPlayer's own DefaultShuffleOrder.
    private val positionOf = IntArray(shuffled.size).also { arr ->
        for (position in shuffled.indices) arr[shuffled[position]] = position
    }

    override fun getLength(): Int = shuffled.size

    // FIX log_fail_478: interface ShuffleOrder media3 1.10.1 HANYA 1 parameter (index) — 0
    // repeatMode di signature ini (wrap REPEAT_MODE_ALL ditangani ExoPlayer sendiri lewat
    // getFirstIndex()/getLastIndex() saat traversal kena INDEX_UNSET, bukan tanggung jawab
    // ShuffleOrder). Signature lama (index, repeatMode) = asumsi salah, 0 pernah match interface
    // asli -> "overrides nothing". Behavior WRAP tetap sama persis (getFirstIndex/getLastIndex
    // sudah benar di bawah, ExoPlayer yang panggil itu saat REPEAT_MODE_ALL aktif).
    override fun getNextIndex(index: Int): Int {
        if (index < 0 || index >= positionOf.size) return C.INDEX_UNSET
        val position = positionOf[index]
        return if (position + 1 < shuffled.size) shuffled[position + 1] else C.INDEX_UNSET
    }

    override fun getPreviousIndex(index: Int): Int {
        if (index < 0 || index >= positionOf.size) return C.INDEX_UNSET
        val position = positionOf[index]
        return if (position - 1 >= 0) shuffled[position - 1] else C.INDEX_UNSET
    }

    override fun getLastIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[shuffled.size - 1]

    override fun getFirstIndex(): Int = if (shuffled.isEmpty()) C.INDEX_UNSET else shuffled[0]

    override fun cloneAndInsert(insertionIndex: Int, insertionCount: Int): ShuffleOrder {
        if (insertionCount <= 0) return this
        if (shuffled.isEmpty()) {
            // Batch 717 — antrean BARU (pertama kali atau menggantikan yang lama): acak, BUKAN identitas.
            runCatching { onQueueReplaced() }
            return derive(IntArray(insertionCount) { it }.also { it.shuffle(random) })
        }
        val shifted = IntArray(shuffled.size) { i ->
            val value = shuffled[i]
            if (value >= insertionIndex) value + insertionCount else value
        }
        val appended = IntArray(insertionCount) { insertionIndex + it }
        val after = positionForPlayNext(insertionIndex)
        if (after < 0) return derive(shifted + appended)
        // Batch 717 — "Putar Berikutnya": sisip tepat setelah posisi lagu aktif di urutan Shuffle.
        val result = IntArray(shifted.size + insertionCount)
        shifted.copyInto(result, 0, 0, after + 1)
        appended.copyInto(result, after + 1)
        shifted.copyInto(result, after + 1 + insertionCount, after + 1, shifted.size)
        return derive(result)
    }

    /** Posisi (di [shuffled]) lagu aktif bila sisipan di [insertionIndex] adalah "Putar Berikutnya", selain itu -1. */
    private fun positionForPlayNext(insertionIndex: Int): Int {
        if (insertionIndex >= shuffled.size) return -1 // penambahan di ujung antrean = ekor
        val current = runCatching { currentIndexProvider() }.getOrDefault(C.INDEX_UNSET)
        if (current < 0 || current >= shuffled.size || insertionIndex != current + 1) return -1
        return positionOf[current]
    }

    override fun cloneAndRemove(indexFrom: Int, indexToExclusive: Int): ShuffleOrder {
        val removedCount = indexToExclusive - indexFrom
        val result = ArrayList<Int>(shuffled.size)
        for (value in shuffled) {
            when {
                value >= indexFrom && value < indexToExclusive -> Unit // dibuang, ikut terhapus
                value >= indexToExclusive -> result.add(value - removedCount)
                else -> result.add(value)
            }
        }
        return derive(result.toIntArray())
    }

    override fun cloneAndClear(): ShuffleOrder = derive(IntArray(0))

    companion object {
        /** Ukuran window "baru-baru diputar" — lihat poin 2 di doc kelas ini. Skala mengikuti
         * ukuran antrean saat ini, bukan angka tetap: antrean pendek (<=8) dapat 0 (fitur ini
         * otomatis no-op murni utk antrean sekecil itu, BUKAN bug), selebihnya seperempat
         * ukuran antrean dibatasi 5..25 supaya tidak pernah menelan mayoritas antrean kecil
         * maupun jadi tidak berarti apa-apa di antrean raksasa. */
        fun windowSizeFor(queueSize: Int): Int =
            if (queueSize <= 8) 0 else (queueSize / 4).coerceIn(5, 25)
    }
}

/**
 * Menyusun permutasi dari `songIds.indices`: lagu yang BUKAN bagian dari [recentlyPlayedIds]
 * diprioritaskan di awal urutan, lagu yang baru saja diputar ditaruh belakangan — plus jaminan
 * 0 dua lagu SAMA PERSIS bersebelahan langsung kalau `songIds` kebetulan punya duplikat (mis.
 * antrean yang sama dimasukkan 2x lewat "Tambah ke Antrean"). Degradasi aman: kalau seluruh
 * antrean jatuh ke satu kelompok saja (window relatif kelewat besar/kecil dari ukuran antrean
 * saat itu), fallback ke shuffle penuh biasa alih-alih memaksa split yang tidak berguna.
 *
 * Batch 717 — [anchorIndex] (0 until size): indeks lagu yang sedang diputar, dipaku di POSISI PERTAMA urutan; sisanya
 * diacak seperti biasa (segar dulu, baru-diputar belakangan). Di luar rentang (default) = perilaku lama tanpa jangkar.
 */
fun buildAntiRepeatNearbyOrder(
    songIds: List<Long>,
    recentlyPlayedIds: Set<Long>,
    anchorIndex: Int = C.INDEX_UNSET,
    random: Random = Random.Default
): IntArray {
    val n = songIds.size
    if (n <= 1) return IntArray(n) { it }

    val anchored = anchorIndex in 0 until n
    val freshIdx = ArrayList<Int>(n)
    val recentIdx = ArrayList<Int>(n)
    for (i in 0 until n) {
        if (anchored && i == anchorIndex) continue
        if (songIds[i] in recentlyPlayedIds) recentIdx.add(i) else freshIdx.add(i)
    }

    val order: MutableList<Int> = ArrayList<Int>(n)
    if (anchored) order.add(anchorIndex)
    if (freshIdx.isEmpty() || recentIdx.isEmpty()) {
        order.addAll((freshIdx + recentIdx).shuffled(random))
    } else {
        order.addAll(freshIdx.shuffled(random))
        order.addAll(recentIdx.shuffled(random))
    }

    // Guard adjacency lagu SAMA PERSIS — best-effort: cari posisi j lain yang aman utk
    // ditukar (song beda DAN 0 menciptakan adjacency baru di tetangga j). Kalau tidak ada
    // (mis. seluruh antrean 1 lagu diulang terus), dibiarkan apa adanya, 0 crash.
    for (i in 1 until order.size) {
        if (songIds[order[i]] != songIds[order[i - 1]]) continue
        val target = songIds[order[i]]
        var swapWith = -1
        for (j in order.indices) {
            if (j == i || j == i - 1) continue
            if (anchored && j == 0) continue // jangkar tak boleh bergeser
            if (songIds[order[j]] == target) continue
            val leftOk = j == 0 || songIds[order[j - 1]] != target
            val rightOk = j == order.size - 1 || songIds[order[j + 1]] != target
            if (leftOk && rightOk) {
                swapWith = j
                break
            }
        }
        if (swapWith != -1) {
            val tmp = order[i]
            order[i] = order[swapWith]
            order[swapWith] = tmp
        }
    }

    return order.toIntArray()
}
