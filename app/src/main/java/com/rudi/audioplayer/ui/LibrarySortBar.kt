package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Batch 597 — baris "Urutkan: ..." di atas daftar Lagu / grid Album; menu tarik-turun memilih urutan
 * (dipakai bersama oleh kedua tab). [options] = label berurutan, [selectedIndex] = pilihan aktif.
 */
@Composable
internal fun LibrarySortBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // Batch 635 — false = tidak memenuhi lebar (dipakai sejajar di ujung baris chip pada layar pendek/landscape).
    fillWidth: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    "Urutkan: ${options.getOrElse(selectedIndex) { "" }} ▾",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelect(index)
                        },
                        trailingIcon = if (index == selectedIndex) {
                            { Icon(Icons.Default.Check, contentDescription = "Dipilih") }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
