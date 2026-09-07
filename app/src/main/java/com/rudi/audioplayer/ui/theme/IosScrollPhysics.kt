package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

// Batch 364 — User (device asli) laporan: "efek scrolling masih sat set, zero transisi mulus
// like iOS!!" setelah ROADMAP_LIQUID_GLASS_REDESIGN.md ditutup 100% Batch 363 (yang soal
// GPU-lag, BUKAN soal ini — laporan ini item baru, di luar roadmap itu). Root cause dari sisi
// Compose: (1) overscroll bawaan Android di batas atas/bawah list cuma "glow" lalu berhenti
// mendadak — tidak ada rubber-band/pantulan seperti UIScrollView, dan (2) kurva fling default
// (`rememberSplineBasedDecay`) memang di-tune buat feel Android native, jauh lebih pendek/cepat
// berhenti dibanding glide UIScrollView yang lebih panjang & mulus. File ini menyediakan
// perbaikan utk keduanya:
//   (a) `IosOverscrollFactory` — dipasang SEKALI di root (`Theme.kt`, via
//       `LocalOverscrollFactory`), otomatis berlaku ke SEMUA `LazyColumn`/`LazyRow`/
//       `verticalScroll`/`horizontalScroll` di seluruh app tanpa perlu disentuh 1-per-1 (docs
//       resmi: komponen level-tinggi seperti LazyColumn otomatis konsumsi
//       `LocalOverscrollFactory` lewat `rememberOverscrollEffect()`).
//   (b) `rememberIosFlingBehavior()` — TIDAK bisa dipasang app-wide (Compose belum expose
//       CompositionLocal utk flingBehavior default seperti overscroll), jadi opt-in per
//       scrollable. Diterapkan bertahap per micro-batch (MICRO_BATCH: maks 3 file kode/task) —
//       screen yg belum kebagian ada di `PENDING_IosFlingBehavior.md`.

// Batch 372 — User laporan: "kaku"+"regresi" MASIH bareng persis kayak sebelumnya walau
// `OVERSCROLL_SETTLE_STIFFNESS` sudah diganti 3x (1500→10000→4000, Batch 369-371) — 3x ganti
// ANGKA, gejala IDENTIK, adalah sinyal klasik salah PARAMETER yang ditune, bukan salah nilainya.
// Ditanya "kaku ini paling kerasa pas ngapain": jawab user "tarik ujung daftar sampai mentok,
// pakai banget [tenaga]" — ini men-describe FASE TARIKAN (jari masih nempel), BUKAN fase pegas-
// balik-setelah-lepas yang jadi target `OVERSCROLL_SETTLE_STIFFNESS` di [applyToFling] (itu HANYA
// jalan setelah jari dilepas). Fase tarikan diatur SEPENUHNYA oleh [rubberBandResistance] di bawah
// — kode yang TIDAK PERNAH disentuh sejak Batch 364, jadi wajar 3x tuning stiffness settle tidak
// mengubah gejala ini: dua parameter itu independen, mengatur dua fase yang berbeda.
//
// Ditanya juga "testing di layar mana" (dugaan: mungkin cuma sebagian dari 11 layar sisa
// `PENDING_IosFlingBehavior.md` yang bermasalah) — user TIDAK YAKIN layar mana persisnya. Ini
// KONSISTEN dgn diagnosis di atas, bukan kontradiksi: [IosRubberBandOverscrollEffect] dipasang
// SEKALI app-wide lewat `LocalOverscrollFactory` di `Theme.kt` ([IosOverscrollFactory] di bawah),
// jadi bug ini UNIVERSAL ke semua scrollable — beda dgn `rememberIosFlingBehavior()` (kurva fling,
// opt-in per-layar, itu yg di-track di `PENDING_IosFlingBehavior.md`, TIDAK terkait bug ini).
//
// Root cause: range lama (220px, FIXED, tidak bergantung ukuran device/viewport) terlalu kecil
// di device modern manapun — resistance sudah turun ke ~separuh hanya di 220px tarikan, mendekati
// nol jauh sebelum jari sempat ditarik "pakai banget" — makanya konten terasa "mentok"/rigid,
// TIDAK PEDULI sekeras/sejauh apa jari menarik. UIScrollView/WebKit asli mengikat parameter jarak
// ini ke DIMENSI VIEWPORT (`d` di formula `x*d*c/(d+c*x)`), bukan angka px tetap. Fix: jarak
// "separuh resistance" diturunkan dari ukuran viewport hasil `measure()` (sudah tersedia di
// [overscrollNode], 0 context/composable tambahan), PER SUMBU (lebar utk drag horizontal, tinggi
// utk vertikal) — bukan lagi 1 angka tetap yg sama utk semua ukuran layar & kedua arah.

