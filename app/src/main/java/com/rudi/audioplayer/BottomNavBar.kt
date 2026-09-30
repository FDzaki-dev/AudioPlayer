package com.rudi.audioplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.ui.bouncyPress
import com.rudi.audioplayer.ui.theme.isSkeuTheme

// Batch 520 — Wave 2 T10 (MOVE-ONLY, R3): MagnifyingTabLabel/GlassTabIcon/NoRippleIndication/CustomNavBarTabItem dipindah
// dari MainActivity.kt (paket sama `com.rudi.audioplayer`, jadi 0 import baru di MainActivity). Badan + komentar identik
// karakter-per-karakter; satu-satunya beda = `private` -> `internal` pada 2 fungsi yang dipanggil dari AppNavHost
// (GlassTabIcon, CustomNavBarTabItem). MagnifyingTabLabel (dipakai GlassTabIcon di file ini) dan NoRippleIndication
// (0 pemakai di seluruh source sejak Batch 449) SENGAJA tetap private. AppNavHost TIDAK dipecah. Lihat CHANGELOG Batch 520.

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
// Batch 447 — user lampirkan video referensi iOS Jam (drag lintas tab Alarm/Jam dunia/Timer/
// Stopwatch): ikon+pill SUDAH ikut lerp warna kontinu 1:1 sinkron jari (Batch 440/446), TAPI
// label teks di bawahnya TIDAK — root cause: `Text(...)` di bawah 0 pernah di-set `color`
// eksplisit sejak fungsi ini dibuat (Batch 437), jadi warnanya 100% inherit `LocalContentColor`
// bawaan `NavigationBarItem` M3, yang HANYA bereaksi ke boolean `selected` (snap begitu
// navigate() commit index-crossing) — BUKAN ke `focus` kontinu yang sudah dipakai utk
// scale/opacity DI FUNGSI INI JUGA. Efeknya: selama drag pelan/parsial (belum commit index),
// ikon sudah keburu blend warna (mengikuti jari), tapi teks di bawahnya masih warna lama 100%
// sampai commit — "1 aksen bergerak bersama" (tujuan eksplisit Batch 440) putus di teks,
// persis beda dari referensi video (ikon+label iOS Jam berubah warna BERSAMAAN, bukan teks
// menyusul lompat). Fix: param baru `color: Color?` (default null = 0 override, IDENTIK
// perilaku lama persis, 0 regresi ke satu-satunya titik pemakaian lain manapun kalau ada) —
// dipetakan ke `Text(color = ...)` di bawah. Nilai dihitung di 1 titik pemanggil
// (`GlassTabIcon`, SUDAH punya `tint`+`unselectedIconColor`+`glassAlpha` yg sama persis
// dipakai ikon) via `lerp` yang SAMA PERSIS, 0 hitungan/token warna baru — Skeu DIKECUALIKAN
// (tetap kirim null, warna default M3 apa adanya, aturan solid Batch 58/61/79 tidak disentuh).
// (ANTI-STALE Batch 449: "tetap kirim null" akurat sampai Batch 448 — `NavigationBarItem` yang
// menyuplai LocalContentColor tsb DIHAPUS Batch 449, Skeu kini kirim token eksplisit, bukan null
// lagi. Lihat blok `labelColor`/`skeuIconColor` di `GlassTabIcon` utk kondisi terkini.)
@Composable
private fun MagnifyingTabLabel(text: String, focus: Float, color: Color? = null) {
    val clampedFocus = focus.coerceIn(0f, 1f)
    val baseStyle = MaterialTheme.typography.labelMedium
    // Batch 445 — user eksplisit: drag real-time tab-bar "kurang smooth". Root cause KEDUA
    // (selain fix `glassAlpha` di `GlassTabIcon`): `style = baseStyle.copy(fontSize = ...)` di
    // bawah ini mengubah fontSize SUNGGUHAN tiap frame drag (bukan cuma transform) — Text harus
    // di-remeasure+relayout ULANG tiap kali `focus` berubah (tiap pointer-move event selama
    // drag), dobel dgn scale visual `graphicsLayer` di bawah yang SUDAH cukup utk efek membesar
    // (2 mekanisme scale independen bertumpuk = magnitude gabungan lebih besar dari maksud awal
    // DAN beban layout-pass berulang yang berkontribusi ke drag terasa tersendat). Fix: fontSize
    // asli (`baseStyle`, 0 di-copy) dipertahankan APA ADANYA — 0 remeasure lagi selama drag,
    // efek "membesar" SEPENUHNYA lewat `graphicsLayer` scale (draw-phase murni, pola sama
    // persis "Px-sinkron dibaca graphicsLayer" yang sudah dipakai `tabDragOffsetPx`/
    // `tabBarOverscrollPx`, Batch 433/434/444). Faktor scale dinaikkan dari 0.08f ke 0.23f
    // (≈ gabungan magnitude lama 1.14×1.08=1.231) supaya besar visual akhir label saat fokus
    // penuh TETAP sama seperti sebelumnya — 0 perubahan tampilan yang diminta, murni pindah
    // mekanisme jadi lebih ringan.
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = baseStyle,
        // Batch 447 — lihat komentar lengkap di atas fungsi ini. `Color.Unspecified` (default
        // param Compose `Text`, bukan literal baru dari batch ini) = perilaku identik dgn 0
        // parameter `color` sama sekali — inherit LocalContentColor bawaan M3 apa adanya.
        color = color ?: Color.Unspecified,
        modifier = Modifier
            .graphicsLayer {
                val scale = 1f + clampedFocus * 0.23f
                scaleX = scale
                scaleY = scale
                alpha = 0.68f + 0.32f * clampedFocus
            }
            // Batch 442 — `.blur()` efek "kaca pembesar" (Batch 437) DICABUT. Root cause
            // terpisah dari fix style di atas: radius idle (focus=0, tab TIDAK sedang digeser)
            // = (1-0)*1.3 = 1.3dp KONSTAN di 2 dari 3 label SETIAP SAAT (bukan cuma sesaat
            // selama drag) — laporan+screenshot user konfirmasi efeknya cuma bikin
            // "Perpustakaan"/"Pengaturan" terlihat buram permanen, 0 manfaat visual nyata
            // (kaskade DESCENDING TRUTH: laporan eksplisit user > spec lama Batch 437).
            // `scaleX`/`scaleY`/`alpha` (graphicsLayer di atas) TETAP jalan sbg sinyal fokus
            // kontinu selama drag — cuma komponen blur yang dicabut, 0 elemen lain disentuh.
    )
}

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
@Composable
internal fun GlassTabIcon(
    icon: ImageVector,
    label: String,
    focus: Float,
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    isDragging: Boolean
) {
    val isSkeu = isSkeuTheme()
    // Cross-fade kontinu (bukan snap ON/OFF) — pill kaca menyala/meredup halus mengikuti
    // transisi selected, pola animasi sama (tween) yang sudah dipakai transisi NavHost (Batch
    // 330, 200/150ms) supaya "rasa" transisi tetap konsisten satu app.
    // Batch 440 — request eksplisit user: adaptasi behavior dari panduan
    // `drag_drop_glass_ios_kotlin.md` (dilampirkan ulang) — kapsul & warna ikon di referensi
    // bertransisi MENGIKUTI PERSENTASE GESER JARI SECARA LANGSUNG (`pageOffsetFraction`
    // HorizontalPager), bukan cuma snap ikut boolean `selected` setelah tab commit. Arsitektur
    // riil app ini TETAP permanent NavHost routes (bukan HorizontalPager — lihat rasionalisasi
    // Batch 438 di atas, reorder/pager sengaja tidak dipakai literal), tapi app ini SUDAH punya
    // padanan persis `pageOffsetFraction` guide: `focus` (param di atas, dihitung live tiap
    // frame selama drag oleh `tabMagnifyFocus`, Batch 435/437 — 1f di tab aktif idle, turun ke
    // 0f digeser menjauh, tab tetangga naik 0f→1f digeser mendekat). Target
    // `animateFloatAsState` diganti dari `if (selected) 1f else 0f` (statis, cuma bereaksi
    // setelah commit) jadi `focus` langsung — idle-nya SAMA PERSIS 1f/0f seperti sebelumnya (0
    // regresi tap, tween 220ms yang sama tetap jalan sbg smoothing), bedanya sekarang capsule
    // ini juga ikut bereaksi kontinu selama jari masih menggeser, persis seperti referensi.
    // Batch 445 — feedback lanjutan user PASCA Batch 444 di device asli, 2 poin: (1) "drag jari
    // real-time belum sepenuhnya smooth", (2) "floating effect HANYA saat drag, warna berubah
    // seketika real-time (bukan cuma pindah warna instant lintas tab)". Root cause TUNGGAL utk
    // keduanya: `animateFloatAsState(targetValue = focus, tween(220))` di atas adalah lapis
    // smoothing KEDUA di atas `focus` yang SUDAH kontinu real-time (fungsi tenda
    // `tabBarDragFocus`, Batch 444) — targetnya sendiri bergerak tiap event pointer-move selama
    // drag, jadi tween 220ms itu terus "mengejar" target yang TERUS PINDAH (bukan mengejar 1
    // target diam spt transisi tap biasa), hasil yang dirender SELALU tertinggal dari posisi
    // jari asli: lag itu yang terasa "kurang smooth" (poin 1), dan karena tertinggal, warna
    // ikon/pill (baca `glassAlpha` di bawah) terlihat menyusul-lompat bukan berubah seketika
    // sinkron dgn jari (poin 2).
    // Fix: `animateFloatAsState` diganti `Animatable` manual dikontrol lewat `isDragging` (param
    // baru, dihitung sekali di pemanggil dari `tabBarDragIndexPx`/`tabDragOffsetPx` — lihat titik
    // pemakaian di `AppNavHost`) — SELAMA drag aktif: `snapTo(focus)` tiap kali `focus` berubah
    // (0 animasi, 1:1 sinkron per frame dgn posisi jari mentah, pola sama seperti
    // `tabDragOffsetPx`/`tabBarOverscrollPx` yang baca nilai mentah langsung tanpa animasi utk
    // real-time tracking, Batch 433/434/444). Begitu drag lepas (`isDragging` → false):
    // `animateTo(focus, tween(220))` dari titik SINKRON terakhir itu — nyambung mulus ke posisi
    // commit final (0f/1f) TANPA lompatan mundur. Tap biasa (0 drag aktif sama sekali) tetap
    // dapat cross-fade tween(220) yang SAMA PERSIS seperti sebelumnya (Batch 440) — 0 regresi.
    val glassAlphaAnim = remember { Animatable(focus) }
    LaunchedEffect(focus, isDragging) {
        if (isDragging) {
            glassAlphaAnim.snapTo(focus)
        } else {
            glassAlphaAnim.animateTo(focus, tween(220))
        }
    }
    val glassAlpha = glassAlphaAnim.value
    val pillShape = RoundedCornerShape(16.dp)
    val tint = MaterialTheme.colorScheme.primary
    // Batch 448 — ROMBAK TOTAL mekanisme drag bottom nav (instruksi eksplisit user + video
    // referensi iOS Jam asli). Root cause bug "gak bagus sama sekali" (2 kotak pill
    // tumpang-tindih dgn seam/celah kelihatan pas drag, dikonfirmasi lewat frame-by-frame video
    // user): sejak Batch 446, ADA 2 SISTEM GAMBAR PILL BERJALAN BERSAMAAN — (1) pill per-tab DI
    // SINI (dibatasi lebar kolomnya sendiri) DAN (2) 1 pill "bridge" tambahan yang digambar
    // `drawWithContent` di `NavigationBar` (AppNavHost) BEBAS lintas kolom. Keduanya nyala
    // BERSAMAAN selama drag (bridge utk lintas-kolom, punya function ini utk idle/tap) — 2 kotak
    // rounded-rect beda ukuran/beda sumber saling tumpuk = seam persis yg kelihatan di video.
    // Fix: pill glass (non-Skeu) DIHAPUS TOTAL dari sini — SATU-SATUNYA penggambar pill utk
    // identitas kaca kini pill unified di `NavigationBar` (AppNavHost, selalu aktif di SEMUA
    // state: idle/tap/drag/nudge, bukan cuma saat drag spt bridge lama) yang bebas meluncur
    // mulus lintas kolom tanpa batas/seam krn 1 kanvas bersama. Icon+label lerp warna (baris di
    // bawah fungsi ini) TIDAK disentuh — itu SUDAH benar 1:1 sesuai video (state Batch 447),
    // murni pill BACKGROUND yang direstrukturisasi. Skeu TIDAK disentuh sama sekali (aturan lama
    // "solid, bukan kaca", Batch 58/61/79) — tetap pill diskrit sendiri di bawah, 0 regresi.
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .then(
                if (isSkeu) {
                    // Skeu: 0 kaca, replikasi manual solid pill M3 default (indicatorColor
                    // dimatikan/transparent di titik pemakaian supaya 1 composable ini jadi
                    // SATU-SATUNYA penggambar indicator, konsisten lintas identitas).
                    if (selected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer, pillShape)
                    else Modifier
                } else {
                    // Batch 448 — 0 background/border digambar di sini lagi (lihat komentar
                    // panjang di atas fungsi ini). Pill glass tunggal kini digambar 1x di
                    // `NavigationBar` (AppNavHost), bebas lintas kolom, 0 duplikasi.
                    Modifier
                }
            )
            .bouncyPress(interactionSource, pressedScale = 0.9f)
            // Batch 452 — user: label nav bawah masih kepotong ellipsis ("Perpustakaan"/
            // "Pengaturan") di kondisi normal (bukan cuma font-scale aksesibilitas besar spt
            // catatan lama Batch 442). Root cause: 12.dp padding kiri+kanan di sini memakan 24.dp
            // dari lebar kolom (~1/3 lebar bar) SEBELUM Text diukur — margin tipis tapi cukup utk
            // memicu ellipsis pada label 12 huruf di layar sempit. Touch target 0 kena dampak sama
            // sekali: area sentuh tab = `Box.weight(1f, fill=true)` di `CustomNavBarTabItem`
            // (pembungkus di LUAR Column ini), BUKAN padding Column ini — padding ini murni jarak
            // visual internal. Fix: 12.dp -> 4.dp (bebaskan 16.dp lebar tambahan utk Text, 0
            // sentuh style/fontSize/theme token). Efek samping disengaja: pill solid Skeu (background
            // di atas, dibungkus padding yg SAMA) ikut sedikit lebih ramping ke arah teks — masih
            // proporsional (aturan solid Batch 58/61/79 tidak disentuh), bukan regresi ukuran
            // kapsul raksasa Batch 450.
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Batch 440 — elemen guide yang BELUM diadaptasi batch-batch sebelumnya: ikon sendiri
        // (bukan cuma pill di belakangnya) ikut `lerp` warna kontinu, teknik PERSIS
        // `androidx.compose.ui.graphics.lerp` di guide. Titik awal `unselectedIconColor` =
        // token M3 resmi (`NavigationBarItemDefaults`, IDENTIK dgn default lama sebelum batch
        // ini — 0 hardcode warna baru, ikut identitas tema aktif apa pun, konsisten dgn aturan
        // Batch 437 §warna). Titik akhir = `tint` (primary) yang sama dgn aksen pill di atas,
        // supaya ikon & pill bergerak sebagai 1 aksen, bukan 2 warna lepas. Skeu DIKECUALIKAN
        // (aturan "solid, bukan kaca" Batch 58/61/79, app-wide) — tetap tint default M3 apa
        // adanya, 0 lerp.
        if (isSkeu) {
            // Batch 449 — dulu implisit lewat `LocalContentColor` bawaan `NavigationBarItem` M3
            // (DIHAPUS batch ini, lihat `CustomNavBarTabItem`) — tanpa titik ini, ikon Skeu akan
            // STATIS 1 warna (kehilangan beda selected/unselected sama sekali, bukan cuma soal
            // gray-flash). Snap biner (0 lerp, aturan solid Batch 58/61/79 tidak disentuh), token
            // M3 resmi PERSIS yang dulu dipakai NavigationBarItem secara default.
            val skeuIconColor = if (selected) NavigationBarItemDefaults.colors().selectedIconColor
                else NavigationBarItemDefaults.colors().unselectedIconColor
            Icon(icon, contentDescription = null, tint = skeuIconColor)
        } else {
            val unselectedIconColor = NavigationBarItemDefaults.colors().unselectedIconColor
            Icon(icon, contentDescription = null, tint = lerp(unselectedIconColor, tint, glassAlpha))
        }
        // Batch 447 — labelColor: PERSIS pola unselectedIconColor/lerp di atas, target token
        // resmi `unselectedTextColor` (bukan `unselectedIconColor` yg dipakai ikon), 0 hardcode
        // baru. (ANTI-STALE Batch 449: dulu null utk Skeu di sini — sejak Batch 449 Skeu kirim
        // token eksplisit, lihat blok `labelColor` di bawah, komentarnya sendiri.)
        // `glassAlpha` (bukan `focus` mentah) dipakai di SINI (parameter ke-2) juga — identik
        // nilai selama drag aktif (glassAlpha snapTo(focus) tiap frame), bedanya HANYA di jendela
        // easing 220ms pasca lepas jari: scale/opacity label kini ikut melunak bareng warna
        // ikon+labelColor baru ini, bukan snap instan sendirian seperti sebelumnya — konsisten
        // dgn tujuan "1 aksen bergerak bersama" (Batch 440), 0 dampak ke tap biasa/idle (identik
        // 0f/1f di kedua kasus).
        // Batch 449 — sama alasan `skeuIconColor` di atas: dulu implisit `LocalContentColor`
        // `NavigationBarItem` (DIHAPUS), kini eksplisit token resmi selected/unselectedTextColor.
        val labelColor = if (isSkeu) {
            if (selected) NavigationBarItemDefaults.colors().selectedTextColor
            else NavigationBarItemDefaults.colors().unselectedTextColor
        } else lerp(NavigationBarItemDefaults.colors().unselectedTextColor, tint, glassAlpha)
        MagnifyingTabLabel(label, glassAlpha, labelColor)
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
@Composable
internal fun RowScope.CustomNavBarTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f, fill = true)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
