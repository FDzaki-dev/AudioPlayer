# MANUAL_QA_CHECKLIST.md

Dibuat di Batch 103 sebagai pasangan `app/src/androidTest/` — bukan pengganti, tapi bagian yang
jujur tidak bisa (atau belum sepadan usahanya utk) diotomasi murni lewat kode di sesi kerja ini.
`PlaybackTransportTest.kt` menutup play/pause/seek/skip/next-previous/repeat-off-all-one/shuffle
lewat `MediaController` sungguhan ke `PlaybackService` sungguhan di emulator CI. Daftar di bawah
ini butuh device fisik dan/atau interaksi sistem (Bluetooth, lock screen, notifikasi, tombol
headset) yang emulator CI standar tidak simulasikan dengan realistis — mencoba "mengotomasi"-nya
lewat mock akan cuma memverifikasi mock-nya sendiri, bukan perilaku sungguhan.

Centang manual tiap kali diuji di device fisik; kalau ada yang gagal, catat model device + versi
Android + langkah reproduksi di `CHANGELOG.md` sebagai bug batch berikutnya.

## Audio focus
- [ ] Playback pause otomatis saat panggilan telepon masuk, resume/tetap pause setelah panggilan
      selesai (sesuai perilaku audio focus transient yang diharapkan).
- [ ] Playback duck (volume turun sementara, bukan pause) saat notifikasi suara singkat dari app
      lain (mis. pesan masuk) diputar bersamaan.
- [ ] Playback pause saat app lain (mis. video call, app musik lain) minta audio focus eksklusif.

## Bluetooth / media output
- [ ] Playback pindah otomatis ke output Bluetooth saat headset/speaker Bluetooth disambungkan
      selagi musik main.
- [ ] Playback pause otomatis saat perangkat Bluetooth aktif diputus (bukan lanjut lewat speaker
      HP tiba-tiba).
- [ ] Tombol play/pause/next/previous fisik di headset/speaker Bluetooth berfungsi.
- [ ] Perpindahan ke output audio lain (mis. USB-C DAC, Chromecast bila didukung) tidak
      menyebabkan crash atau audio ganda.

## Lock-screen & notification controls
- [ ] Media control muncul di lock screen begitu playback mulai, hilang wajar saat berhenti total
      (bukan nyangkut).
- [ ] Play/pause/next/previous dari lock screen berfungsi dan sinkron balik ke UI in-app.
- [ ] Artwork + judul/artis di lock screen & notifikasi sinkron dgn lagu yang benar-benar main,
      termasuk tepat setelah skip cepat berturut-turut.
- [ ] Swipe/dismiss notifikasi media saat playback aktif berperilaku wajar (tidak menghentikan
      Service scr tidak sengaja jika masih ingin lanjut background).

## Headset kabel
- [ ] Tombol single-click play/pause di headset kabel berfungsi.
- [ ] Playback pause otomatis saat headset kabel dicabut selagi musik main.

## Process death & recreation
- [ ] Force-stop app dari Pengaturan Android saat musik sedang main, buka lagi — queue/posisi/
      repeat/shuffle terrestore sesuai `PlaybackStateStore` (roadmap durable state, gap list
      item #6 — batch terpisah).
- [ ] "Kill" app dari recent-apps switcher selagi playback background aktif — audio TIDAK ikut
      berhenti (foreground service tetap hidup), notifikasi tetap terkendali.
- [ ] Low-memory system kill (disimulasikan lewat `adb shell am kill <package>` selagi app di
      background, BUKAN sedang main foreground) diikuti buka app lagi — tidak crash, state
      pulih wajar.

## Background playback
- [ ] Musik tetap main saat layar dikunci dalam waktu lama (mis. >30 menit, cek battery
      optimization tidak membunuh Service di device yg agresif spt sebagian merk Android).
- [ ] Musik tetap main saat berpindah ke app lain / multitasking split-screen.

## Android 15/16-spesifik
- [ ] **Belum bisa diuji berarti** — `compileSdk`/`targetSdk` proyek masih 34 (`app/build.gradle.
      kts`), belum menyasar API level 15/16 secara eksplisit. Menaikkan `targetSdk` adalah
      perubahan protected-asset berisiko tinggi tersendiri (predictive back, foreground service
      type enforcement, notifikasi, dll bisa berubah perilaku) — sengaja BUKAN bagian batch ini,
      butuh sesi/batch khusus dgn fokus audit kompatibilitas platform (mirip pola Batch 99 utk
      Android 14 ke bawah), bukan digabung diam-diam ke batch testing ini.