// Batch 374 — User laporan singkat: "sekarang perbaiki karakter pantulan yang kerasa tidak
// natural sama sekali woy!!" — beda AKSIS dari Batch 368-373 (yang semuanya soal KECEPATAN
// settle, `stiffness`). "Karakter pantulan" secara eksplisit menunjuk ke bentuk osilasi pegas
// itu sendiri — parameter `dampingRatio`, satu-satunya yang TIDAK PERNAH disentuh sejak Batch
// 364 (tercatat "disetujui user Batch 366", tapi user sekarang eksplisit bilang "sama sekali"
// tidak natural — feedback baru menimpa asumsi lama itu, bukan kontradiksi berbahaya). Lihat
// [applyToFling] utk detail diagnosis & fix (`DampingRatioMediumBouncy` -> `DampingRatioLowBouncy`).

// Batch 373 — User laporan singkat: "revert effect bounce dari yang kaku -> hampir mengambang!!"
// — instruksi arah, bukan pertanyaan diagnostik baru. "Bounce" di sini merujuk fase PEGAS BALIK
// setelah jari dilepas ([applyToFling] di bawah), BUKAN fase tarikan ([rubberBandResistance] +
// [RUBBER_BAND_VIEWPORT_FRACTION] — itu target fix Batch 372 yang terpisah & tidak disentuh batch
// ini). Ini persis skenario "TURUNKAN kalau masih kerasa kaku/satset" yang sudah diantisipasi
// eksplisit di komentar [OVERSCROLL_SETTLE_STIFFNESS] sejak Batch 371 — breakeven baru [1500,
// 4000]. Tapi kata "revert" + "hampir mengambang" (bukan cuma "sedikit kekakuan") menunjuk ke
// tindakan yang lebih tegas dari sekadar 1 langkah biseksi geometris (~2450) — user minta balik
// ke ujung BAWAH bracket, bukan titik tengahnya. `1500` (`Spring.StiffnessMedium`) dipilih persis
// krn nilai ITU SENDIRI yang sudah didokumentasikan Batch 369 sebagai titik yang masih terasa
// "ngambang" (floating) — preset resmi Compose yang sudah pernah diuji nyata, bukan angka baru
// hasil tebakan. `dampingRatio` (`DampingRatioMediumBouncy`) TETAP TIDAK disentuh — user cuma
// bicara soal KECEPATAN settle ("kaku" -> "mengambang"), bukan KARAKTER pantulannya, konsisten
// pola Batch 369-372.

