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
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
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
 * `4000` (angka bersih, deviasi <4% dari nilai eksak, diabaikan). Kalau masih perlu tuning:
 * NAIKKAN dari 4000 kalau masih kerasa ngambang (breakeven baru: [4000, 10000]), TURUNKAN kalau
 * masih kerasa kaku/satset (breakeven baru: [1500, 4000]) — cukup ulangi biseksi geometris di
 * rentang yang menyempit, TIDAK perlu balik ke preset resmi.
 */
private const val OVERSCROLL_SETTLE_STIFFNESS = 4000f

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
        object : Modifier.Node(), LayoutModifierNode {
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
        // Fling selesai sementara konten masih tertarik ke luar batas (mis. fling ke arah luar) —
        // pegas balik ke 0. `DampingRatioMediumBouncy` dipilih sengaja: ada sedikit "pantulan"
        // sekilas (khas iOS) tapi tidak berosilasi berkali-kali seperti bouncy ball.
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
        overscrollOffset.animateTo(
            targetValue = Offset.Zero,
            initialVelocity = Offset(remaining.x, remaining.y),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
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
