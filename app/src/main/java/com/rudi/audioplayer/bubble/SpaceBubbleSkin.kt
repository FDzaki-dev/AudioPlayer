package com.rudi.audioplayer.bubble

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.rudi.audioplayer.util.PowerProbe
import com.rudi.audioplayer.util.SafeGuard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Batch 660 — SKIN SPACE untuk bubble mini player melayang. Bubble = overlay View (bukan Compose), jadi kosmos
// tema Space (SpaceCosmos.kt) DIGAMBAR ULANG pakai android.graphics di 3 Drawable: [SpaceSkinDrawable] (panel
// kapsul + lencana minimized: void gradien, 3 awan nebula hanyut, bintang berkelip + kilau silang, meteor, kilatan
// kaca, rim sweep cyan-violet-rose berputar, halo + cincin planet + bulan mengorbit), [SpaceOrbDrawable] (orb
// Play berpendar), [SpaceKeyDrawable] (kenop kristal Prev/Next). Aturan performa (pelajaran Batch 328/440):
// 0 alokasi di draw, shader dibangun SEKALI di onBoundsChange, jam ~30 fps (Handler 32 ms, Batch 668) yang BERHENTI
// otomatis saat view tak terlihat (setVisible -> layar mati / bubble diminimize = 0 biaya) dan statis bila animator
// sistem dimatikan; lencana minimized beku setelah [BADGE_LIVE_MS]. Warna = cermin token Color.kt (Space*), sengaja literal agar Service overlay tak menarik Compose.

internal object SpaceBubblePalette {
    val ACCENT: Int = 0xFF5CD6FF.toInt()
    val NEBULA_INDIGO: Int = 0xFF4B3FD8.toInt()
    val NEBULA_VIOLET: Int = 0xFF8E5CF7.toInt()
    val NEBULA_ROSE: Int = 0xFFE0589F.toInt()
    val STAR_WHITE: Int = 0xFFF2F5FF.toInt()
    val STAR_GOLD: Int = 0xFFFFD58A.toInt()
    val VOID_TOP: Int = 0xFF0C1030.toInt()
    val VOID_MID: Int = 0xFF150F40.toInt()
    val VOID_BOTTOM: Int = 0xFF080520.toInt()
    val ORB_HIGHLIGHT: Int = 0xFFE8FBFF.toInt()
    val ORB_DEEP: Int = 0xFF3B7DF0.toInt()
    val KEY_TOP: Int = 0xFF262E66.toInt()
    val KEY_MID: Int = 0xFF121738.toInt()
    val KEY_BOTTOM: Int = 0xFF0A0D22.toInt()

    const val TINT_WHITE = 0
    const val TINT_CYAN = 1
    const val TINT_GOLD = 2

    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    fun starColor(tint: Int): Int = when (tint) {
        TINT_CYAN -> ACCENT
        TINT_GOLD -> STAR_GOLD
        else -> STAR_WHITE
    }
}

/** Matematika murni (tanpa Android) — diuji di SpaceBubbleMathTest. */
internal object SpaceBubbleMath {
    const val TWO_PI = 6.2831855f

    fun wrapUnit(v: Float): Float = v - floor(v)

    /** Kedipan bintang 0,22..1,0 — kuadrat supaya sebagian besar waktu redup, sesekali berkilau. */
    fun twinkle(phase: Float, periodS: Float, t: Float): Float {
        if (periodS <= 0f) return 1f
        val k = 0.5f + 0.5f * sin(TWO_PI * (t / periodS + phase))
        return 0.22f + 0.78f * k * k
    }

    /** Kemajuan 0..1 selama jendela aktif tiap [periodS] detik; -1 bila sedang tidak aktif (meteor/kilatan). */
    fun burstProgress(t: Float, periodS: Float, activeFraction: Float, offsetS: Float): Float {
        if (periodS <= 0f || activeFraction <= 0f) return -1f
        val cycle = wrapUnit((t + offsetS) / periodS)
        return if (cycle < activeFraction) cycle / activeFraction else -1f
    }

    fun orbitAngle(t: Float, periodS: Float, startRad: Float): Float {
        if (periodS <= 0f) return startRad
        return startRad + TWO_PI * wrapUnit(t / periodS)
    }

    /** Denyut halus 0..1 (mulai 0,5). */
    fun pulse(t: Float, periodS: Float): Float {
        if (periodS <= 0f) return 1f
        return 0.5f + 0.5f * sin(TWO_PI * t / periodS)
    }
}

