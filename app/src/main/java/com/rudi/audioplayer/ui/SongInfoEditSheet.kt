package com.rudi.audioplayer.ui

import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.Id3TagWriter
import com.rudi.audioplayer.data.Song
import com.rudi.audioplayer.ui.theme.frostedGlass
import java.util.Locale

/**
 * Gap List "Wajib" #1 — form edit metadata. Batch ini SENGAJA cuma bisa nyimpan untuk lagu
 * MediaStore format MP3 ([TagEditor.editabilityCheck] yang tentukan itu di sisi
 * ViewModel/TagEditor, bukan diduplikasi/ditebak ulang di sini) — sheet ini tetap dirender
 * untuk SEMUA lagu (bukan disembunyikan) supaya user tahu field-nya, tapi kalau lagu itu
 * ternyata tidak didukung, [onSave] tetap dipanggil dan pesan "belum didukung"-nya muncul
 * lewat Snackbar (dikirim balik dari TagEditor lewat PlayerViewModel) — bukan divalidasi dua
 * kali dengan logika yang mungkin beda dari sumber kebenarannya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoEditSheet(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (Id3TagWriter.EditableTags) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Key di song.id — sama alasan LyricsSheet: kalau lagu ganti sementara sheet ini entah
    // bagaimana masih terbuka, draft basi tidak boleh ketimpa ke lagu yang salah.
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var albumArtist by remember(song.id) { mutableStateOf(song.albumArtist ?: "") }
    var genre by remember(song.id) { mutableStateOf(song.genre ?: "") }
    var composer by remember(song.id) { mutableStateOf(song.composer ?: "") }
    var trackNumber by remember(song.id) { mutableStateOf(song.trackNumber?.toString() ?: "") }
    var discNumber by remember(song.id) { mutableStateOf(song.discNumber?.toString() ?: "") }

    // Cerminan APA ADANYA dari TagEditor.editabilityCheck — dua pesan berbeda supaya user
    // tahu PERSIS kenapa (folder tambahan vs format file), bukan 1 pesan generik.
    val isMediaStoreSong = song.uri.authority == MediaStore.AUTHORITY
    val mime = song.mimeType?.lowercase(Locale.ROOT)
    val isMp3 = mime == "audio/mpeg" || mime == "audio/mp3"
    val unsupportedReason = when {
        !isMediaStoreSong -> "Lagu dari folder tambahan belum didukung untuk diedit (butuh izin tulis terpisah)."
        !isMp3 -> "Format file ini belum didukung untuk diedit (baru MP3)."
        else -> null
    }

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
                "Edit Info Lagu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (unsupportedReason != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        unsupportedReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artis") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = albumArtist,
                onValueChange = { albumArtist = it },
                label = { Text("Artis Album") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            OutlinedTextField(
                value = composer,
                onValueChange = { composer = it },
                label = { Text("Komposer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = trackNumber,
                    onValueChange = { new -> trackNumber = new.filter { it.isDigit() } },
                    label = { Text("No. Track") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 2.dp)
                )
                OutlinedTextField(
                    value = discNumber,
                    onValueChange = { new -> discNumber = new.filter { it.isDigit() } },
                    label = { Text("No. Disc") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Batal") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            Id3TagWriter.EditableTags(
                                title = title,
                                artist = artist,
                                album = album,
                                albumArtist = albumArtist.ifBlank { null },
                                genre = genre.ifBlank { null },
                                composer = composer.ifBlank { null },
                                trackNumber = trackNumber.toIntOrNull(),
                                discNumber = discNumber.toIntOrNull()
                            )
                        )
                    }
                ) { Text("Simpan") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
