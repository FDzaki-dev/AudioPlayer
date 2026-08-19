package com.rudi.audioplayer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rudi.audioplayer.ui.theme.Radius
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A small, dismiss-once hint banner for surfacing features a casual user could easily miss
 * (hidden gestures, controls that moved behind a menu) — a lighter-weight alternative to a
 * full onboarding flow or pixel-anchored coach marks. The call site owns persistence (via a
 * settings store) so a given hint only ever shows once, not every time the screen recomposes. */
@Composable
fun FeatureHintBanner(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.ml),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f, fill = true)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Batch 141 — audit hit-target formal kategori #4 MICRO_UIUX_AUDIT.md: 40dp di bawah
            // minimum sentuh Material 48dp. Icon visual (16dp) TIDAK ikut diperbesar — hit-target
            // vs ukuran visual adalah 2 hal berbeda, IconButton 48dp cuma memperluas area sentuh
            // di sekeliling icon kecil yang sama, bukan bikin banner kelihatan lebih "penuh".
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(16.dp))
            }
        }
    }
}