// Batch 375 — User kasih root cause spesifik (bukan laporan "masih kerasa X" seperti 368-374):
// "ketika user tarik sampai mentok terus layar kehilangan kontak sentuhan (misalnya layar ->
// case hp), itu akan memicu semacam delay sepersekian detik sebelum balik ke kondisi semula".
// Ini SUMBU KETIGA yang beda lagi dari 368-374 (yang selalu soal parameter spring —
// `stiffness`/`dampingRatio`, DUA-DUANYA TIDAK disentuh batch ini): skenario ini bukan soal
// "pegas-nya kurang pas", tapi soal pegas baliknya TIDAK PERNAH DIPICU sama sekali sampai
// sesuatu yang lain (event tak terkait) kebetulan menyentuhnya belakangan.
//
// Diagnosis (verified via dokumentasi resmi `PointerInputModifierNode.onCancelPointerInput`,
// developer.android.com — dipicu spesifik saat "Android dispatches ACTION_CANCEL to Compose"):
// jari yang "kehilangan kontak" dengan cara meluncur ke bezel/case (BUKAN diangkat bersih dalam
// batas layar) adalah kandidat kuat `ACTION_CANCEL`, bukan `ACTION_UP` normal — salah satu
// pemicu paling umum di Android adalah zona disambiguasi gesture-navigasi (edge back-gesture/
// predictive back) yang MENAHAN touch stream sejenak sebelum akhirnya membatalkannya ke app;
// bagian delay INI sendiri (di level OS, sebelum event sampai ke Compose sama sekali) di luar
// kendali kode app mana pun, TIDAK bisa "diperbaiki" dari sisi ini.
//
// TAPI: kontrak resmi `OverscrollEffect` cuma punya 2 pintu masuk event, [applyToScroll] &
// [applyToFling] — keduanya bagian dari alur "drag berakhir NORMAL" yang dikelola `scrollable()`
// sendiri (dokumentasi resmi `FlingBehavior.performFling`: "When drag has ended WITH VELOCITY",
// tidak menyebut skenario dibatalkan/`ACTION_CANCEL`). Sinyal analog dari kodebase resmi androidx
// sendiri (fork `CupertinoOverscrollEffect.kt`, PR resmi "Fix freeze when scrolling is cancelled
// during overscroll"): overscroll effect BISA macet/frozen kalau drag-nya dibatalkan di tengah,
// karena efeknya cuma direset lewat jalur fling normal — persis kelas bug yang sama. [overscrollNode]
// milik file ini punya NOL penanganan untuk skenario cancel — begitu offset ter-`snapTo` ke
// posisi mentok (Batch 372) lalu `ACTION_CANCEL` turun, TIDAK ADA kode yang memicu [settleToZero]
// sampai kebetulan ada scroll delta baru lain yang menyentuhnya — persis kenapa kerasa "delay",
// bukan macet permanen, cuma menunggu trigger yang salah.
//
// Fix: [overscrollNode] (di bawah) sekarang JUGA implement `PointerInputModifierNode` (API resmi,
// contoh persis dari dokumentasi `DelegatingNode` — 1 node boleh gabung beberapa interface
// `Modifier.Node`, `LayoutModifierNode` yang sudah ada TIDAK disentuh/dihapus) — hitung
// `pointersDown` mentah dari `onPointerEvent` (pass `Initial`, sebelum ada consumer lain yang bisa
// mempengaruhi), dan `onCancelPointerInput()` (dipanggil PERSIS saat `ACTION_CANCEL` turun ke
// Compose, per dokumentasi resmi) sebagai jaring pengaman KEDUA yang independen dari alur fling
// normal — begitu pointer terakhir lepas/batal, LANGSUNG panggil [settleToZero] kalau overscroll
// masih punya offset & belum ada animasi jalan (`!isRunning`, guard biar tidak duplikat/berebut
// sama alur fling normal kalau itu ternyata tetap terpanggil). Ini SEPENUHNYA jaring pengaman
// tambahan — jalur [applyToFling] normal (termasuk semua tuning stiffness/dampingRatio Batch
// 368-374) TIDAK diubah sama sekali, cuma dipanggil dari 2 tempat sekarang (lihat [settleToZero]).
//
// **Belum ditest di device asli** (tidak ada env Android nyata di sesi ini) — skenario "tarik
// sampai mentok lalu geser jari ke bezel/case" perlu direplikasi manual. Kalau MASIH kerasa ada
// jeda setelah ini: kemungkinan besar sisa delay itu murni dari OS (window disambiguasi gesture-
// navigasi sebelum `ACTION_CANCEL` sampai ke Compose sama sekali) — di luar apa yang bisa
// diperbaiki lewat [IosRubberBandOverscrollEffect], bukan berarti fix ini belum lengkap.

/**
 * Porsi dimensi viewport (lebar utk sumbu x, tinggi utk sumbu y) yang jadi jarak "separuh
 * resistance" rubber-band — analog konstanta tension `c` WebKit, dipakai di peran "range" formula
 * sederhana file ini (lihat [rubberBandResistance]), bukan port formula WebKit persis.
 * Kalau abis testing masih kerasa kaku: NAIKKAN. Kalau kerasa terlalu lentur/susah "mentok":
 * TURUNKAN. Cukup ubah pecahan ini — JANGAN balik ke konstanta px tetap (itu yg jadi bug 372).
 */
