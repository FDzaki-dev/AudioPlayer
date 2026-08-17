package com.rudi.audioplayer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.rudi.audioplayer.util.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Gap List #10 — Backup/restore data lokal. Bundel semua SharedPreferences yang berarti dibawa
 * lintas reinstall/device (playlist, favorit, rating, playlist otomatis, riwayat/statistik
 * dengar, bookmark, mode audiobook, dan pengaturan) jadi 1 file JSON portabel, ditulis ke
 * Documents/AudioPlayer/backups lewat MediaStore (API 29+, pola identik [AppLogger] — tidak
 * butuh izin storage tambahan). Restore membaca file lewat Storage Access Framework (user pilih
 * file secara eksplisit — app ini tidak minta izin baca umum ke Documents).
 *
 * SENGAJA DIKECUALIKAN dari whitelist (bukan kelupaan):
 * - `app_lock` (PIN/lockout `AppLockStore`) — data keamanan; memuat ulang PIN dari file backup
 *   yang bisa saja dibagikan/disalin ke device lain adalah risiko, bukan kenyamanan.
 * - `custom_folders` (URI SAF folder tambahan) — `persistedUriPermission` terikat ke
 *   install+device asal; memulihkan string URI mentah tanpa hak izinnya cuma menghasilkan entri
 *   folder mati (lihat penanganan "izin dicabut" Batch 106) — bukan restore yang berarti.
 * - `onboarding_hints` — status UI first-run sekali pakai, tidak ada nilai dibawa lintas restore.
 * - `search_history` — riwayat pencarian, nilainya rendah & berumur pendek, di luar scope demi
 *   menjaga cakupan tetap sempit (bukan bug, keputusan sengaja).
 * - `sleep_timer` — cuma `endAt` epoch-millis dari timer yang sedang berjalan; me-restore ini
 *   di sesi lain nyaris pasti berarti me-restore timer yang sudah lewat.
 */
object BackupManager {
    const val SCHEMA_VERSION = 1

    private val BACKUP_RELATIVE_PATH = "${Environment.DIRECTORY_DOCUMENTS}/AudioPlayer/backups"
    private const val MAX_BACKUPS = 20
    private val fileStampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /** prefsName -> label manusiawi dipakai di ringkasan konfirmasi restore. Urutan di sini juga
     * urutan tampil ringkasan. */
    private val WHITELISTED_PREFS = linkedMapOf(
        "playlists" to "Playlist",
        "smart_playlists" to "Playlist Otomatis",
        "favorites" to "Favorit",
        "ratings" to "Rating bintang",
        "bookmarks" to "Bookmark posisi",
        "audiobook_mode" to "Mode Audiobook",
        "listening_history" to "Riwayat dengar",
        "play_stats" to "Statistik putar",
        "hourly_listen_stats" to "Statistik jam dengar",
        "library_filter" to "Folder/lagu disembunyikan",
        "app_theme" to "Tema",
        "crossfade" to "Pengaturan crossfade",
        "silence_skip_settings" to "Lewati keheningan",
        "shake_settings" to "Kocok untuk lewati",
        "radio_settings" to "Radio otomatis",
        "visualizer_settings" to "Visualizer",
        "floating_bubble_settings" to "Mini player mengambang"
    )

    data class BackupPayload(
        val schemaVersion: Int,
        val timestamp: Long,
        val prefsData: Map<String, Map<String, Any?>>
    ) {
        /** label -> jumlah entri, dipakai UI untuk menampilkan ringkasan sebelum restore
         * benar-benar dieksekusi (guard "jangan overwrite destruktif tanpa validasi"). */
        fun summaryCounts(): Map<String, Int> =
            WHITELISTED_PREFS.mapNotNull { (prefsName, label) ->
                val entries = prefsData[prefsName] ?: return@mapNotNull null
                if (entries.isEmpty()) return@mapNotNull null
                label to entries.size
            }.toMap()
    }

