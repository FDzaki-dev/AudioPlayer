package com.rudi.audioplayer.ui

import android.content.Context
import android.media.MediaMetadataRetriever
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

/** Detail file yang hanya bisa dibaca dari file itu sendiri; diambil sekali saat sheet dibuka. */
internal data class SongFileProbe(
    val fileName: String?,
    val bitrateBps: Long?,
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

    fun sampleRate(hz: Int?, locale: Locale = Locale.getDefault()): String? {
        if (hz == null || hz <= 0) return null
        return if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(locale, "%.1f kHz", hz / 1000.0)
    }

    fun bitDepth(bits: Int?): String? = bits?.takeIf { it > 0 }?.let { "$it-bit" }
}

/** Baca nama berkas + bitrate/sample rate/bit depth. Blocking: panggil dari Dispatchers.IO. */
private fun probeSongFile(context: Context, song: Song): SongFileProbe {
    var fileName: String? = null
    try {
        context.contentResolver.query(song.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) fileName = c.getString(0)?.takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        AppLogger.e("SongInfoSheet", "Gagal baca nama berkas uri=${song.uri}", e)
    }
    var bitrate: Long? = null
    var sampleRate: Int? = null
    var bits: Int? = null
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, song.uri)
        bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
        sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
        bits = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()
    } catch (e: Exception) {
        AppLogger.e("SongInfoSheet", "Gagal baca metadata teknis uri=${song.uri}", e)
    } finally {
        retriever.release()
    }
    return SongFileProbe(fileName, bitrate, sampleRate, bits)
}

private fun songInfoRows(song: Song, probe: SongFileProbe?): List<Pair<String, String>> = buildList {
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
    add("Format" to SongInfoFormat.container(song.mimeType, probe?.fileName))
    SongInfoFormat.bitrate(probe?.bitrateBps)?.let { add("Bitrate" to it) }
    SongInfoFormat.sampleRate(probe?.sampleRateHz)?.let { add("Sample Rate" to it) }
    SongInfoFormat.bitDepth(probe?.bitsPerSample)?.let { add("Kedalaman Bit" to it) }
    if (song.fileSize > 0L) add("Ukuran" to SongInfoFormat.fileSize(song.fileSize))
    if (song.dateAdded > 0L) {
        add("Ditambahkan" to SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(song.dateAdded * 1000L)))
    }
    probe?.fileName?.let { add("Nama Berkas" to it) }
    add("Folder" to song.folderPath.ifBlank { song.folderName })
}

/**
 * Batch 726 — Info Lagu (baca-saja): semua metadata yang sudah ada di [Song] + detail teknis
 * dari file (bitrate, sample rate, bit depth, nama berkas) yang dimuat sekali di Dispatchers.IO.
 * [onEdit] != null menampilkan tombol "Edit Info Lagu".
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
    var probe by remember(song.id) { mutableStateOf<SongFileProbe?>(null) }
    LaunchedEffect(song.id) {
        probe = withContext(Dispatchers.IO) { probeSongFile(context, song) }
    }
    val rows = remember(song, probe) { songInfoRows(song, probe) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "Info Lagu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            if (onEdit != null) {
                Spacer(modifier = Modifier.height(16.dp))
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