private const val RUBBER_BAND_VIEWPORT_FRACTION = 0.55f

/**
 * Berapa persen delta tarikan baru yang masih diteruskan ke offset overscroll, pada jarak
 * [magnitudePx], dgn jarak "separuh resistance" [rangePx] (dinamis per viewport & per sumbu —
 * lihat [RUBBER_BAND_VIEWPORT_FRACTION] dan `rubberBandRangeXPx`/`rubberBandRangeYPx`).
 */
private fun rubberBandResistance(magnitudePx: Float, rangePx: Float): Float =
    1f / (1f + abs(magnitudePx) / rangePx.coerceAtLeast(1f))

/**
 * Stiffness pegas balik overscroll di [IosRubberBandOverscrollEffect.applyToFling] — CUSTOM
 * (bukan preset `Spring.Stiffness*` resmi). Batch 368→370 mencoba preset resmi berurutan (200 →
 * 400 → 1500 → 10000) dan berhasil MEMBRACKET titik ideal dari 2 sisi: `StiffnessMedium` (1500,
 * Batch 369) user masih lapor "ngambang"; `StiffnessHigh` (10000, Batch 370) user lapor kebalikan
 * persis — "kaku/satset", kehilangan rasa pantulan iOS. Artinya sweet spot ada DI ANTARA keduanya,
 * tapi lompatan antar preset resmi (kelipatan ~3-6x tiap naik) terlalu kasar buat presisi di sini
 * — preset berikutnya di atas/bawah rentang [1500, 10000] tidak ada. Batch 371 pindah ke nilai
 * custom lewat biseksi yang benar secara fisika: karena kecepatan settle berbanding ke frekuensi
 * natural ωₙ = √(stiffness/mass) — BUKAN linear ke `stiffness` mentah (pelajaran pahit dari Batch
 * 368) — titik tengah yang benar secara PERSEPSI adalah rata-rata GEOMETRIS di `stiffness`
 * (setara rata-rata aritmetis di ωₙ), bukan rata-rata aritmetis di `stiffness` mentah yang akan
 * bias jauh ke salah satu ujung dari sisi kecepatan rasa. √(1500 × 10000) ≈ 3873, dibulatkan ke
 * `4000` (angka bersih, deviasi <4% dari nilai eksak, diabaikan).
 *
 * Batch 373 — User: "revert ... dari yang kaku -> hampir mengambang!!" — 4000 masih kerasa
 * kaku/satset, minta balik ke arah floating secara tegas (bukan cuma 1 langkah biseksi halus).
 * DITURUNKAN ke `1500f` (= `Spring.StiffnessMedium`, preset resmi Compose) — ujung BAWAH bracket
 * [1500, 4000] yang sudah didokumentasikan Batch 369 sendiri sebagai titik yang masih terasa
 * "ngambang" (floating), bukan angka custom baru hasil tebakan. Kalau masih perlu tuning:
 * TURUNKAN lebih jauh (mis. `Spring.StiffnessMediumLow` 400, atau `StiffnessLow` 200 — breakeven
 * baru [200, 1500]) kalau MASIH kerasa kurang mengambang; NAIKKAN sedikit (biseksi geometris ke
 * arah [1500, 4000] lagi) kalau sekarang JUSTRU kelewat floating/lambat balik. `dampingRatio`
 * (`DampingRatioMediumBouncy`) tetap tidak disentuh.
 */
private const val OVERSCROLL_SETTLE_STIFFNESS = 1500f

/**
 * Overscroll ala iOS: menggeser KONTEN (bukan menggambar glow di atasnya) saat ditarik lewat
 * batas list, dengan tahanan yang makin kuat seiring jarak tarikan (rubber-band), lalu pegas
 * balik ke posisi normal saat jari dilepas. Satu instance = satu scrollable (dibuat baru tiap
 * `rememberOverscrollEffect()`), jadi aman menangani sumbu x & y sekaligus lewat `Offset` — utk
 * `LazyColumn` (vertikal) sumbu x instance ini tidak akan pernah bergerak dari 0, dan sebaliknya
 * utk `LazyRow`.
 */
