# Add project specific ProGuard rules here.
# Media3 and Coil ship their own consumer rules.

# Batch 628 — ShizukuBridge memanggil Shizuku.newProcess lewat REFLEKSI (private sejak API 13.1.0):
# jangan dibuang/diubah namanya oleh R8 (rilis), kalau tidak akses shell Shizuku gagal senyap.
-keepclassmembers class rikka.shizuku.Shizuku {
    *** newProcess(...);
}

# Batch 687 — R8 TETAP menyusutkan + mengoptimalkan kode (isMinifyEnabled/isShrinkResources TIDAK diubah); yang dimatikan HANYA
# PENGACAKAN NAMA. Alasan (bukti log v686): tumpukan di log diagnostik berupa nama pendek (`b1.w1.i`, `c2.a.b`, `n2.k.e`, `cc.y.j`)
# sehingga probe StartupStall + 5 FATAL lama tak bisa dibaca tanpa mapping.txt (tak pernah ikut dikirim ke sesi kerja). Konstitusi
# mewajibkan logger menangkap stacktrace MENTAH yang terbaca; aplikasi ini pemakaian pribadi (tak ada kebutuhan menyembunyikan nama).
# SourceFile + LineNumberTable dipertahankan supaya frame berbentuk `Kelas.metode(Berkas.kt:baris)`. Batalkan = hapus 2 baris ini.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
