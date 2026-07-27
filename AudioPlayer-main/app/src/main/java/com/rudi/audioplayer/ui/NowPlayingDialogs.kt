package com.rudi.audioplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(remainingMs: Long?, onDismiss: () -> Unit, onSelect: (Int) -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                if (remainingMs != null) Text("Berhenti dlm ${formatDuration(remainingMs)}", color = MaterialTheme.colorScheme.primary)
                listOf(10, 15, 30, 45, 60).forEach { min ->
                    TextButton({ onSelect(min); onDismiss() }, Modifier.fillMaxWidth()) { Text("$min menit") }
                }
            }
        },
        confirmButton = { TextButton(if (remainingMs != null) onCancel else onDismiss) { Text(if (remainingMs != null) "Matikan Timer" else "Tutup") } }
    )
}

@Composable
fun SpeedDialog(speed: Float, crossfade: Boolean, onDismiss: () -> Unit, onSelect: (Float) -> Unit, onToggleCrossfade: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Putar") },
        text = {
            Column {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                    TextButton({ onSelect(s) }, Modifier.fillMaxWidth()) { Text("${s}x ${if (s == speed) "✓" else ""}") }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onToggleCrossfade(!crossfade) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(!crossfade, { onToggleCrossfade(false) })
                    Spacer(Modifier.width(8.dp))
                    Text("Gapless (Murni)")
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Tutup") } }
    )
}