/** Posisi bintang dibangkitkan SEKALI & deterministik (seed tetap) — tak ada alokasi per frame. */
internal class SpaceBubbleStars(val count: Int, seed: Int) {
    val x = FloatArray(count)
    val y = FloatArray(count)
    val weight = FloatArray(count) // 0..1, 1 = paling terang/besar
    val phase = FloatArray(count)
    val period = FloatArray(count) // detik per satu kedipan penuh
    val tint = IntArray(count)

    init {
        val rnd = Random(seed)
        for (i in 0 until count) {
            x[i] = rnd.nextFloat()
            y[i] = rnd.nextFloat()
            val roll = rnd.nextFloat()
            weight[i] = roll * roll
            phase[i] = rnd.nextFloat()
            period[i] = 3f + rnd.nextFloat() * 6f
            val tintRoll = rnd.nextFloat()
            tint[i] = when {
                tintRoll < 0.58f -> SpaceBubblePalette.TINT_WHITE
                tintRoll < 0.84f -> SpaceBubblePalette.TINT_CYAN
                else -> SpaceBubblePalette.TINT_GOLD
            }
        }
    }
}

/** Pengatur jam (~30 fps) dipakai bersama oleh ketiga Drawable.
 *
 * Batch 668 (baterai) — dulu 1 `ValueAnimator` INFINITE: callback-nya datang di SETIAP vsync (120x/dtk di layar 120 Hz)
 * lalu 3 dari 4 dibuang oleh penyaring 32 ms, jadi thread utama + SurfaceFlinger dibangunkan 120x/dtk hanya untuk
 * menggambar ~30 frame. Kini `Handler.postDelayed` 32 ms (~31 bangun/dtk); gerak tetap berbasis waktu nyata
 * (`seconds` dari uptime), jadi kecepatan orbit/kedip tak berubah. [liveMs] > 0 = jam BEKU di frame terakhir setelah
 * sekian ms sejak [start] (dipakai lencana minimized: tak ada alasan menggambar ulang overlay 30x/dtk selamanya di
 * atas aplikasi lain); [start] berikutnya (view tampil lagi / layar menyala) menyalakannya lagi sebentar. */
private class SpaceClock(private val liveMs: Long = 0L, private val onFrame: (Float) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var active = false
    private var originMs = 0L
    private var liveStartMs = 0L
    var seconds = 0f
        private set

    val running: Boolean get() = active

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!active) return
            val now = SystemClock.uptimeMillis()
            seconds = (now - originMs) / 1000f
            onFrame(seconds)
            PowerProbe.hit(PowerProbe.Hit.BUBBLE_FRAME) // Batch 669 — penghitung frame untuk catcher daya
            if (liveMs > 0L && now - liveStartMs >= liveMs) {
                active = false // beku di frame yang baru digambar; tak ada tick terjadwal lagi
                return
            }
            handler.postDelayed(this, FRAME_GAP_MS)
        }
    }

    fun start() {
        if (active) return
        // Animator dimatikan sistem (hemat baterai / skala animasi 0) -> tampil statis di detik terakhir.
        if (!ValueAnimator.areAnimatorsEnabled()) return
        val now = SystemClock.uptimeMillis()
        originMs = now - (seconds * 1000f).toLong()
        liveStartMs = now
        active = true
        handler.postDelayed(tickRunnable, FRAME_GAP_MS)
    }

    fun stop() {
        active = false
        handler.removeCallbacks(tickRunnable)
    }

    private companion object {
        const val FRAME_GAP_MS = 32L
    }
}

private const val PANEL_STAR_COUNT = 28
private const val BADGE_STAR_COUNT = 9
private const val BADGE_LIVE_MS = 8_000L // Batch 668 — lencana minimized beku setelah 8 dtk tampil (0 = animasi selamanya)
private const val METEOR_PERIOD_S = 9f
private const val METEOR_ACTIVE = 0.16f
private const val SHIMMER_PERIOD_S = 6.5f
private const val SHIMMER_ACTIVE = 0.42f
private const val RIM_PERIOD_S = 10f
private const val MOON_PERIOD_S = 7f
private const val BADGE_PLANET_DP = 17f
private const val TRAIL_SEGMENTS = 7
private const val MOON_TAIL = 6

/**
 * Latar kapsul/lencana Space. [badge] = false: panel mini player (kapsul penuh); true: lencana 48dp saat diminimize
 * (lingkaran, planet di tengah otomatis). Pada panel, pusat+jari-jari planet diberikan Service lewat [setPlanet]
 * (diambil dari posisi nyata ImageView sampul, jadi selalu konsentris dgn sampul walau ukuran layout berubah).
 */