    /** Serialize seluruh prefs whitelist ke 1 file JSON di Documents/AudioPlayer/backups.
     * Return nama file kalau sukses, null kalau gagal/di bawah API 29. */
    fun exportToDocuments(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val app = context.applicationContext
            val root = JSONObject()
            root.put("schemaVersion", SCHEMA_VERSION)
            root.put("timestamp", System.currentTimeMillis())
            root.put("app", "AudioPlayer")

            val prefsRoot = JSONObject()
            for (prefsName in WHITELISTED_PREFS.keys) {
                val prefs = app.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                val all = prefs.all
                if (all.isEmpty()) continue
                prefsRoot.put(prefsName, serializePrefs(all))
            }
            root.put("prefs", prefsRoot)

            val fileName = "backup_${fileStampFormat.format(Date())}_${UUID.randomUUID()}.json"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, BACKUP_RELATIVE_PATH)
            }
            val resolver = app.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                ?: return null
            resolver.openOutputStream(uri)?.use { it.write(root.toString().toByteArray()) }
            enforceBackupRetention(app)
            fileName
        }.onFailure {
            AppLogger.e("BackupManager", "Gagal export backup", it)
        }.getOrNull()
    }

    /** Baca + validasi file yang dipilih user lewat SAF. Null kalau file bukan backup yang valid
     * (JSON rusak, tidak ada schemaVersion, atau schema-nya dari masa depan yang belum dikenal
     * versi app ini) — dipisah dari [applyBackup] supaya UI bisa menampilkan ringkasan &
     * meminta konfirmasi eksplisit dulu sebelum data apa pun benar-benar ditimpa. */
    fun readAndValidate(context: Context, uri: Uri): BackupPayload? {
        return runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
                ?: return null
            val root = JSONObject(text)
            val schemaVersion = root.optInt("schemaVersion", -1)
            // Backup dari versi app yang lebih baru (schema belum dikenal) ditolak daripada
            // ditebak — lebih aman gagal jelas daripada salah mengartikan struktur baru.
            if (schemaVersion !in 1..SCHEMA_VERSION) return null
            val timestamp = root.optLong("timestamp", 0L)
            val prefsRoot = root.optJSONObject("prefs") ?: return null

            val prefsData = mutableMapOf<String, Map<String, Any?>>()
            for (prefsName in WHITELISTED_PREFS.keys) {
                val obj = prefsRoot.optJSONObject(prefsName) ?: continue
                prefsData[prefsName] = deserializePrefs(obj)
            }
            if (prefsData.isEmpty()) return null
            BackupPayload(schemaVersion, timestamp, prefsData)
        }.onFailure {
            AppLogger.e("BackupManager", "Gagal parse file backup", it)
        }.getOrNull()
    }

    /** Timpa prefs whitelist dengan isi [payload]. Prefs whitelist yang TIDAK ada di file backup
     * (mis. backup lama dari sebelum sebuah fitur ada) sengaja TIDAK disentuh/dikosongkan —
     * restore itu "isi apa yang ada di file", bukan "hapus semua yang tidak ada di file". Setiap
     * prefs yang memang ada di file di-replace penuh (clear lalu isi), bukan di-merge, supaya
     * hasil restore deterministik & sama persis dengan isi file. */
    @Suppress("UNCHECKED_CAST")
    fun applyBackup(context: Context, payload: BackupPayload) {
        val app = context.applicationContext
        for ((prefsName, entries) in payload.prefsData) {
            if (prefsName !in WHITELISTED_PREFS) continue // jaga-jaga file backup dimodifikasi manual
            val prefs = app.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit().clear()
            for ((key, value) in entries) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value as Set<String>)
                    else -> Unit // tipe tak dikenal (harusnya tidak pernah terjadi) — skip diam, jangan crash restore
                }
            }
            editor.apply()
        }
    }

    // Setiap value SharedPreferences dibungkus {"type": ..., "value": ...} — JSON tidak
    // membedakan Int/Long/Float secara native, jadi tag tipe eksplisit wajib supaya round-trip
    // (export lalu import) tidak diam-diam mengubah Int jadi Long atau sebaliknya.
    private fun serializePrefs(all: Map<String, *>): JSONObject {
        val obj = JSONObject()
        for ((key, value) in all) {
            val entry = JSONObject()
            when (value) {
                is String -> { entry.put("type", "string"); entry.put("value", value) }
                is Int -> { entry.put("type", "int"); entry.put("value", value) }
                is Long -> { entry.put("type", "long"); entry.put("value", value) }
                is Float -> { entry.put("type", "float"); entry.put("value", value.toDouble()) }
                is Boolean -> { entry.put("type", "bool"); entry.put("value", value) }
                is Set<*> -> {
                    entry.put("type", "set")
                    val arr = JSONArray()
                    value.forEach { arr.put(it.toString()) }
                    entry.put("value", arr)
                }
                else -> continue // tipe tak dikenal — skip, tidak pernah terjadi lewat SharedPreferences resmi
            }
            obj.put(key, entry)
        }
        return obj
    }

    private fun deserializePrefs(obj: JSONObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for (key in obj.keys()) {
            val entry = obj.optJSONObject(key) ?: continue
            when (entry.optString("type")) {
                "string" -> result[key] = entry.optString("value")
                "int" -> result[key] = entry.optInt("value")
                "long" -> result[key] = entry.optLong("value")
                "float" -> result[key] = entry.optDouble("value").toFloat()
                "bool" -> result[key] = entry.optBoolean("value")
                "set" -> {
                    val arr = entry.optJSONArray("value") ?: JSONArray()
                    result[key] = (0 until arr.length()).map { arr.getString(it) }.toSet()
                }
                else -> Unit // entri rusak/tipe tak dikenal — dilewati, tidak menggagalkan seluruh restore
            }
        }
        return result
    }

    /** FIFO sama seperti retensi log AppLogger — export manual berulang tidak boleh menumpuk
     * file tanpa batas di Documents user. */
    private fun enforceBackupRetention(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$BACKUP_RELATIVE_PATH/", "backup_%.json")
        val ids = mutableListOf<Long>()
        resolver.query(collection, projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) ids.add(cursor.getLong(idCol))
        }
        if (ids.size <= MAX_BACKUPS) return
        for (id in ids.drop(MAX_BACKUPS)) {
            runCatching { resolver.delete(ContentUris.withAppendedId(collection, id), null, null) }
        }
    }
}