private class IosRubberBandOverscrollEffect : OverscrollEffect {

    private val overscrollOffset = Animatable(Offset.Zero, Offset.VectorConverter)

    // Batch 372 — diisi tiap `measure()`, dipakai `rubberBandRangeXPx`/`rubberBandRangeYPx` di
    // bawah supaya range resistance rubber-band ikut skala viewport asli (lihat KDoc
    // [RUBBER_BAND_VIEWPORT_FRACTION]), bukan lagi angka px tetap yang sama di semua ukuran layar.
    private var viewportWidthPx = 0f
    private var viewportHeightPx = 0f

    // `Modifier.Node` sendiri sudah punya `coroutineScope` (tersedia setelah node ini attach ke
    // hierarchy) — dipakai langsung di `applyToScroll` di bawah supaya effect ini tidak perlu
    // parameter `CoroutineScope` eksternal dari `rememberCoroutineScope()` (yang tidak bisa
    // dipanggil di `OverscrollFactory.createOverscrollEffect()`, fungsi itu bukan @Composable).
    private val overscrollNode =
        object : Modifier.Node(), LayoutModifierNode, PointerInputModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                viewportWidthPx = placeable.width.toFloat()
                viewportHeightPx = placeable.height.toFloat()
                return layout(placeable.width, placeable.height) {
                    val offset = overscrollOffset.value
                    placeable.placeRelativeWithLayer(offset.x.roundToInt(), offset.y.roundToInt())
                }
            }

            // Batch 375 — hitung pointer mentah, independen dari `scrollable()`/`applyToFling`
            // (lihat catatan Batch 375 di atas file ini utk diagnosis lengkap kenapa jalur fling
            // normal saja tidak cukup). Pass `Initial` dipakai supaya hitungan ini tidak
            // terpengaruh consumer lain di pohon modifier (selalu lihat event mentah).
            private var pointersDown = 0

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize,
            ) {
                if (pass != PointerEventPass.Initial) return
                when (pointerEvent.type) {
                    PointerEventType.Press -> pointersDown++
                    PointerEventType.Release -> {
                        pointersDown = (pointersDown - 1).coerceAtLeast(0)
                        if (pointersDown == 0) settleIfAbandoned()
                    }
                    else -> Unit
                }
            }

            // Dipanggil PERSIS saat Android dispatch `ACTION_CANCEL` ke Compose (dokumentasi
            // resmi `PointerInputModifierNode.onCancelPointerInput`) — skenario "jari kehilangan
            // kontak" (mis. meluncur ke bezel/case) yang jadi laporan Batch 375, bukan cuma
            // pelengkap teoretis.
            override fun onCancelPointerInput() {
                pointersDown = 0
                settleIfAbandoned()
            }

            // Jaring pengaman: kalau overscroll masih py offset tapi tidak ada animasi settle yg
            // sedang jalan (berarti jalur [applyToFling] normal TIDAK terpanggil utk gesture ini
            // — persis kelas bug Batch 375), paksa panggil [settleToZero] langsung dari sini.
            // Guard `!isRunning` sengaja dicek supaya tidak duplikat/berebut kalau jalur fling
            // normal ternyata TETAP terpanggil (harmless, Animatable aman dipanggil `animateTo`
            // 2x — panggilan baru otomatis membatalkan yg lama — tapi guard ini menghindari
            // launch coroutine yg tidak perlu di kasus umum/normal).
            private fun settleIfAbandoned() {
                if (overscrollOffset.value == Offset.Zero || overscrollOffset.isRunning) return
                coroutineScope.launch { settleToZero() }
            }
        }

    /** Jarak (px) "separuh resistance" sumbu x (drag horizontal) — lihat [RUBBER_BAND_VIEWPORT_FRACTION]. */
    private val rubberBandRangeXPx: Float get() = viewportWidthPx * RUBBER_BAND_VIEWPORT_FRACTION

    /** Jarak (px) "separuh resistance" sumbu y (drag vertikal) — lihat [RUBBER_BAND_VIEWPORT_FRACTION]. */
    private val rubberBandRangeYPx: Float get() = viewportHeightPx * RUBBER_BAND_VIEWPORT_FRACTION

    override val node: DelegatableNode get() = overscrollNode

    override val isInProgress: Boolean
        get() = overscrollOffset.isRunning || overscrollOffset.value != Offset.Zero

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        val current = overscrollOffset.value
        // Lepas tegangan overscroll dulu kalau user mulai scroll balik arah, per sumbu — sama
        // pola dengan contoh resmi Google utk OverscrollEffect (1 sumbu), digeneralisasi ke 2.
        val consumedByRelease = Offset(
            x = relaxAxis(delta.x, current.x),
            y = relaxAxis(delta.y, current.y),
        )
        if (consumedByRelease != Offset.Zero) {
            overscrollNode.coroutineScope.launch { overscrollOffset.snapTo(current + consumedByRelease) }
        }
        val leftForScroll = delta - consumedByRelease
        val consumedByScroll = performScroll(leftForScroll)
        val overscrollDelta = leftForScroll - consumedByScroll
        if (overscrollDelta != Offset.Zero && source == NestedScrollSource.UserInput) {
            val projected = current + consumedByRelease
            val resisted = Offset(
                x = overscrollDelta.x * rubberBandResistance(projected.x, rubberBandRangeXPx),
                y = overscrollDelta.y * rubberBandResistance(projected.y, rubberBandRangeYPx),
            )
            overscrollNode.coroutineScope.launch { overscrollOffset.snapTo(projected + resisted) }
        }
        return consumedByRelease + consumedByScroll
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        val consumed = performFling(velocity)
        val remaining = velocity - consumed
        settleToZero(Offset(remaining.x, remaining.y))
    }

    // Batch 375 — diekstrak dari isi `applyToFling` (dulu inline langsung di situ) SUPAYA bisa
    // dipanggil ulang dari [overscrollNode.settleIfAbandoned] (jaring pengaman `onCancelPointerInput`/
    // pointer-release, lihat catatan Batch 375 di atas file ini) — 0 perubahan LOGIKA/PARAMETER,
    // murni pemindahan biar tidak duplikasi kode antara 2 titik panggil. `initialVelocity` default
    // `Offset.Zero` dipakai spesifik dari jalur jaring pengaman (kasus cancel mentah tidak punya
    // info velocity fling sungguhan); jalur [applyToFling] normal tetap kirim `remaining` asli.
    private suspend fun settleToZero(initialVelocity: Offset = Offset.Zero) {
        // Fling selesai sementara konten masih tertarik ke luar batas (mis. fling ke arah luar) —
        // pegas balik ke 0. `DampingRatioLowBouncy` dipilih (Batch 374, gantikan
        // `DampingRatioMediumBouncy` Batch 364-373) — lihat catatan Batch 374 di bawah utk alasan.
        //
        // Batch 368 — User laporan regresi: "ditarik maksimal tapi tidak langsung reset ketempat
        // semula". Root cause (verified via kontrak resmi `OverscrollEffect.applyToFling`,
        // developer.android.com: performFling mengembalikan velocity yg SUDAH dikonsumsi, jadi
        // `remaining` di atas = sisa velocity yg BELUM terkonsumsi): kalau rilis jari terjadi
        // dengan velocity rendah/nyaris nol (tarik pelan sampai mentok lalu lepas jari begitu
        // saja, BEDA dgn fling/sentakan cepat ke arah batas), maka `remaining` ikut ~0 — pegas
        // balik cuma mengandalkan gaya pemulihannya sendiri, dan `StiffnessLow` (200) sengaja
        // "lembut" jadi kerasa lambat/ngambang persis di kasus itu. Batch 368 menaikkan ke
        // `StiffnessMediumLow` (400) dgn asumsi "2x nilai = 2x cepat" — TERBUKTI KELIRU (user
        // masih lapor regresi kerasa persis skenario yang sama, "ditarik sampai mentok ujung
        // layar").
        //
        // Batch 369 — Root cause KENAPA Batch 368 belum cukup: pegas massa-tunggal punya
        // frekuensi natural ωₙ = sqrt(stiffness/mass), dan waktu settling berbanding TERBALIK
        // dgn ωₙ — bukan linear ke `stiffness` mentah. Menaikkan `stiffness` 200→400 (2x nilai)
        // cuma menaikkan ωₙ sebesar sqrt(2) ≈ 1.41x, jadi waktu kembali ke 0 cuma turun ~29%
        // (bukan 50% seperti diasumsikan Batch 368) — persis kenapa masih "kerasa ngambang" tepat
        // di kasus tarik-jauh-lepas-pelan yang sama. Fix: naikkan ke `Spring.StiffnessMedium`
        // (1500, konstanta resmi Compose — bukan angka custom) — sqrt(1500/400) ≈ 1.94x lebih
        // cepat dari Batch 368, sqrt(1500/200) ≈ 2.74x lebih cepat dari baseline Batch 364,
        // lompatan yang genuinely terasa alih-alih di bawah ambang persepsi.
        // `dampingRatio` (`DampingRatioMediumBouncy`) TETAP TIDAK diubah — pantulan khas iOS yang
        // sudah disetujui user (Batch 366) tidak disentuh, murni kecepatan "kembali ke 0" yang
        // dipercepat lagi.
        //
        // Batch 370 — User laporan lagi: "regresi nya masih kerasa!!" — dinaikkan ke
        // `Spring.StiffnessHigh` (10000, preset resmi tertinggi Compose).
        //
        // Batch 371 — User laporan HASIL Batch 370, dan ini SINYAL PENTING: "regresi nya sendiri
        // gak hilang, yang ada malah jadi kaku/Satset!!" — dua gejala SEKALIGUS, bukan cuma
        // "masih kurang", dan dua-duanya di ARAH BERLAWANAN (masih ngambang DAN sekarang kaku).
        // Dibaca sebagai: preset 1500 (Batch 369) undershoot, preset 10000 (Batch 370) overshoot
        // ke arah berlawanan — rasa "smooth like iOS" yang jadi tujuan asli (Batch 364) ada DI
        // ANTARA dua titik itu, bukan di salah satu ujung preset resmi. Lompatan antar preset
        // resmi Compose (200/400/1500/10000) terlalu kasar untuk presisi di titik ini. Fix:
        // pindah ke [OVERSCROLL_SETTLE_STIFFNESS] (custom, 4000) — rata-rata GEOMETRIS dari 1500
        // & 10000 (bukan aritmetis; benar secara fisika karena kecepatan settle ~ ωₙ = √(stiffness/
        // mass), lihat dokumentasi lengkap di deklarasi konstantanya). `dampingRatio`
        // (`DampingRatioMediumBouncy`) TETAP TIDAK diubah — user cuma keberatan soal KECEPATAN
        // settle ("kaku/satset"), bukan soal KARAKTER pantulannya.
        //
        // Batch 373 — User: "revert effect bounce dari yang kaku -> hampir mengambang!!". 4000
        // (Batch 371) masih dirasakan kaku; diturunkan ke `1500` (`Spring.StiffnessMedium`) —
        // ujung bawah bracket [1500, 4000], nilai yang sudah pernah diuji & didokumentasikan
        // Batch 369 sebagai terasa "ngambang". Lihat dokumentasi lengkap di deklarasi
        // [OVERSCROLL_SETTLE_STIFFNESS] utk arah tuning berikutnya kalau masih perlu.
        //
        // Batch 374 — User: "perbaiki karakter pantulan yang kerasa tidak natural sama sekali
        // woy!!" — SUMBU BEDA dari Batch 368-373 (semuanya soal `stiffness`, KECEPATAN pegas
        // kembali ke 0). "Karakter pantulan" menunjuk ke BENTUK osilasinya sendiri —
        // `dampingRatio`, satu-satunya parameter spring ini yang belum pernah diubah sejak Batch
        // 364. `DampingRatioMediumBouncy` (0.5) punya rasio redaman relatif rendah — pegas
        // overshoot lalu berosilasi 2-3+ kali sebelum benar-benar diam, karakter yang lebih dekat
        // ke "bola pantul"/spring-toy ketimbang rubber-band UIScrollView asli (yang cuma overshoot
        // SATU KALI tipis lalu langsung tenang, nyaris tanpa osilasi ulang). Root cause dari
        // "tidak natural" kemungkinan besar persis osilasi berulang itu — bukan soal kecepatan.
        // Fix: `DampingRatioMediumBouncy` (0.5) -> `DampingRatioLowBouncy` (0.75, preset resmi
        // Compose berikutnya menuju redaman kritis) — overshoot tunggal yang jauh lebih halus,
        // 1 ayunan balik lalu settle, tanpa lompat ke `DampingRatioNoBouncy` (1.0, redaman kritis
        // penuh) yang akan menghapus pantulan sama sekali (bukan yang diminta — user cuma bilang
        // "tidak natural", bukan "hapus pantulan"). `stiffness` ([OVERSCROLL_SETTLE_STIFFNESS],
        // 1500 sejak Batch 373) TIDAK disentuh — sumbu yang berbeda, di luar laporan ini.
        overscrollOffset.animateTo(
            targetValue = Offset.Zero,
            initialVelocity = initialVelocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = OVERSCROLL_SETTLE_STIFFNESS,
            ),
        )
    }

    /** Berapa banyak [deltaAxis] yang diserap untuk melepas tegangan overscroll [currentAxis] yang sudah ada. */
    private fun relaxAxis(deltaAxis: Float, currentAxis: Float): Float {
        if (abs(currentAxis) < 0.5f || deltaAxis == 0f) return 0f
        if (sign(deltaAxis) == sign(currentAxis)) return 0f // arah sama = menambah tarikan, bukan melepas
        val newValue = currentAxis + deltaAxis
        // Kalau delta ini sampai membalik tanda, berarti sudah lebih dari cukup utk melepas
        // tegangan — serap hanya sebesar yg dibutuhkan buat pas mendarat di 0, sisanya diteruskan
        // ke scrollable asli sebagai scroll normal.
        return if (sign(currentAxis) != sign(newValue)) -currentAxis else deltaAxis
    }
}