internal class SpaceSkinDrawable(private val density: Float, private val badge: Boolean) : Drawable(), Animatable {

    private val stars = SpaceBubbleStars(if (badge) BADGE_STAR_COUNT else PANEL_STAR_COUNT, if (badge) 6601 else 6602)
    private val clock = SpaceClock(if (badge) BADGE_LIVE_MS else 0L) { invalidateSelf() }

    private val clipPath = Path()
    private val rimPath = Path()
    private val matrixRim = Matrix()
    private val matrixRing = Matrix()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var bgShader: Shader? = null
    private var rimShader: SweepGradient? = null
    private var shimmerShader: Shader? = null
    private var haloShader: Shader? = null
    private var ringShader: SweepGradient? = null
    private val cloudShaders = arrayOfNulls<Shader>(CLOUD_COUNT)
    private val cloudCx = FloatArray(CLOUD_COUNT)
    private val cloudCy = FloatArray(CLOUD_COUNT)
    private val cloudR = FloatArray(CLOUD_COUNT)

    private var cornerPx = 0f
    private var shimmerWidth = 0f
    private var planetX = 0f
    private var planetY = 0f
    private var planetR = 0f
    private var failureReported = false

    fun setPlanet(cx: Float, cy: Float, r: Float) {
        if (planetX == cx && planetY == cy && planetR == r) return
        applyPlanet(cx, cy, r)
        invalidateSelf()
    }

