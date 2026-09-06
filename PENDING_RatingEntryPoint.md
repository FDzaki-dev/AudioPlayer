# PENDING: Titik input rating setelah StarRatingRow dihapus (Batch 357)

## Konteks
Batch 357 menghapus `StarRatingRow` (5 bintang) dari `NowPlayingScreen.kt` atas permintaan user
(modernisasi UI, referensi Spotify/Apple Music). Audit sebelum eksekusi menemukan fakta yang
tidak diketahui sebelumnya: `StarRatingRow` adalah **satu-satunya jalur TULIS** ke `RatingStore`
di seluruh codebase.

Titik lain yang menyentuh rating, dicek satu per satu:
- `LibraryScreen.kt` — cuma `ratingOf = { id -> ratingStore.getRating(id) }`, dipakai untuk
  **membaca**/filter, bukan menulis.
- `SmartPlaylistEngine.kt` / `SmartPlaylist.kt` / `SmartPlaylistScreen.kt` — rating dipakai
  sebagai **kriteria filter** ("rating minimum"), bukan tempat mengatur rating lagu.
- `SongInfoEditSheet.kt` — TIDAK ada field rating (dicek, nihil).
- `BackupRestoreSheet.kt` / `BackupManager.kt` / `VaultStore.kt` — cuma ikut serta saat
  backup/restore/vault memindahkan data (termasuk rating) apa adanya, bukan UI input.

## Dampak setelah Batch 357
- Rating yang SUDAH ADA di `RatingStore` (dari sebelum batch ini) tetap terbaca & tetap bisa
  dipakai filter Smart Playlist — **tidak hilang, tidak rusak**.
- Tidak ada lagi cara bagi user memberi/mengubah rating lagu manapun dari UI apa pun di app ini.
- Fitur Smart Playlist "rating minimum" secara efektif jadi **write-once-dari-masa-lalu**: kalau
  user belum sempat rating lagu tertentu sebelum batch ini, lagu itu tidak akan pernah bisa masuk
  kriteria filter tersebut lagi kecuali ada jalur input baru.

## Kenapa tidak langsung diperbaiki di batch yang sama
Micro-Batch (maks 3 file kode/task) + Zero-Refactor: menambah UI set-rating baru (mis. di
`LibraryScreen.kt` context menu per-lagu, atau field baru di `SongInfoEditSheet.kt`) adalah
**scope terpisah** dari permintaan asli batch ini (murni kosmetik Now Playing), butuh keputusan
desain sendiri (di mana, bentuknya apa) — bukan sesuatu yang aman diasumsikan sepihak.

## Opsi untuk sesi berikutnya (butuh keputusan/konfirmasi user, JANGAN dieksekusi tanpa itu)
1. **Biarkan begitu saja** — kalau user memang sudah tidak peduli fitur rating (mis. jarang
   dipakai), tidak perlu jalur input baru. Konsekuensi: kriteria "rating minimum" di Smart
   Playlist jadi fitur beku, mungkin layak diperjelas di README/UI sebagai legacy.
2. **Pindahkan ke `LibraryScreen.kt`** — tambah aksi "Beri Rating" di context menu/long-press
   per lagu (pola sudah ada di file itu untuk aksi per-lagu lain, cek dulu sebelum eksekusi).
3. **Pindahkan ke `SongInfoEditSheet.kt`** — tambah field 5-bintang di sheet edit info lagu yang
   sudah ada, dekat metadata lain.
4. **Kembalikan ke Now Playing tapi lebih ringkas** — mis. 1 ikon bintang tunggal yang saat
   ditekan membuka dialog/dropdown 5 pilihan, alih-alih 5 ikon permanen di layar.

`currentRating`/`onSetRating` di signature `NowPlayingScreen()` (parameter dari
`MainActivity.kt`) sengaja TIDAK dihapus di Batch 357 — masih tersedia dipakai lagi kalau Opsi 4
yang dipilih, tanpa perlu sentuh `MainActivity.kt` (protected) sama sekali.
