# PENDING: pasang `rememberIosFlingBehavior()` di sisa layar berscroll

Dibuka Batch 364 (lihat `PROJECT_STATE.md`/`CHANGELOG.md` batch itu utk konteks lengkap laporan
user "scrolling masih sat set, minta transisi mulus like iOS"). Rubber-band overscroll-nya
**sudah selesai app-wide** (dipasang sekali di `Theme.kt`, tidak ada task tersisa di sisi itu) —
dokumen ini KHUSUS soal kurva fling (momentum meluncur setelah jari dilepas), yang belum bisa
app-wide karena Compose belum expose CompositionLocal utk `flingBehavior` default (beda dari
overscroll yang sudah dapat `LocalOverscrollFactory`).

## Pola yang sudah dipasang di `LibraryScreen.kt` (Batch 364) — tinggal REUSE persis

1. Tambah import: `import com.rudi.audioplayer.ui.theme.rememberIosFlingBehavior`
2. Di pemanggilan `LazyColumn(...)`/`LazyRow(...)`/`LazyVerticalGrid(...)` yang bersangkutan,
   tambah parameter: `flingBehavior = rememberIosFlingBehavior()`
3. Kalau scrollable itu sudah punya `state = ...` eksplisit, `flingBehavior` tetap parameter
   terpisah — urutan parameter tidak masalah (named parameter), tinggal disisipkan di mana saja
   yang rapi dibaca.
4. 0 perlu sentuh apa pun di `IosScrollPhysics.kt` sendiri kecuali user eksplisit minta tuning
   rasa (`frictionMultiplier`, default `0.75f`, ada di file itu).

## Sisa 8 file (urutan bebas — silakan comot berapa pun sesuai batas Micro-Batch, maks 3 file kode/task)

- [x] `ui/HomeScreen.kt` — 2 scrollable **(selesai Batch 367)**
- [x] `ui/PlaylistScreen.kt` — 1 scrollable **(selesai Batch 367)**
- [x] `ui/QueueSheet.kt` — 1 scrollable **(selesai Batch 367)**
- [x] `ui/SmartPlaylistScreen.kt` — 2 scrollable **(selesai Batch 377)**
- [x] `ui/SettingsScreen.kt` — 1 scrollable **(selesai Batch 377)**
- [x] `ui/StatsDashboardScreen.kt` — 1 scrollable **(selesai Batch 377)**
- [ ] `ui/VaultSheet.kt` — 1 scrollable
- [ ] `ui/SongPickerSheet.kt` — 1 scrollable
- [ ] `ui/FolderManagerSheet.kt` — 1 scrollable
- [ ] `ui/DuplicateFinderSheet.kt` — 1 scrollable
- [ ] `ui/ABRepeatBookmarkSheet.kt` — 1 scrollable
- [ ] `ui/EqualizerSheet.kt` — 2 scrollable
- [ ] `ui/LyricsSheet.kt` — 1 scrollable
- [ ] `ui/lyrics/LyricsView.kt` — 2 scrollable (lirik auto-scroll — cek dulu apakah ini pakai
      `animateScrollToItem` terprogram; kalau iya, kurva fling manual mungkin tidak relevan di
      situ, cukup pasang utk drag manual user)

**Catatan `NowPlayingScreen.kt`**: TIDAK ada di daftar ini — file itu sudah dicek Batch 364, 0
`LazyColumn`/`LazyRow`/Pager instantiation nyata (cuma disebut di komentar), jadi tidak ada yang
perlu ditambah `flingBehavior` di situ untuk sekarang.

## Hapus dokumen ini kalau

Semua 14 checkbox di atas sudah tercentang (atau user eksplisit bilang cukup/tidak perlu semua
layar dikerjakan) — dicatat sebagai selesai di `PROJECT_STATE.md`/`CHANGELOG.md` batch penutupnya.
