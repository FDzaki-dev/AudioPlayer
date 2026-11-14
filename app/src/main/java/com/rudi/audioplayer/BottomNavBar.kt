package com.rudi.audioplayer

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudi.audioplayer.ui.theme.isSkeuTheme

// Batch 522 — Wave 2 T10 (MOVE-ONLY, R3; eksekusi ulang — percobaan pertama Batch 520 ditarik Batch 521 karena laporan performa
// turun, lalu user menduga penyebabnya HP overheat): MagnifyingTabLabel/GlassTabIcon/NoRippleIndication/CustomNavBarTabItem dipindah
// dari MainActivity.kt (paket sama `com.rudi.audioplayer`, jadi 0 import baru di MainActivity). Badan + komentar identik
// karakter-per-karakter; satu-satunya beda = `private` -> `internal` pada 2 fungsi yang dipanggil dari AppNavHost
// (GlassTabIcon, CustomNavBarTabItem). MagnifyingTabLabel (dipakai GlassTabIcon di file ini) dan NoRippleIndication
// (0 pemakai di seluruh source sejak Batch 449) SENGAJA tetap private. AppNavHost TIDAK dipecah. Lihat CHANGELOG Batch 522.

// Batch 437 — request eksplisit user: label NavigationBarItem bawah (Beranda/Perpustakaan/
// Pengaturan) diganti dari `Text("Beranda")` polos jadi composable ini — efek "kaca pembesar"
// ala iOS, ukuran/blur/opacity Text bereaksi KONTINU terhadap `focus` (0f..1f, lihat
// `tabMagnifyFocus` di AppNavHost) alih-alih cuma snap ON/OFF ikut boolean `selected`. `style`
// diturunkan dari `LocalTextStyle.current` (bukan style baru) — hanya `fontSize` yang
// di-override eksplisit, warna (selected/unselected, dianimasikan sendiri oleh M3 lewat
// LocalContentColor) & sisanya (letterSpacing/lineHeight/fontWeight) TETAP ikut identitas tema
// aktif apa pun (Apple/Tactile/SkeuDarkLite/LiquidGlass/dst) — 0 hardcode warna baru. `blur()`
// dipakai tanpa percabangan Build.VERSION: minSdk project ini sudah 31, RenderEffect (dasar
// Modifier.blur di Compose) tersedia sejak API 31.
// Batch 442 — fix bug (user screenshot: "Perpustakaan"/"Pengaturan" terpotong jadi
// "Perpusta"/"Pengatur", pill "Beranda" tampak anomali besar). Root cause: Batch 439 memindah
// composable ini dari slot `label` NavigationBarItem (M3 otomatis bungkus slot itu dgn
// ProvideTextStyle(labelMedium)) ke dalam slot `icon` (lihat `GlassTabIcon`) — di slot `icon`,
// `LocalTextStyle.current` TIDAK di-provide M3 sbg style label kecil, jatuh balik ke ambient
// default di root MaterialTheme (bodyLarge, jauh lebih besar dari label nav semestinya) —
// persis item "belum-terverifikasi" yang sudah diperingatkan PROJECT_STATE.md sejak Batch 439
// ("pill gabungan ikon+label ... x font-scale besar") sebelum device asli tersedia utk
// mengonfirmasi. Fix: baca style resmi label nav LANGSUNG dari token M3
// (`MaterialTheme.typography.labelMedium`, IDENTIK dgn yang M3 pakai di slot `label` default)
// alih-alih ambient yang salah — 0 hardcode sp baru, tetap ikut identitas tema aktif apa pun.
// `overflow = TextOverflow.Ellipsis` ditambah sbg jaring pengaman (mis. font-scale aksesibilitas
// besar) — dulu 0 di-set (default Clip), itu sebabnya kliping lama menghasilkan huruf terpotong
// mentah alih-alih "..." yang jelas.
// Batch 438 — request eksplisit user: lampiran `drag_drop_glass_ios_kotlin.md` + screenshot
// bottom nav, "hasil sebelumnya (Batch 437, efek kaca PEMBESAR di LABEL) mengecewakan ...
// adaptasi 100% berdasarkan panduan". Panduan asli = demo generik (bukan app ini): 3 tab
// custom draggable-reorder + `Modifier.blur(20.dp)` di container. 2 bagian TIDAK dipakai
// literal — bukan penolakan, adaptasi ke arsitektur riil (SOP §HIGH-RISK ADAPTABILITY):
//   1. Reorder drag-to-swap: 3 tab ini route top-level Nav Compose permanen
//      (home/library/settings — dipakai state-restoration Batch 301, gesture-swipe Batch 435,
//      NavigationRailItem tablet). Reorder mengubah pasangan ikon<->rute jadi tidak tetap,
//      breaking change jauh di luar scope "efek visual kaca" yang diminta (pola penolakan sama
//      persis swap-ke-HorizontalPager Batch 435). Interaksi drag guide diadaptasi jadi tap
//      press-scale saja (lihat `bouncyPress` di bawah) — gesture tap tetap 100% dipegang
//      `NavigationBarItem` sendiri, 0 `pointerInput` kustom baru (kalau dipasang akan bersaing
//      gesture dgn klik pindah tab = risiko regresi persis yg diperingatkan SOP §HIGH-RISK).
//   2. `Modifier.blur(20.dp)` di container: PERSIS anti-pattern yang didokumentasikan sendiri
//      oleh proyek ini di `BlurUtils.kt` (blur() mengaburkan KONTEN sendiri, ikon/teks di
//      dalamnya ikut buram — bukan "kaca" yg dimaksud), DAN blur asli (Haze/`hazeEffect`) sudah
//      DIMATIKAN PERMANEN app-wide (Batch 329, root cause: stutter musik device asli — keputusan
//      eksplisit user, tidak diaktifkan ulang batch ini). Diadaptasi jadi translucent-tint +
//      border tipis (teknik sama `frostedGlass()`), TIDAK memanggil `frostedGlass()` langsung
//      karena shape-nya (`MaterialTheme.shapes.large`) + alpha-nya (0.92/0.96, disetel utk panel
//      besar/card/sheet supaya tetap kebaca TANPA blur asli di belakangnya) didesain utk panel
//      besar, bukan pill nav sekecil ini — dipakai versi lokal skala-pill di sini (0 perubahan
//      ke `BlurUtils.kt`/12+ call site lain, sesuai batas 3 file/tugas).
// Elemen guide yang DIPAKAI: translucent overlay tipis + border rim tipis (glass rim) + scale
// saat berinteraksi (guide: 1.08 saat drag-hold; di sini: `bouncyPress` 0.9 saat tap-press,
// konvensi tekan-tactile yg SUDAH dipakai LockScreen/MiniPlayerBar/dst — bukan sistem baru).
// `isSkeuTheme()` DIKECUALIKAN dari efek glass ini — identitas ini punya aturan tegas sejak
// Batch 58/61/79 "panel solid, bukan lapisan kaca, 0 garis tepi apa pun", berlaku app-wide (0
// spesifik ke nav) — pill Skeu tetap solid (indicator M3 default look, direplikasi manual krn
// `indicatorColor` M3 dimatikan/transparent di titik pemakaian, lihat `AppNavHost`).
// Utk 5 identitas lain: catatan lama Batch 53 "§15 jangan jadikan nav item glowing glass
// capsule" DISUPERSEDE eksplisit oleh instruksi user batch ini (kaskade DESCENDING TRUTH SOP:
// instruksi eksplisit baru > catatan/spec lama) — didokumentasikan di PROJECT_STATE.md/README.md,
// bukan dihapus diam-diam.
// Batch 439 — permintaan eksplisit user: 2 screenshot referensi (nav app ini vs tab bar iOS
// Jam/Clock) + instruksi "perbaiki bottom nav bar agar lebih mirip gaya visual iOS app jam
// tersebut, matikan ripple khas Android saat klik". Dibanding referensi iOS Jam, gap utama pill
// Batch 438 (di atas) cuma membungkus IKON — di iOS Jam, highlight tab aktif membungkus IKON+
// LABEL sekaligus jadi satu blok. `GlassTabIcon` diperluas ambil alih slot label juga (param
// `label`/`focus` baru, dipanggil balik ke `MagnifyingTabLabel` yang SAMA PERSIS, 0 logic
// pembesar Batch 437 diubah) lalu 3 titik pemakaian di bawah (`icon = { GlassTabIcon(...) }`)
// melepas parameter `label = { MagnifyingTabLabel(...) }` milik `NavigationBarItem` (M3 selalu
// naruh label itu di SLOT terpisah di bawah ikon, tidak bisa disatukan ke 1 pill dari luar
// composable-nya) — 0 breaking ke `NavigationBarItem` sendiri, cuma pindah tempat rendernya.
// Bentuk pill juga diganti dari stadium penuh (`percent = 50`, cocok utk lingkaran-ikon-saja)
// jadi `RoundedCornerShape(16.dp)` — kotak rounded, sama seperti referensi iOS Jam yang
// membungkus blok ikon+teks (stadium penuh di blok setinggi itu akan terlihat seperti kapsul
// obat, bukan seperti referensi). `NavigationRailItem` (tablet) TIDAK disentuh — tidak dipakai
// `GlassTabIcon` sama sekali (lihat definisinya di `AppNavHost`, pakai `Icon`/`Text` polos), di
// luar scope 2 screenshot yang keduanya nav ponsel.
// Batch 657 — GlassTabIcon DIROMBAK sesuai video acuan (ANTI-STALE: komentar Batch 437-556 di atas = riwayat; mekanisme lerp warna
// `focus`, `MagnifyingTabLabel` (skala 1,23x + alpha 0,68) dan `bouncyPress` (0,9x) sudah TIDAK ada). Video acuan: ikon/label tidak
// membesar per-tab (yang membesar = BILAH, di `AppNavHost`) dan warna aksen TIDAK memudar linear, melainkan ter-klip tepat di bentuk
// pil (ikon separuh berwarna saat pil melintas). Fungsi ini kini murni "wajah" statis: `highlighted=false` = warna tak-terpilih,
// `highlighted=true` = warna terpilih (token M3 utk Skeu, `primary` utk tema lain — identik dgn titik akhir lerp lama).
// `CustomNavBarTabItem` memanggilnya 2x (dasar + salinan aksen yang di-klip ke pil), jadi WAJIB murni.
@Composable
internal fun GlassTabIcon(
    icon: ImageVector,
    label: String,
    highlighted: Boolean
) {
    val isSkeu = isSkeuTheme()
    val itemColors = NavigationBarItemDefaults.colors()
    val tint = MaterialTheme.colorScheme.primary
    val iconColor = when {
        !highlighted -> itemColors.unselectedIconColor
        isSkeu -> itemColors.selectedIconColor
        else -> tint
    }
    val labelColor = when {
        !highlighted -> itemColors.unselectedTextColor
        isSkeu -> itemColors.selectedTextColor
        else -> tint
    }
    Column(
        // Padding horizontal 4.dp (Batch 452: cegah ellipsis "Perpustakaan"/"Pengaturan") dipertahankan; area sentuh = slot
        // `CustomNavBarTabItem`, bukan padding ini.
        modifier = Modifier
            .widthIn(min = 64.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = iconColor)
        FittedNavLabel(text = label, color = labelColor)
    }
}

