package com.rudi.audioplayer.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Id3TagWriter
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.ui.theme.frostedGlass
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Batch 726 — penyimpan tag untuk menu ⋮ Library. Disediakan MainActivity hanya di sekitar layar
 * Library; `null` (default) = tidak tersedia, jadi item "Edit Info Lagu" tidak ditampilkan.
 * CompositionLocal dipilih agar kontrak `SongRow`/`SongListView`/`SearchResultsView` tidak perlu
 * menerima callback baru di tiap lapisan.
 */
val LocalSongTagSaver = staticCompositionLocalOf<((Song, Id3TagWriter.EditableTags) -> Unit)?> { null }

/**
 * Batch 727 — detail teknis LAMBAT yang hanya bisa dibaca dari isi file (sample rate, bit depth).
 * Dipisah dari nama berkas karena `MediaMetadataRetriever.setDataSource` pada .opus/.ogg besar
 * memakan >3 dtk di perangkat uji; hasil non-null = selesai (nilai individual boleh null).
 */
internal data class SongTechProbe(
    val sampleRateHz: Int?,
    val bitsPerSample: Int?
)

/** Format tampilan murni (tanpa Context) agar teruji di JVM. */
internal object SongInfoFormat {

    fun fileSize(bytes: Long, locale: Locale = Locale.getDefault()): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(locale, "%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format(locale, "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(locale, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    /**
     * Label format. Ekstensi nama berkas didahulukan karena `audio/ogg` ambigu (Vorbis atau Opus);
     * tanpa ekstensi dikenal, jatuh ke MIME; tanpa keduanya "Tidak diketahui".
     */
    fun container(mime: String?, fileName: String?): String {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        val byExt = when (ext) {
            "mp3" -> "MP3"
            "flac" -> "FLAC"
            "m4a", "mp4" -> "AAC (M4A)"
            "aac" -> "AAC"
            "ogg", "oga" -> "OGG"
            "opus" -> "Opus (OGG)"
            "wav" -> "WAV"
            "wma" -> "WMA"
            "amr" -> "AMR"
            "3gp" -> "3GP"
            else -> null
        }
        if (byExt != null) return byExt
        val m = mime?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when (m) {
            "" -> "Tidak diketahui"
            "audio/mpeg", "audio/mp3" -> "MP3"
            "audio/flac", "audio/x-flac" -> "FLAC"
            "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac" -> "AAC (M4A)"
            "audio/ogg", "application/ogg" -> "OGG"
            "audio/opus" -> "Opus"
            "audio/x-wav", "audio/wav", "audio/wave", "audio/vnd.wave" -> "WAV"
            "audio/x-ms-wma" -> "WMA"
            "audio/amr" -> "AMR"
            "audio/3gpp" -> "3GP"
            else -> m.substringAfter('/').removePrefix("x-").uppercase(Locale.ROOT).ifBlank { "Tidak diketahui" }
        }
    }

    fun bitrate(bps: Long?): String? = bps?.takeIf { it > 0L }?.let { "${(it + 500L) / 1000L} kbps" }

    /**
     * Bitrate rata-rata = ukuran berkas x 8 / durasi; tersedia SEKETIKA dari kolom MediaStore
     * (nilai yang sama dgn yang dilaporkan retriever di log uji: 125,9 MiB / 128:11 = 137 kbps).
     * Null bila ukuran atau durasi tak valid.
     */
    fun averageBitrate(sizeBytes: Long, durationMs: Long): String? {
        if (sizeBytes <= 0L || durationMs <= 0L) return null
        return bitrate(sizeBytes * 8_000L / durationMs)
    }

    fun sampleRate(hz: Int?, locale: Locale = Locale.getDefault()): String? {
        if (hz == null || hz <= 0) return null
        return if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(locale, "%.1f kHz", hz / 1000.0)
    }

    fun bitDepth(bits: Int?): String? = bits?.takeIf { it > 0 }?.let { "$it-bit" }
}

/** Nama berkas lewat MediaProvider (milidetik). Blocking: panggil dari Dispatchers.IO. */
private fun queryFileName(context: Context, song: Song): String? {
    var fileName: String? = null
    try {
        context.contentResolver.query(song.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) fileName = c.getString(0)?.takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        AppLogger.e("SongInfoSheet", "Gagal baca nama berkas uri=${song.uri}", e)
    }
    return fileName
}

/** Sample rate + bit depth dari isi file. LAMBAT, blocking: panggil dari Dispatchers.IO. */
private fun probeTechnical(context: Context, song: Song): SongTechProbe {
    var sampleRate: Int? = null
    var bits: Int? = null
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, song.uri)
        sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
        bits = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()
    } catch (e: Exception) {
        AppLogger.e("SongInfoSheet", "Gagal baca metadata teknis uri=${song.uri}", e)
    } finally {
        retriever.release()
    }
    return SongTechProbe(sampleRate, bits)
}

