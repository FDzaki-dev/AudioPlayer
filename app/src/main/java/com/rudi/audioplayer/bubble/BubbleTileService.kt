package com.rudi.audioplayer.bubble

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.rudi.audioplayer.data.FloatingBubbleStore

/**
 * Roadmap #11 lanjutan (Batch 100) — Quick Settings Tile supaya bubble bisa dinyalakan/
 * dimatikan langsung dari shade notifikasi, TANPA buka app sama sekali — celah ke-2 dari
 * instruksi user ("tambahkan trigger... bisa juga tambahkan quick setting tile"), pelengkap
 * [FloatingBubbleService]'s auto-start lewat `PlaybackService.maybeStartFloatingBubble()`
 * (yang triggernya "mulai playback", bukan "user sengaja mau toggle bubble-nya saja").
 *
 * Baca/tulis preferensi LANGSUNG ke [FloatingBubbleStore] (SharedPreferences), BUKAN lewat
 * `PlayerViewModel`'s StateFlow — System UI bisa menginstansiasi TileService kapan pun tanpa
 * `MainActivity`/ViewModel pernah hidup sama sekali di sesi itu. Konsekuensi jujur: kalau app
 * KEBETULAN sedang kebuka bareng saat tile di-tap, switch di SettingsScreen baru ikut sinkron
 * lagi setelah layar itu di-resume (lihat `MainActivity`'s `DisposableEffect` ON_RESUME) —
 * StateFlow tidak auto-observe perubahan SharedPreferences dari komponen lain, bukan bug,
 * batasan arsitektur yang sama kelasnya dengan pola "batasan jujur" lain di proyek ini.
 *
 * **`@RequiresApi(N)`**: QS Tile API baru ada sejak Android 7.0 (API 24) — 1 level di atas
 * `minSdk 23` project ini. Class ini TIDAK PERNAH diinstansiasi sistem di device API 23 (fitur
 * Quick Settings Tile custom sendiri belum ada di situ), tapi anotasi tetap wajib supaya lint
 * `NewApi` tidak menganggap ini pemakaian API di bawah `minSdk` tanpa pengaman eksplisit.
 */
@RequiresApi(Build.VERSION_CODES.N)
class BubbleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        val store = FloatingBubbleStore(this)

        if (store.isEnabled()) {
            store.setEnabled(false)
            stopService(Intent(this, FloatingBubbleService::class.java))
            refreshTileState()
            return
        }

        if (Settings.canDrawOverlays(this)) {
            store.setEnabled(true)
            val serviceIntent = Intent(this, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            refreshTileState()
            return
        }

        // Belum ada izin overlay — TileService tidak bisa minta izin sensitif ini langsung
        // (bukan runtime permission dialog biasa, butuh layar sistem penuh
        // Settings.ACTION_MANAGE_OVERLAY_PERMISSION), jadi buka layar itu lewat
        // startActivityAndCollapse alih-alih toggle store langsung. User perlu tap tile lagi
        // setelah izin diberikan — konsisten sama alur MainActivity.toggleFloatingBubble(),
        // cuma titik masuknya beda.
        val permissionIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 401, permissionIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(permissionIntent)
        }
    }

    private fun refreshTileState() {
        val active = FloatingBubbleStore(this).isEnabled() && Settings.canDrawOverlays(this)
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