// Batch 658 — user: "judul tab kebesaran sehingga berubah menjadi ....." (v657). AKAR: label nav memakai `labelMedium` apa adanya
// (12-13sp x skala font sistem) di slot yang hanya ±96-113dp (lebar layar 360-411dp: (bilah - 16dp jarak) / 3 - 8dp padding); "Perpustakaan"
// pada 13sp + font 1,15x = ±98dp > 96dp di layar 360dp -> ellipsis (Batch 442/452 hanya melonggarkan padding, tak menjamin muat).
// FIX: ukuran huruf label MENYUSUT bertahap (langkah 0,5sp, lantai 9sp) sampai muat satu baris di lebar yang tersedia; muat di ukuran
// aslinya = 0 perubahan (tak menyusut). Diukur SINKRON saat komposisi (`rememberTextMeasurer` + kunci `remember`), jadi lapisan dasar dan
// salinan aksen `CustomNavBarTabItem` (2 pemanggil `GlassTabIcon`) mendapat ukuran IDENTIK tanpa berkedip. `lineHeight`/`letterSpacing`
// tetap (hanya `fontSize` yang berubah) -> tinggi label/bilah/pil TIDAK berubah. Ellipsis tetap jaring pengaman bila 9sp pun tak muat.
internal const val NAV_LABEL_MIN_SP = 9f
internal const val NAV_LABEL_STEP_SP = 0.5f

