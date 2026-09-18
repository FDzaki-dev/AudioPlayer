package com.rudi.audioplayer.data

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Batch 488 [urgent user report: "app tidak load berulang kali setiap dibuka kembali pasca
 * app di kill!!"]. Root cause CONFIRMED via pembacaan kode (bukan tebakan) — lihat KDoc
 * `PlayerViewModel.ensureLibraryLoaded()`/`refreshLibrary()`: `libraryLoadedOnce` di sana murni
 * in-memory `var`, jadi SETIAP proses baru (app dibuka lagi pasca kill) selalu memicu full
 * MediaStore+SAF scan dari nol, `_libraryLoading=true` (shimmer skeleton) menutupi seluruh
 * list SETIAP kali walau library 0 berubah sejak sesi terakhir. Sudah didokumentasikan sendiri
 * Batch 386/436 sbg "jalur paling panas cold-start" — Batch 436 waktu itu MENYIMPULKAN ini
 * "expected behavior" (bukan bug), TIDAK di-fix. Batch ini REVISI kesimpulan itu jadi di-fix.
 *
 * Store ini TIDAK MENGGANTIKAN scan asli (`refreshLibrary()` tetap jalan SETIAP cold start
 * persis seperti sebelumnya, menangkap lagu baru/dihapus/berubah) — cuma menyimpan HASIL scan
 * terakhir supaya proses BERIKUTNYA bisa menampilkannya seketika sambil scan asli jalan senyap
 * di background (stale-while-revalidate, pola umum Spotify/Apple Music dkk).
 *
 * `org.json` dipakai (bagian Android SDK, BUKAN dependency baru) — 0 library serialisasi baru
 * ditambah ke `build.gradle.kts`. Ditulis ke `context.filesDir` (private app storage, 0 izin
 * baru diperlukan, beda dari MediaStore/SAF yang butuh permission). Write lewat temp-file-lalu-
 * rename (bukan tulis langsung) supaya proses di-kill PERSIS di tengah `save()` tidak pernah
 * meninggalkan file cache setengah-tertulis/korup yang bisa bikin `load()` proses BERIKUTNYA
 * gagal parse — baik file lama utuh (rename belum sempat terjadi) maupun file baru utuh
 * (rename sudah selesai), tidak ada kondisi di antaranya yang bisa terbaca.
 */
class LibraryCacheStore(context: Context) {
    private val cacheFile = File(context.filesDir, FILE_NAME)
    private val tmpFile = File(context.filesDir, "$FILE_NAME.tmp")

    /** Dipanggil dari `PlayerViewModel.refreshLibrary()` setelah scan SUKSES — caller
     *  bertanggung jawab menjalankan ini di `Dispatchers.IO` (fungsi ini sendiri murni file I/O
     *  sinkron, sesuai pola `PlaybackStateStore`/Store lain di file ini — 0 dispatcher internal
     *  supaya caller yang kontrol threading-nya, konsisten app-wide). */
    fun save(songs: List<Song>) {
        try {
            val array = JSONArray()
            for (song in songs) {
                val obj = JSONObject()
                obj.put(KEY_ID, song.id)
                obj.put(KEY_TITLE, song.title)
                obj.put(KEY_ARTIST, song.artist)
                obj.put(KEY_ALBUM, song.album)
                obj.put(KEY_ALBUM_ID, song.albumId)
                obj.put(KEY_DURATION, song.duration)
                obj.put(KEY_DATE_ADDED, song.dateAdded)
                obj.put(KEY_URI, song.uri.toString())
                obj.put(KEY_FOLDER_NAME, song.folderName)
                obj.put(KEY_FOLDER_PATH, song.folderPath)
                obj.put(KEY_YEAR, song.year)
                obj.put(KEY_ALBUM_ARTIST, song.albumArtist ?: JSONObject.NULL)
                obj.put(KEY_COMPOSER, song.composer ?: JSONObject.NULL)
                obj.put(KEY_TRACK_NUMBER, song.trackNumber ?: JSONObject.NULL)
                obj.put(KEY_DISC_NUMBER, song.discNumber ?: JSONObject.NULL)
                obj.put(KEY_FILE_SIZE, song.fileSize)
                obj.put(KEY_MIME_TYPE, song.mimeType ?: JSONObject.NULL)
                obj.put(KEY_GENRE, song.genre ?: JSONObject.NULL)
                array.put(obj)
            }
            val root = JSONObject()
            root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            root.put(KEY_SONGS, array)
            val json = root.toString()
            tmpFile.writeText(json)
            if (!tmpFile.renameTo(cacheFile)) {
                // Rename atomik bisa gagal di sebagian filesystem/OEM — fallback tulis langsung
                // daripada diam-diam kehilangan hasil cache batch ini (jarang, tapi 0 exception
                // membisu lebih baik dari kehilangan snapshot tanpa jejak).
                cacheFile.writeText(json)
                tmpFile.delete()
            }
        } catch (e: Exception) {
            // Gagal simpan cache TIDAK BOLEH menjegal/meng-crash scan asli yang sudah sukses
            // memanggil ini — cold start berikutnya cuma jatuh balik ke "0 cache", 0 regresi
            // lain, murni kehilangan win kecepatan untuk 1 sesi berikutnya.
            Log.w(TAG, "Gagal simpan cache library, diabaikan", e)
        }
    }

    /** Dipanggil dari `PlayerViewModel.ensureLibraryLoaded()` — caller bertanggung jawab
     *  menjalankan ini di `Dispatchers.IO` (pola sama `save()` di atas). Null = tidak ada cache
     *  valid (belum pernah tersimpan, ATAU korup/skema lama) — caller WAJIB jatuh balik ke
     *  scan asli seperti sebelum batch ini, TIDAK ada state baru yang perlu ditangani caller. */
    fun load(): List<Song>? {
        return try {
            if (!cacheFile.exists()) return null
            val root = JSONObject(cacheFile.readText())
            if (root.optInt(KEY_SCHEMA_VERSION, -1) != SCHEMA_VERSION) return null
            val array = root.getJSONArray(KEY_SONGS)
            val songs = ArrayList<Song>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                songs.add(
                    Song(
                        id = obj.getLong(KEY_ID),
                        title = obj.getString(KEY_TITLE),
                        artist = obj.getString(KEY_ARTIST),
                        album = obj.getString(KEY_ALBUM),
                        albumId = obj.getLong(KEY_ALBUM_ID),
                        duration = obj.getLong(KEY_DURATION),
                        dateAdded = obj.getLong(KEY_DATE_ADDED),
                        uri = Uri.parse(obj.getString(KEY_URI)),
                        folderName = obj.getString(KEY_FOLDER_NAME),
                        folderPath = obj.getString(KEY_FOLDER_PATH),
                        year = obj.optInt(KEY_YEAR, 0),
                        albumArtist = obj.optStringOrNull(KEY_ALBUM_ARTIST),
                        composer = obj.optStringOrNull(KEY_COMPOSER),
                        trackNumber = obj.optIntOrNull(KEY_TRACK_NUMBER),
                        discNumber = obj.optIntOrNull(KEY_DISC_NUMBER),
                        fileSize = obj.optLong(KEY_FILE_SIZE, 0L),
                        mimeType = obj.optStringOrNull(KEY_MIME_TYPE),
                        genre = obj.optStringOrNull(KEY_GENRE)
                    )
                )
            }
            songs
        } catch (e: Exception) {
            // Cache korup/tidak kompatibel TIDAK BOLEH menjegal cold start — dianggap saja
            // "tidak ada cache", pola fail-safe identik PlaybackStateStore.load() di file
            // sebelah (Batch 108).
            Log.w(TAG, "Gagal load cache library, dianggap tidak ada", e)
            null
        }
    }

    // org.json tidak punya cara baku membedakan "key absen" vs "key ada tapi bernilai JSON
    // null" (skema di atas SENGAJA menulis JSONObject.NULL eksplisit untuk field nullable,
    // bukan meniadakan key-nya) — 2 extension lokal ini baca perbedaan itu dengan benar,
    // daripada `optString` bawaan yang justru mengembalikan literal string "null" untuk kasus
    // ini (footgun umum `org.json`, bukan asumsi/dugaan).
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key)) null else getInt(key)

    companion object {
        private const val TAG = "LibraryCacheStore"
        private const val FILE_NAME = "library_cache.json"
        private const val SCHEMA_VERSION = 1
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_SONGS = "songs"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_ARTIST = "artist"
        private const val KEY_ALBUM = "album"
        private const val KEY_ALBUM_ID = "album_id"
        private const val KEY_DURATION = "duration"
        private const val KEY_DATE_ADDED = "date_added"
        private const val KEY_URI = "uri"
        private const val KEY_FOLDER_NAME = "folder_name"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_YEAR = "year"
        private const val KEY_ALBUM_ARTIST = "album_artist"
        private const val KEY_COMPOSER = "composer"
        private const val KEY_TRACK_NUMBER = "track_number"
        private const val KEY_DISC_NUMBER = "disc_number"
        private const val KEY_FILE_SIZE = "file_size"
        private const val KEY_MIME_TYPE = "mime_type"
        private const val KEY_GENRE = "genre"
    }
}
