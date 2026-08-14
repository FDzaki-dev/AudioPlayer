package com.rudi.audioplayer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rudi.audioplayer.data.ListeningStatsEngine
import com.rudi.audioplayer.ui.theme.Radius
import com.rudi.audioplayer.ui.theme.isSkeuTheme
import com.rudi.audioplayer.ui.theme.isTactileTheme
import com.rudi.audioplayer.ui.theme.skeuEmboss
import com.rudi.audioplayer.ui.theme.tactileEmboss
import java.time.DayOfWeek

/** Full-screen dashboard reached from Settings ("Statistik Dengar"). Every number here is
 * derived from data the app already collects (PlayStatsStore/ListeningHistoryStore/
 * HourlyListenStore, aggregated by ListeningStatsEngine) — nothing new is tracked beyond the
 * hour-of-day counter added in this same batch. */
@Composable
fun StatsDashboardScreen(snapshot: ListeningStatsEngine.Snapshot, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Statistik Dengar", style = MaterialTheme.typography.titleLarge)
        }

        if (snapshot.totalPlays == 0) {
            EmptyState(
                title = "Belum ada data dengar",
                subtitle = "Statistik muncul begitu kamu mulai memutar lagu — jam favorit, artis paling sering, dan tren mingguan akan terisi otomatis."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Headphones,
                            label = "Total Diputar",
                            value = "${snapshot.totalPlays}",
                            unit = "lagu"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        SummaryStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Schedule,
                            label = "Waktu Dengar",
                            value = formatListeningDuration(snapshot.totalListeningMs)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    StatSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tren 7 Hari Terakhir", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        WeeklyTrendChart(snapshot.weeklyTrend)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    StatSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jam Favorit Dengar", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (snapshot.peakHour == null) {
                            Text(
                                "Belum cukup data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            val hour = snapshot.peakHour
                            val nextHour = (hour + 1) % 24
                            Text(
                                "%02d:00–%02d:00".format(hour, nextHour),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${snapshot.peakHourCount} kali diputar di jam ini, sepanjang waktu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    StatSectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Artis Paling Sering", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (snapshot.topArtists.isEmpty()) {
                            Text(
                                "Belum cukup data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        } else {
                            snapshot.topArtists.forEachIndexed { index, artistCount ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        artistCount.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${artistCount.playCount}x",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

/** Small 2-up card for the headline numbers (total plays / total listening time). */
@Composable
private fun SummaryStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    StatSectionCard(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            if (unit != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

/** Reusable panel used for every section on this screen — same conditional Tactile/Skeu emboss
 * vs. flat Surface pattern already used by HomeScreen's ContinueListeningCard (Batch 59). */
@Composable
private fun StatSectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val isTactile = isTactileTheme()
    val isSkeu = isSkeuTheme()
    val isPanelTheme = isTactile || isSkeu
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    isTactile -> Modifier.tactileEmboss(shape = MaterialTheme.shapes.medium, elevation = 6.dp)
                    isSkeu -> Modifier.skeuEmboss(shape = MaterialTheme.shapes.medium, elevation = 6.dp)
                    else -> Modifier.clip(RoundedCornerShape(Radius.xl))
                }
            ),
        color = if (isPanelTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (isPanelTheme) 0.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Simple 7-bar chart, hand-drawn on Canvas — first custom chart in this codebase, so kept
 * deliberately minimal (rounded bars only, no gridlines/axis-drawing/text-on-canvas) to limit
 * surface area for a rendering mistake that can't be caught without a compiler in this
 * environment. Day labels are ordinary Text composables below the Canvas, not drawn on it. */
@Composable
private fun WeeklyTrendChart(days: List<ListeningStatsEngine.DayCount>) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    val maxCount = days.maxOfOrNull { it.playCount } ?: 0

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        if (days.isEmpty()) return@Canvas
        val slotWidth = size.width / days.size
        val barWidth = slotWidth * 0.5f
        val cornerRadius = CornerRadius(barWidth / 2f)
        days.forEachIndexed { index, day ->
            val fraction = if (maxCount > 0) day.playCount.toFloat() / maxCount.toFloat() else 0f
            val barHeight = (size.height * fraction).coerceAtLeast(6f)
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            // Faint full-height track first, so days with 0 plays are still visible as a slot.
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                dayOfWeekLabel(day.date.dayOfWeek),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun dayOfWeekLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Sen"
    DayOfWeek.TUESDAY -> "Sel"
    DayOfWeek.WEDNESDAY -> "Rab"
    DayOfWeek.THURSDAY -> "Kam"
    DayOfWeek.FRIDAY -> "Jum"
    DayOfWeek.SATURDAY -> "Sab"
    DayOfWeek.SUNDAY -> "Min"
}

/** Formats milliseconds as "Xj Ym" (or just "Ym" under an hour, "0m" when empty). */
private fun formatListeningDuration(ms: Long): String {
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}j ${minutes}m" else "${minutes}m"
}