/**
 * Ukuran huruf (sp) terbesar dari [baseSp] turun per [stepSp] sampai [fits] true; tak pernah di bawah [minSp].
 * [baseSp] sudah muat -> [baseSp]; tak pernah muat -> [minSp]. Fungsi murni (diuji `TabPillMathTest`).
 */
internal fun fitNavLabelSp(baseSp: Float, minSp: Float, stepSp: Float, fits: (Float) -> Boolean): Float {
    if (stepSp <= 0f || baseSp <= minSp) return baseSp
    var size = baseSp
    while (size > minSp && !fits(size)) size -= stepSp
    return maxOf(size, minSp)
}

@Composable
private fun FittedNavLabel(text: String, color: Color) {
    val base = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val density = LocalDensity.current
    BoxWithConstraints {
        val maxPx = constraints.maxWidth
        val style = remember(text, base, maxPx, density) {
            if (maxPx == Constraints.Infinity || base.fontSize == TextUnit.Unspecified) {
                base
            } else {
                val fitted = fitNavLabelSp(base.fontSize.value, NAV_LABEL_MIN_SP, NAV_LABEL_STEP_SP) { sizeSp ->
                    measurer.measure(
                        text = text,
                        style = base.copy(fontSize = sizeSp.sp),
                        softWrap = false,
                        maxLines = 1,
                        density = density
                    ).size.width <= maxPx
                }
                base.copy(fontSize = fitted.sp)
            }
        }
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = style,
            color = color
        )
    }
}

