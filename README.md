O
X
- **Equalizer**: `EqualizerController.kt` kini tersambung penuh — sheet baru di Now Playing dengan toggle aktif/nonaktif, **preset kuat buatan sendiri** (Flat/Bass+/Treble+/Vokal+, sengaja dibuat dramatis karena preset bawaan Android biasanya sangat halus di banyak perangkat), preset bawaan perangkat, dan slider per band frekuensi. Geser slider atau pilih preset otomatis mengaktifkan efeknya. Tersimpan otomatis dan diterapkan ulang tiap sesi
- **Fade Transisi (crossfade sederhana)**: volume melandai halus ±3 detik di pergantian lagu, bisa diatur nyala/mati lewat dialog "Pengaturan Putar" (ikon kecepatan). Aktif secara default
- **APK release ditandatangani konsisten**: CI kini bisa memakai keystore release asli (lewat secret GitHub) untuk build release, bukan debug key — install APK baru tidak perlu uninstall dulu. Otomatis jatuh ke debug key kalau secret belum diisi, jadi tidak pernah gagal build

## Belum selesai / dalam pengerjaan
- Gapless playback murni (tanpa jeda encoder/decoder) belum ada — yang ada sekarang crossfade berbasis fade volume, bukan penyambungan buffer audio
- Shared-element transition sungguhan (mini player → Now Playing sebagai satu elemen visual) belum ada — versi sekarang pakai animasi scale-in sebagai pendekatan yang lebih aman (lihat catatan di riwayat commit)

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai artifact bernama `AudioPlayer-release`. Kalau secret `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, dan `SIGNING_KEY_PASSWORD` sudah diisi di pengaturan repo, APK ditandatangani pakai keystore release asli — kalau salah satu kosong, otomatis jatuh ke debug key tanpa bikin build gagal.

## Rencana v2 (belum dibuat)
- Gapless playback murni (penyambungan buffer audio tanpa jeda)
- Shared-element transition mini player ↔ Now Playing (butuh bump versi Compose)
- Widget home screen
- Lirik otomatis (cari/unduh dari internet — versi sekarang murni input manual)