/**
 * Baris Info Lagu. [fileName] null = belum/tidak ada; [tech] null = probe lambat belum selesai
 * (baris "Sample Rate" diisi "Memuat…" agar tinggi sheet tak melompat saat hasil datang).
 */
private fun songInfoRows(song: Song, fileName: String?, tech: SongTechProbe?): List<Pair<String, String>> = buildList {
    add("Judul" to song.title)
    add("Artis" to song.artist)
    if (song.album.isNotBlank()) add("Album" to song.album)
    song.albumArtist?.let { add("Artis Album" to it) }
    song.composer?.let { add("Komposer" to it) }
    song.genre?.let { add("Genre" to it) }
    if (song.year > 0) add("Tahun" to song.year.toString())
    song.trackNumber?.let { add("Nomor Trek" to it.toString()) }
    song.discNumber?.let { add("Nomor Disk" to it.toString()) }
    add("Durasi" to formatDuration(song.duration))
    add("Format" to SongInfoFormat.container(song.mimeType, fileName))
    SongInfoFormat.averageBitrate(song.fileSize, song.duration)?.let { add("Bitrate (rata-rata)" to it) }
    if (tech == null) {
        add("Sample Rate" to "Memuat…")
    } else {
        SongInfoFormat.sampleRate(tech.sampleRateHz)?.let { add("Sample Rate" to it) }
        SongInfoFormat.bitDepth(tech.bitsPerSample)?.let { add("Kedalaman Bit" to it) }
    }
    if (song.fileSize > 0L) add("Ukuran" to SongInfoFormat.fileSize(song.fileSize))
    if (song.dateAdded > 0L) {
        add("Ditambahkan" to SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(song.dateAdded * 1000L)))
    }
    fileName?.let { add("Nama Berkas" to it) }
    add("Folder" to song.folderPath.ifBlank { song.folderName })
}

/**
 * Batch 726 — Info Lagu (baca-saja): semua metadata yang sudah ada di [Song] + detail dari file.
 * Batch 727 — dimuat DUA TAHAP di Dispatchers.IO: (1) nama berkas (cepat) -> Format/Nama Berkas
 * langsung benar (.opus tak lagi tampil "OGG"); (2) sample rate/bit depth (lambat, baris
 * "Memuat…" sampai selesai). Bitrate rata-rata dihitung seketika dari ukuran dan durasi.
 * Judul sheet dan tombol Edit dipatok di luar area scroll. Durasi tiap tahap dicatat ke
 * AppLogger sebagai angka mentah. [onEdit] != null menampilkan tombol "Edit Info Lagu".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: Song,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var fileName by remember(song.id) { mutableStateOf<String?>(null) }
    var tech by remember(song.id) { mutableStateOf<SongTechProbe?>(null) }
    LaunchedEffect(song.id) {
        val t0 = SystemClock.elapsedRealtime()
        fileName = withContext(Dispatchers.IO) { queryFileName(context, song) }
        val t1 = SystemClock.elapsedRealtime()
        tech = withContext(Dispatchers.IO) { probeTechnical(context, song) }
        val t2 = SystemClock.elapsedRealtime()
        AppLogger.w(
            "SongInfoSheet",
            "probe nama=${t1 - t0}ms teknis=${t2 - t1}ms mime=${song.mimeType} ukuran=${song.fileSize}B uri=${song.uri}"
        )
    }
    val rows = remember(song, fileName, tech) { songInfoRows(song, fileName, tech) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Info Lagu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Hanya daftar baris yang bergulir; judul di atas dan tombol Edit di bawah tetap terlihat.
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        rows.forEach { (label, value) ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (onEdit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Info Lagu")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