// Batch 439 — 0 dampak ke `selected`/klik: `Indication` kosong (cuma `drawContent()`, 0 layer
// visual digambar) dipasang lewat `CompositionLocalProvider(LocalIndication provides ...)` di
// `bottomBar`, BUKAN `Modifier.clickable` baru. `NavigationBarItem` (M3) membaca ripple-nya dari
// `LocalIndication.current` secara internal persis seperti komponen selectable/clickable
// Compose Foundation lain — override di titik pemakaian ini cukup, 0 perlu sentuh
// `NavigationBarItem`/`selected`/route logic sama sekali. Efek gelombang ripple bawaan Android
// hilang, tapi warna & scale-down `bouncyPress` (Batch 438) di `GlassTabIcon` tetap jalan penuh
// (2 mekanisme feedback tekan yang independen) — cocok dengan referensi iOS Jam yang 0 ripple
// tapi tetap ada feedback visual saat tab ditekan.
// Batch 440 — implementasi kontrak `Indication`/`IndicationInstance` (`rememberUpdatedInstance`
// + objek `drawIndication()`) di atas kini HARD ERROR compiler (bukan lagi cuma deprecated
// warning) di compose-bom 2026.04.01 — `compileDebugKotlin`/`compileReleaseKotlin` FAILED,
// lihat `log_fail_424.zip`. Migrasi ke kontrak resmi pengganti (`IndicationNodeFactory` +
// `Modifier.Node`/`DrawModifierNode`) — 0 behavior berubah, MASIH murni `drawContent()` kosong,
// 0 layer visual digambar, titik pemakaian `CompositionLocalProvider(LocalIndication provides
// NoRippleIndication)` di `bottomBar` TIDAK disentuh (tetap kompatibel — `IndicationNodeFactory`
// adalah subtipe `Indication`).
private object NoRippleIndication : IndicationNodeFactory {
    private class NoRippleIndicationNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NoRippleIndicationNode()
    }

    // Batch 441 — trigger `log_fail_425.zip`: kontrak `IndicationNodeFactory` (Batch 440)
    // me-re-abstract `equals`/`hashCode` (bukan cuma warisan default `Any`, interface-nya sendiri
    // deklarasi ulang keduanya sbg abstract) — WAJIB diimplementasi eksplisit di titik
    // implementasi, `object` Kotlin TIDAK otomatis dianggap cukup oleh compiler walau secara
    // semantik singleton sudah unik. Identity check sederhana cukup: 1 instance tunggal
    // sepanjang hidup app, 0 state yang membedakan.
    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = -1
}