    private fun applyPlanet(cx: Float, cy: Float, r: Float) {
        planetX = cx
        planetY = cy
        planetR = r
        if (r <= 0f) {
            haloShader = null
            ringShader = null
            return
        }
        val p = SpaceBubblePalette
        haloShader = RadialGradient(
            cx, cy, r * 2.1f,
            intArrayOf(p.withAlpha(p.ACCENT, 0.34f), p.withAlpha(p.NEBULA_VIOLET, 0.14f), p.withAlpha(p.NEBULA_VIOLET, 0f)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        ringShader = SweepGradient(cx, cy, rimColors(), RIM_STOPS)
    }

    private fun rimColors(): IntArray {
        val p = SpaceBubblePalette
        return intArrayOf(
            p.withAlpha(p.ACCENT, 0.95f),
            p.withAlpha(p.NEBULA_VIOLET, 0.9f),
            p.withAlpha(p.NEBULA_ROSE, 0.9f),
            p.withAlpha(p.ACCENT, 0.95f)
        )
    }

    private fun cloud(i: Int, cx: Float, cy: Float, r: Float, color: Int, alpha: Float) {
        val p = SpaceBubblePalette
        cloudCx[i] = cx
        cloudCy[i] = cy
        cloudR[i] = r
        cloudShaders[i] = RadialGradient(
            cx, cy, r,
            intArrayOf(p.withAlpha(color, alpha), p.withAlpha(color, alpha * 0.35f), p.withAlpha(color, 0f)),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val p = SpaceBubblePalette
        cornerPx = min(w, h) / 2f
        clipPath.reset()
        clipPath.addRoundRect(0f, 0f, w, h, cornerPx, cornerPx, Path.Direction.CW)
        val strokeW = RIM_DP * density
        val inset = strokeW / 2f
        rimPath.reset()
        rimPath.addRoundRect(inset, inset, w - inset, h - inset, cornerPx - inset, cornerPx - inset, Path.Direction.CW)
        rimPaint.strokeWidth = strokeW

        bgShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(p.VOID_TOP, p.VOID_MID, p.VOID_BOTTOM),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        val reach = max(w, h)
        cloud(0, w * 0.10f, h * 0.15f, reach * 0.62f, p.NEBULA_INDIGO, 0.60f)
        cloud(1, w * 0.62f, h * 1.00f, reach * 0.55f, p.NEBULA_VIOLET, 0.42f)
        cloud(2, w * 0.97f, h * 0.05f, reach * 0.40f, p.NEBULA_ROSE, 0.38f)
        rimShader = SweepGradient(w / 2f, h / 2f, rimColors(), RIM_STOPS)
        shimmerWidth = if (badge) w * 0.55f else w * 0.26f
        shimmerShader = LinearGradient(
            0f, 0f, shimmerWidth, 0f,
            intArrayOf(p.withAlpha(p.STAR_WHITE, 0f), p.withAlpha(p.STAR_WHITE, 0.20f), p.withAlpha(p.STAR_WHITE, 0f)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        if (badge) applyPlanet(w / 2f, h / 2f, BADGE_PLANET_DP * density)
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val saved = canvas.save()
        try {
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            val t = clock.seconds
            val sky = canvas.save()
            canvas.clipPath(clipPath)
            drawSky(canvas, w, h, t)
            canvas.restoreToCount(sky)
            drawRim(canvas, w, h, t)
            drawPlanetDecor(canvas, t)
        } catch (e: Exception) {
            if (!failureReported) {
                failureReported = true
                SafeGuard.report("SpaceSkinDrawable", "draw", e)
            }
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun drawSky(canvas: Canvas, w: Float, h: Float, t: Float) {
        fillPaint.shader = bgShader
        canvas.drawRect(0f, 0f, w, h, fillPaint)

        for (i in 0 until CLOUD_COUNT) {
            val shader = cloudShaders[i] ?: continue
            val dx = sin(t * CLOUD_RATE[i] + i * 1.7f) * 3f * density
            val dy = cos(t * CLOUD_RATE[i] * 0.8f + i * 0.9f) * 2f * density
            canvas.save()
            canvas.translate(dx, dy)
            cloudPaint.shader = shader
            canvas.drawCircle(cloudCx[i], cloudCy[i], cloudR[i], cloudPaint)
            canvas.restore()
        }

        if (planetR > 0f) {
            haloPaint.shader = haloShader
            haloPaint.alpha = (190f + 65f * SpaceBubbleMath.pulse(t, 4.2f)).toInt()
            canvas.drawCircle(planetX, planetY, planetR * 2.1f, haloPaint)
        }

        drawStars(canvas, w, h, t)
        drawMeteor(canvas, w, h, t)

        val sweep = SpaceBubbleMath.burstProgress(t, SHIMMER_PERIOD_S, SHIMMER_ACTIVE, 0f)
        if (sweep >= 0f) {
            val x = -shimmerWidth + (w + 2f * shimmerWidth) * sweep
            canvas.save()
            canvas.translate(x, 0f)
            canvas.skew(-0.35f, 0f)
            shimmerPaint.shader = shimmerShader
            canvas.drawRect(0f, 0f, shimmerWidth, h, shimmerPaint)
            canvas.restore()
        }
    }

    private fun drawStars(canvas: Canvas, w: Float, h: Float, t: Float) {
        val p = SpaceBubblePalette
        for (i in 0 until stars.count) {
            val px = stars.x[i] * w
            val py = stars.y[i] * h
            val tw = SpaceBubbleMath.twinkle(stars.phase[i], stars.period[i], t)
            val weight = stars.weight[i]
            val color = p.starColor(stars.tint[i])
            val a = tw * (0.35f + 0.65f * weight)
            starPaint.color = p.withAlpha(color, a)
            canvas.drawCircle(px, py, (0.45f + 0.95f * weight) * density, starPaint)
            if (weight > 0.55f) {
                val len = (1.8f + 2.6f * weight) * density * (0.6f + 0.4f * tw)
                linePaint.color = p.withAlpha(color, a * 0.8f)
                linePaint.strokeWidth = 0.6f * density
                canvas.drawLine(px - len, py, px + len, py, linePaint)
                canvas.drawLine(px, py - len, px, py + len, linePaint)
            }
        }
    }

    private fun drawMeteor(canvas: Canvas, w: Float, h: Float, t: Float) {
        val mp = SpaceBubbleMath.burstProgress(t, METEOR_PERIOD_S, METEOR_ACTIVE, 2.5f)
        if (mp < 0f) return
        val envelope = sin(PI.toFloat() * mp)
        val headX = w * (0.96f - 0.62f * mp)
        val headY = h * (-0.25f + 1.45f * mp)
        val stepX = w * 0.62f * 0.045f
        val stepY = h * 1.45f * 0.045f
        linePaint.strokeWidth = 1.1f * density
        for (k in 0 until TRAIL_SEGMENTS) {
            val a = envelope * (1f - k / TRAIL_SEGMENTS.toFloat()) * 0.85f
            linePaint.color = SpaceBubblePalette.withAlpha(SpaceBubblePalette.STAR_WHITE, a)
            canvas.drawLine(
                headX + stepX * k, headY - stepY * k,
                headX + stepX * (k + 1), headY - stepY * (k + 1),
                linePaint
            )
        }
    }

    private fun drawRim(canvas: Canvas, w: Float, h: Float, t: Float) {
        val p = SpaceBubblePalette
        glowPaint.color = p.withAlpha(p.ACCENT, 0.07f)
        glowPaint.strokeWidth = 5f * density
        canvas.drawPath(rimPath, glowPaint)
        glowPaint.color = p.withAlpha(p.NEBULA_VIOLET, 0.12f)
        glowPaint.strokeWidth = 3f * density
        canvas.drawPath(rimPath, glowPaint)
        val sweep = rimShader ?: return
        matrixRim.setRotate(360f * SpaceBubbleMath.wrapUnit(t / RIM_PERIOD_S), w / 2f, h / 2f)
        sweep.setLocalMatrix(matrixRim)
        rimPaint.shader = sweep
        canvas.drawPath(rimPath, rimPaint)
    }

    private fun drawPlanetDecor(canvas: Canvas, t: Float) {
        if (planetR <= 0f) return
        val p = SpaceBubblePalette
        val ring = ringShader
        if (ring != null) {
            matrixRing.setRotate(-360f * SpaceBubbleMath.wrapUnit(t / (RIM_PERIOD_S * 0.8f)), planetX, planetY)
            ring.setLocalMatrix(matrixRing)
            ringPaint.shader = ring
            ringPaint.strokeWidth = 2f * density
            canvas.drawCircle(planetX, planetY, planetR + 1.2f * density, ringPaint)
        }
        val orbitR = planetR + 5.5f * density
        orbitPaint.color = p.withAlpha(p.STAR_WHITE, 0.16f)
        orbitPaint.strokeWidth = 0.7f * density
        canvas.drawCircle(planetX, planetY, orbitR, orbitPaint)

        val theta = SpaceBubbleMath.orbitAngle(t, MOON_PERIOD_S, 0.9f)
        for (k in MOON_TAIL downTo 1) {
            val a = theta - k * 0.16f
            moonPaint.color = p.withAlpha(p.STAR_GOLD, (1f - k / (MOON_TAIL + 1f)) * 0.5f)
            canvas.drawCircle(
                planetX + orbitR * cos(a), planetY + orbitR * sin(a),
                (1.7f - 0.2f * k) * density, moonPaint
            )
        }
        val mx = planetX + orbitR * cos(theta)
        val my = planetY + orbitR * sin(theta)
        moonPaint.color = p.withAlpha(p.STAR_GOLD, 0.28f)
        canvas.drawCircle(mx, my, 3.6f * density, moonPaint)
        moonPaint.color = p.STAR_WHITE
        canvas.drawCircle(mx, my, 1.7f * density, moonPaint)

        // Satelit kecil berlawanan arah, berkelip.
        val phi = SpaceBubbleMath.orbitAngle(t, MOON_PERIOD_S * 1.7f, 0.9f + PI.toFloat())
        moonPaint.color = p.withAlpha(p.ACCENT, SpaceBubbleMath.twinkle(0.3f, 2.6f, t))
        canvas.drawCircle(planetX + orbitR * cos(-phi), planetY + orbitR * sin(-phi), 1f * density, moonPaint)
    }

    override fun getOutline(outline: Outline) {
        outline.setRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, cornerPx)
        outline.alpha = 1f
    }

    override fun start() = clock.start()
    override fun stop() = clock.stop()
    override fun isRunning(): Boolean = clock.running

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (visible) clock.start() else clock.stop()
        return changed
    }

    // Alpha/fade idle ditangani View.alpha di FloatingBubbleService, bukan di sini.
    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val CLOUD_COUNT = 3
        const val RIM_DP = 1.5f
        val CLOUD_RATE = floatArrayOf(0.35f, 0.28f, 0.42f)
        val RIM_STOPS = floatArrayOf(0f, 0.34f, 0.67f, 1f)
    }
}

/** Orb Play berpendar: aura berdenyut + riak melebar saat memutar, inti gradien cyan->indigo, kilau bintang. */
internal class SpaceOrbDrawable(private val density: Float) : Drawable(), Animatable {

    private val clock = SpaceClock { invalidateSelf() }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private var auraShader: Shader? = null
    private var coreShader: Shader? = null
    private var playing = false
    private var failureReported = false

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val p = SpaceBubblePalette
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f
        val coreR = r * CORE_FRACTION
        auraShader = RadialGradient(
            cx, cy, r,
            intArrayOf(p.withAlpha(p.ACCENT, 0.60f), p.withAlpha(p.ACCENT, 0.40f), p.withAlpha(p.NEBULA_VIOLET, 0f)),
            floatArrayOf(0f, CORE_FRACTION, 1f), Shader.TileMode.CLAMP
        )
        coreShader = RadialGradient(
            cx - 0.25f * coreR, cy - 0.30f * coreR, coreR * 1.5f,
            intArrayOf(p.ORB_HIGHLIGHT, p.ACCENT, p.ORB_DEEP, p.NEBULA_INDIGO),
            floatArrayOf(0f, 0.38f, 0.75f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val saved = canvas.save()
        try {
            canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
            drawOrb(canvas, w / 2f, h / 2f, min(w, h) / 2f, clock.seconds)
        } catch (e: Exception) {
            if (!failureReported) {
                failureReported = true
                SafeGuard.report("SpaceOrbDrawable", "draw", e)
            }
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun drawOrb(canvas: Canvas, cx: Float, cy: Float, r: Float, t: Float) {
        val p = SpaceBubblePalette
        val coreR = r * CORE_FRACTION
        val scale = if (playing) 0.84f + 0.16f * SpaceBubbleMath.pulse(t, 1.8f) else 0.82f
        canvas.save()
        canvas.scale(scale, scale, cx, cy)
        auraPaint.shader = auraShader
        auraPaint.alpha = if (playing) 255 else 150
        canvas.drawCircle(cx, cy, r, auraPaint)
        canvas.restore()

        if (playing) {
            val rp = SpaceBubbleMath.wrapUnit(t / 2.4f)
            ripplePaint.strokeWidth = 1.2f * density
            ripplePaint.color = p.withAlpha(p.ACCENT, (1f - rp) * 0.55f)
            canvas.drawCircle(cx, cy, coreR + (r - coreR) * rp, ripplePaint)
        }

        corePaint.shader = coreShader
        canvas.drawCircle(cx, cy, coreR, corePaint)
        rimPaint.strokeWidth = 1f * density
        rimPaint.color = p.withAlpha(p.STAR_WHITE, 0.55f)
        canvas.drawCircle(cx, cy, coreR - 0.5f * density, rimPaint)

        val gx = cx + 0.46f * coreR
        val gy = cy - 0.50f * coreR
        val tw = SpaceBubbleMath.twinkle(0.15f, 3.1f, t)
        val len = 3f * density * tw
        glintPaint.strokeWidth = 0.7f * density
        glintPaint.color = p.withAlpha(p.STAR_WHITE, 0.4f + 0.6f * tw)
        canvas.drawLine(gx - len, gy, gx + len, gy, glintPaint)
        canvas.drawLine(gx, gy - len, gx, gy + len, glintPaint)
    }

    override fun getOutline(outline: Outline) {
        outline.setOval(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    override fun start() = clock.start()
    override fun stop() = clock.stop()
    override fun isRunning(): Boolean = clock.running

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (visible) clock.start() else clock.stop()
        return changed
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val CORE_FRACTION = 0.70f
    }
}

/** Kenop kristal Prev/Next: cakram malam gradien, rim cyan tipis, busur kilau di atas. Statis (0 animasi). */
internal class SpaceKeyDrawable(private val density: Float) : Drawable() {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arcRect = RectF()
    private var shader: Shader? = null

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val p = SpaceBubblePalette
        val r = min(w, h) / 2f
        shader = RadialGradient(
            w / 2f - 0.2f * r, h / 2f - 0.3f * r, r * 1.3f,
            intArrayOf(p.KEY_TOP, p.KEY_MID, p.KEY_BOTTOM),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val p = SpaceBubblePalette
        val r = min(w, h) / 2f
        val saved = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        fillPaint.shader = shader
        canvas.drawCircle(w / 2f, h / 2f, r, fillPaint)
        rimPaint.strokeWidth = 1f * density
        rimPaint.color = p.withAlpha(p.ACCENT, 0.38f)
        canvas.drawCircle(w / 2f, h / 2f, r - 0.6f * density, rimPaint)
        val inset = 2.4f * density
        arcRect.set(inset, inset, w - inset, h - inset)
        arcPaint.strokeWidth = 1f * density
        arcPaint.color = p.withAlpha(p.STAR_WHITE, 0.16f)
        canvas.drawArc(arcRect, 200f, 140f, false, arcPaint)
        canvas.restoreToCount(saved)
    }

    override fun getOutline(outline: Outline) {
        outline.setOval(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
