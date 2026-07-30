package com.rudi.audioplayer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.util.AppLogger

/**
 * Read-only viewer for AppLogger's local diagnostic log — errors it caught, and the
 * stack trace of any uncaught crash. Purely a window onto a file that already lives in this
 * app's private storage; nothing shown here is ever sent anywhere. Lets the user actually see
 * what would otherwise be an invisible failure, and copy it out if they want help debugging it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticLogSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var logText by remember { mutableStateOf(AppLogger.readLog()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Log Diagnostik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Catatan error & crash tersimpan lokal di HP ini saja — tidak pernah dikirim ke mana pun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (logText.isBlank()) {
                Text(
                    "Belum ada catatan. Kalau nanti ada error atau crash, jejaknya akan muncul di sini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        logText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(logText))
                        Toast.makeText(context, "Log disalin ke papan klip", Toast.LENGTH_SHORT).show()
                    },
                    enabled = logText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin")
                }
                OutlinedButton(
                    onClick = {
                        AppLogger.clearLog()
                        logText = ""
                    },
                    enabled = logText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus")
                }
            }
        }
    }
}