// Batch 449 — user device-test Batch 448 (video asli, tab bar unified-pill): seam/kotak-ganda
// pill 0 masalah (fix Batch 448 terkonfirmasi device fisik). 1 temuan baru: kilatan kotak abu-abu
// ~0.25 detik di tab yang BARU DITINGGALKAN, tiap pindah tab. Root cause BUKAN `GlassTabIcon`/
// pill unified Batch 448 (0 disentuh batch ini) — `NavigationBarItem` M3 itu sendiri.
// `indicatorColor = Color.Transparent` (titik pemakaian lama) + `NoRippleIndication` (Batch 439,
// override `LocalIndication`) TERBUKTI 0 cukup: `NavigationBarItem` versi M3 dipakai project ini
// construct ripple/state-layer LANGSUNG di titik panggil internalnya sendiri (hardcoded),
// BUKAN baca `LocalIndication.current` — override composition-local Batch 439 TIDAK PERNAH
// menyentuh mekanisme itu. Kotak abu-abu = fade-out state-layer bawaan tsb (durasi default match
// ~0.25s persis laporan user). Instruksi eksplisit user: rombak total (bukan tempel workaround
// ke-2 di atas yang sudah gagal) — `NavigationBarItem` DIHAPUS TOTAL dari 3 titik pemakaian
// `bottomBar` (`NavigationRailItem` tablet TIDAK disentuh — 0 laporan bug di situ, di luar scope
// temuan ini, 0 sektor baru dibuka).
// Pengganti: composable ini (Row+Box manual) — `NavigationBar` (composable pembungkus M3, TETAP
// dipakai, TIDAK dihapus — insets/elevation/clip-kapsul/pill drawWithContent Batch 448 di
// `AppNavHost` 0 disentuh) sudah menyediakan `RowScope` di content lambda-nya (itu sebabnya
// `NavigationBarItem` versi lama bisa pakai `Modifier.weight(1f, true)` internal utk bagi rata 3
// kolom) — direplikasi manual di bawah, `Modifier.weight(1f, fill = true)` PERSIS sama, 0 kolom
// jadi tidak-rata. Isi tiap `Box`: cuma `content()` (GlassTabIcon, icon+label sudah 1 slot sejak
// Batch 439, 0 disentuh) dibungkus `Modifier.selectable(indication = null, ...)` — kontrak RESMI
// Compose Foundation, param `indication` eksplisit SELALU menang di titik panggil (0 celah spt
// `NavigationBarItem` internal di atas, 0 override composition-local diperlukan lagi).
// `role = Role.Tab` + `selected` otomatis jadi semantics oleh `.selectable()` sendiri — kontrak
// aksesibilitas TalkBack/screen-reader IDENTIK milik `NavigationBarItem` lama, 0 regresi.
// `bouncyPress` (Batch 438, di `GlassTabIcon`) baca interactionSource yang SAMA yang dipasang di
// `.selectable()` ini — press-feedback 0 berubah, cuma pindah sumber pengumpul event dari
// `NavigationBarItem` internal ke `.selectable()` manual ini.
// `NoRippleIndication` (definisi di atas) TIDAK dihapus — riwayat arsitektur (log_fail_424/425),
// 0 dipakai lagi di titik ini SAJA, tetap tersedia kalau ada kebutuhan lain nanti.
// Efek samping WAJIB ikut diperbaiki (lihat `GlassTabIcon`, blok Skeu icon+label): warna Skeu
// dulu implisit lewat `LocalContentColor` yang disuplai `NavigationBarItem` — tanpa itu, ikon/
// label Skeu akan STATIS 1 warna (regresi fungsional, bukan cuma soal gray-flash) — diganti snap
// biner eksplisit ke token `NavigationBarItemDefaults` yang sama, 0 lerp (aturan solid Batch
// 58/61/79 tidak disentuh).
// Batch 450 — REGRESI FATAL: user lampirkan video, pill (`drawWithContent` Batch 448 di
// `NavigationBar`) melar jadi kapsul raksasa (dari pertengahan layar sampai hampir dasar layar),
// bukan lagi pas di belakang ikon+label. Root cause: `Modifier.fillMaxHeight()` DITAMBAHKAN di
// `Box` bawah ini (Batch 449) — asumsi keliru bahwa `NavigationBarItem` internal juga
// `fillMaxHeight()` (TIDAK PERNAH diverifikasi ke source asli M3 sebelum ditulis, cuma tebakan).
// Faktanya `NavigationBarItem` cuma bungkus konten ke ukuran NATURAL (icon+label, wrap-content),
// TIDAK PERNAH fillMaxHeight. `Box(fillMaxHeight())` di sini bikin `Row` konten `NavigationBar`
// (non-weighted child di Column luar, diukur dgn constraint maxHeight LONGGAR/belum dipotong)
// ikut melar minta tinggi maksimal yg tersedia — `NavigationBar` (composable pembungkus) jadi
// jauh lebih tinggi dari seharusnya, dan pill `drawWithContent` yg skalanya ikut `size.height`
// composable itu ikut melar sama persis. FIX: `.fillMaxHeight()` DIHAPUS TOTAL, `Box` kembali
// wrap-content PERSIS kontrak asli `NavigationBarItem` (0 pengganti lain dibutuhkan — konten
// `GlassTabIcon` sendiri sudah py padding/minWidth cukup utk touch target, terbukti device Batch
// 448 sebelum Batch 449 mengacaukannya). PELAJARAN: modifier layout (`fillMaxHeight`/`weight`/dst)
// yg meniru API resmi WAJIB diverifikasi ke source/dokumentasi asli dulu, bukan diasumsikan dari
// pola modifier lain di codebase yg mirip tapi beda konteks — kesalahan 1 modifier bisa merusak
// SELURUH bottom nav (bukan cuma 1 tab), lebih parah dari bug yg sedang diperbaiki.
// Batch 657 — geometri pil + pemetaan jari (fungsi MURNI, diuji `TabPillMathTest`). Rumus `tabPillLeft` = rumus `neuKeyDraw`
// (TactileDepth.kt: `(position * slotW - kw / 2).coerceIn(0, width - kw)`), karena lapisan warna ter-klip di bawah WAJIB sama persis
// dgn kunci timbul yang digambar `neuSlidingKey` (74% lebar slot, 62% tinggi bar — nilai yang sama dikirim MainActivity).
internal const val TAB_PILL_WIDTH_FRACTION = 0.74f
internal const val TAB_PILL_HEIGHT_FRACTION = 0.62f
private const val TAB_PILL_CORNER_DP = 16f

