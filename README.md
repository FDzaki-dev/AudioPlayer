# Audio Player

Audio player Android — Kotlin + Jetpack Compose + Media3 ExoPlayer.

## Fitur v1
- Scan otomatis semua file audio di perangkat via MediaStore (mendukung codec mainstream: MP3, AAC/M4A, FLAC, WAV, OGG/Vorbis, OPUS, AMR — apa pun yang bisa diindeks sistem)
- Tab **Lagu** (semua lagu, urut judul) dan **Folder** (dikelompokkan per folder, folder = playlist)
- Playback background via foreground service (Media3 `MediaSessionService`) + kontrol di notification/lock screen
- Now Playing: album art, seek bar, shuffle, repeat (off/all/one), play/pause/next/prev
- Mini player persisten di bagian bawah layar
- Dark mode default

## Build
Build otomatis lewat GitHub Actions setiap push ke `main`. Hasil APK release diunggah sebagai artifact bernama `AudioPlayer-release`.

## Rencana v2 (belum dibuat)
- Equalizer
- Sleep timer
- Pencarian (search)
- Widget home screen
- Lyrics