/** Dipasang di root (`AudioPlayerTheme`, lihat `Theme.kt`) lewat `LocalOverscrollFactory`. */
object IosOverscrollFactory : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect = IosRubberBandOverscrollEffect()

    // OverscrollFactory declares equals/hashCode as abstract (dipakai buat CompositionLocal
    // equality check) -- singleton jadi cukup referential equality, per rekomendasi resmi API.
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Friction multiplier utk [exponentialDecay] — default Compose adalah `1f` (tuned utk fling
 * Android native, berhenti relatif cepat). UIScrollView normal punya `decelerationRate` yang
 * secara matematis juga exponential decay, tapi glide-nya jauh lebih panjang & mulus; `0.75f` di
 * sini dipilih supaya list meluncur lebih jauh & halus tanpa jadi terasa "licin"/tidak
 * terkendali. Boleh di-tuning lebih lanjut kalau user masih merasa kurang/lebih dari ini.
 */
private const val IOS_FLING_FRICTION_MULTIPLIER = 0.75f

/**
 * Kurva fling ala iOS utk dipasang manual per scrollable (`LazyColumn(flingBehavior = ...)`,
 * dst.) — lihat catatan di atas kenapa ini tidak bisa app-wide seperti [IosOverscrollFactory].
 */
@Composable
fun rememberIosFlingBehavior(frictionMultiplier: Float = IOS_FLING_FRICTION_MULTIPLIER): FlingBehavior {
    val flingSpec = remember(frictionMultiplier) {
        exponentialDecay<Float>(frictionMultiplier = frictionMultiplier)
    }
    return remember(flingSpec) { IosFlingBehavior(flingSpec) }
}

private class IosFlingBehavior(private val flingDecay: DecayAnimationSpec<Float>) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        var velocityLeft = initialVelocity
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(flingDecay) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            velocityLeft = velocity
            // Berhenti kalau scrollable tidak lagi bisa mengonsumsi (mentok batas) — biar tidak
            // terus "mendorong" ke arah yang sudah penuh.
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
        return velocityLeft
    }
}