/** Indeks tab di bawah koordinat-x jari (0 until [count]); lebar/jumlah tidak valid -> 0. */
internal fun tabIndexAt(x: Float, width: Float, count: Int): Int {
    if (width <= 0f || count <= 0) return 0
    return (x / width * count).toInt().coerceIn(0, count - 1)
}

/** Pusat pil (satuan tab) yang diikuti jari: dijepit ke pusat tab pertama (0,5) dan terakhir ([count] - 0,5). */
internal fun tabPillTarget(x: Float, width: Float, count: Int): Float {
    if (width <= 0f || count <= 0) return 0.5f
    return (x / width * count).coerceIn(0.5f, count.toFloat() - 0.5f)
}

/** Tepi kiri kunci/pil (px, koordinat bilah) utk pusat [position] (satuan tab) — identik rumus `neuKeyDraw`. */
internal fun tabPillLeft(position: Float, barWidth: Float, count: Int, widthFraction: Float): Float {
    val slotW = barWidth / count.coerceAtLeast(1)
    val keyW = slotW * widthFraction
    return (position * slotW - keyW / 2f).coerceIn(0f, maxOf(0f, barWidth - keyW))
}

// Geometri slot hasil `onPlaced`/`onSizeChanged` (bidang polos, BUKAN state: hanya dibaca di fase gambar, dan perubahan ukuran selalu
// disertai relayout+redraw; menghindari rekomposisi per perubahan ukuran).
private class TabSlotGeometry {
    var left = 0f
    var top = 0f
    var barWidth = 0f
    var barHeight = 0f
    var faceWidth = 0f
    var faceHeight = 0f
}

