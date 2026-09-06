package com.rudi.audioplayer.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LayoutModifierNode
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
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

/**
 * Jarak (px) di mana ketahanan tarikan rubber-band sudah turun ke ~separuh. Makin besar nilainya,
 * makin "kaku"/berat konten terasa saat ditarik lewat batas — 220px dipilih supaya terasa ada
 * tahanan sejak awal (bukan translasi 1:1 seperti Android biasa) tapi masih responsif ke jari,
 * mendekati rasa UIScrollView tanpa perlu port formula persis (`x*d*c/(d+c*x)`) milik WebKit.
 */
private const val RUBBER_BAND_RANGE_PX = 220f

/** Berapa persen delta tarikan baru yang masih diteruskan ke offset overscroll, pada jarak [magnitudePx]. */
private fun rubberBandResistance(magnitudePx: Float): Float = 1f / (1f + abs(magnitudePx) / RUBBER_BAND_RANGE_PX)

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
                return layout(placeable.width, placeable.height) {
                    val offset = overscrollOffset.value
                    placeable.placeRelativeWithLayer(offset.x.roundToInt(), offset.y.roundToInt())
                }
            }
        }

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
                x = overscrollDelta.x * rubberBandResistance(projected.x),
                y = overscrollDelta.y * rubberBandResistance(projected.y),
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
        overscrollOffset.animateTo(
            targetValue = Offset.Zero,
            initialVelocity = Offset(remaining.x, remaining.y),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
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
