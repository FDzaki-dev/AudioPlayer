package com.rudi.audioplayer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Batch 519 — Wave 2 T9 (MOVE-ONLY): WelcomeScreen/WelcomeHighlight/PermissionRationale dipindah dari
// MainActivity.kt (paket sama `com.rudi.audioplayer`, jadi 0 import baru di MainActivity). Badan identik
// karakter-per-karakter; satu-satunya beda = `private` -> `internal` pada 2 fungsi yang dipanggil dari
// MainActivity (WelcomeScreen, PermissionRationale). WelcomeHighlight tetap private: hanya dipakai di file
// ini. Lihat CHANGELOG Batch 519.

@Composable
internal fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                )
            )
            // Batch 111 — layar ini render DI LUAR Scaffold (lihat setContent di MainActivity),
            // jadi tidak dapat contentWindowInsets bawaan Scaffold sama sekali. Di gesture-nav
            // (Android 16 test device) bar cuma overlay tipis nyaris tak kelihatan; di 3-button
            // nav (masih umum Android 15 ke bawah) bar opaque menutupi konten — insets manual di
            // sini yang menutup gap-nya. Padding fixed 32dp tetap di bawah (jarak visual dari
            // konten ke insets), bukan pengganti.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "SELAMAT DATANG",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "SONIX",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Untuk menampilkan koleksi musik kamu, aplikasi ini butuh izin membaca file audio di perangkat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        WelcomeHighlight(Icons.Default.WifiOff, "Semua diproses di HP — tidak ada data yang dikirim ke internet")
        Spacer(modifier = Modifier.height(12.dp))
        WelcomeHighlight(Icons.Default.FolderOff, "Kamu yang atur folder mana yang tampil")
        Spacer(modifier = Modifier.height(12.dp))
        WelcomeHighlight(Icons.Default.GraphicEq, "Equalizer, crossfade, sampai widget — lengkap")

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Lanjutkan")
        }
    }
}

@Composable
private fun WelcomeHighlight(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
internal fun PermissionRationale(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Batch 111 — sama seperti WelcomeScreen, layar ini juga render di luar Scaffold.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Izin akses musik dibutuhkan",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Aplikasi tidak bisa menampilkan lagu tanpa izin ini. Kalau tombol di bawah tidak memunculkan dialog izin, aktifkan izinnya lewat Pengaturan Aplikasi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Coba Lagi")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Buka Pengaturan Aplikasi")
        }
    }
}
