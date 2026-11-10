# Add project specific ProGuard rules here.
# Media3 and Coil ship their own consumer rules.

# Batch 628 — ShizukuBridge memanggil Shizuku.newProcess lewat REFLEKSI (private sejak API 13.1.0):
# jangan dibuang/diubah namanya oleh R8 (rilis), kalau tidak akses shell Shizuku gagal senyap.
-keepclassmembers class rikka.shizuku.Shizuku {
    *** newProcess(...);
}