// Batch 657 — slot tab dgn DUA lapisan (video acuan: warna aksen hanya di dalam bentuk pil): lapisan dasar `content(false)` + di atasnya
// salinan `content(true)` yang digambar ter-klip ke persegi-membulat pil (pusat `pillPosition()`, ukuran = kunci timbul, minimal seukuran
// wajah ikon+label agar label panjang/font besar tak setengah-abu saat diam). `revealAll()` = true -> salinan aksen digambar PENUH tanpa
// klip (tab rute yang sudah aktif tetap berwarna selama jari menekan bilah, seperti video). Semua dibaca di fase GAMBAR.
// Salinan aksen: `clearAndSetSemantics` agar TalkBack tak membaca label dua kali. Wrap-content & tanpa `fillMaxHeight` (pelajaran
// Batch 450 di atas dipertahankan): koordinat slot dibaca dari `onPlaced`, bukan diasumsikan.
@Composable
internal fun RowScope.CustomNavBarTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    tabCount: Int,
    pillPosition: () -> Float,
    revealAll: () -> Boolean,
    content: @Composable (highlighted: Boolean) -> Unit
) {
    val geometry = remember { TabSlotGeometry() }
    val pillClipPath = remember { Path() }
    Box(
        modifier = Modifier
            .weight(1f, fill = true)
            .onPlaced { coordinates ->
                val position = coordinates.positionInParent()
                geometry.left = position.x
                geometry.top = position.y
                val parent = coordinates.parentLayoutCoordinates
                geometry.barWidth = (parent?.size?.width ?: 0).toFloat()
                geometry.barHeight = (parent?.size?.height ?: 0).toFloat()
            }
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        content(false)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics { }
                .drawWithContent {
                    if (revealAll()) {
                        drawContent()
                    } else if (geometry.barWidth > 0f && geometry.barHeight > 0f) {
                        val barWidth = geometry.barWidth
                        val keyW = barWidth / tabCount.coerceAtLeast(1) * TAB_PILL_WIDTH_FRACTION
                        val keyH = geometry.barHeight * TAB_PILL_HEIGHT_FRACTION
                        val keyLeft = tabPillLeft(pillPosition(), barWidth, tabCount, TAB_PILL_WIDTH_FRACTION)
                        val coverW = maxOf(keyW, geometry.faceWidth)
                        val coverH = maxOf(keyH, geometry.faceHeight)
                        val centerX = keyLeft + keyW / 2f - geometry.left
                        val centerY = geometry.barHeight / 2f - geometry.top
                        val clipLeft = centerX - coverW / 2f
                        val clipRight = centerX + coverW / 2f
                        if (clipRight > 0f && clipLeft < size.width) {
                            val clipTop = centerY - coverH / 2f
                            val radius = minOf(TAB_PILL_CORNER_DP.dp.toPx(), minOf(coverW, coverH) / 2f)
                            pillClipPath.rewind()
                            pillClipPath.addRoundRect(
                                RoundRect(clipLeft, clipTop, clipRight, clipTop + coverH, radius, radius)
                            )
                            clipPath(pillClipPath) { this@drawWithContent.drawContent() }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.onSizeChanged { size ->
                    geometry.faceWidth = size.width.toFloat()
                    geometry.faceHeight = size.height.toFloat()
                }
            ) {
                content(true)
            }
        }
    }
}
