package com.rudi.audioplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.BuildConfig
import com.rudi.audioplayer.ui.theme.AppTheme
import com.rudi.audioplayer.ui.theme.colorsFor
import com.rudi.audioplayer.ui.theme.typographyFor

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "PENGATURAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "Tampilan & Info",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
            )
        }

        item {
            Text(
                "Tema",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Setiap tema punya warna, jenis huruf, dan bentuk sudutnya sendiri — bukan cuma ganti warna.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(AppTheme.entries.toList()) { theme ->
            ThemeOptionCard(
                theme = theme,
                selected = theme == currentTheme,
                onClick = { onSelectTheme(theme) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Tentang Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "AudioPlayer versi ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Dibuat untuk didengarkan sepenuhnya offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeOptionCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val previewColors = colorsFor(theme)
    val previewTypography = typographyFor(theme)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = previewColors.surface,
        tonalElevation = 4.dp,
        shadowElevation = if (selected) 6.dp else 0.dp,
        border = if (selected) BorderStroke(2.dp, previewColors.primary) else null,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live swatch of the theme's own background/surface/accent — the actual
            // colors, not a description of them.
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(previewColors.background),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(previewColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = previewColors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.displayName,
                    style = previewTypography.titleMedium,
                    color = previewColors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = previewColors.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Tema aktif",
                    tint = previewColors.primary
                )
            }
        }
    }
}
