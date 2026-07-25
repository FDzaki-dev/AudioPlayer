package com.rudi.audioplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit,
    onRequestBiometric: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Masukkan PIN", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { index ->
                val filled = index < entered.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) {
                                if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        if (error) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("PIN salah", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit ->
                    PinKey(label = digit) {
                        error = false
                        if (entered.length < 6) entered += digit
                        if (entered.length == 6) {
                            if (onVerifyPin(entered)) {
                                onUnlocked()
                            } else {
                                error = true
                                entered = ""
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (biometricEnabled) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).clickable { onRequestBiometric() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Buka dengan sidik jari", modifier = Modifier.size(28.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }
            PinKey(label = "0") {
                error = false
                if (entered.length < 6) entered += "0"
                if (entered.length == 6) {
                    if (onVerifyPin(entered)) onUnlocked() else { error = true; entered = "" }
                }
            }
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).clickable {
                    error = false
                    if (entered.isNotEmpty()) entered = entered.dropLast(1)
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Hapus", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
