package com.rudi.audioplayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.rudi.audioplayer.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Batch 35: [WidgetUpdater.updateAll] decodes, center-crops, and rounds the album-art bitmap —
 * the same expensive work [com.rudi.audioplayer.playback.PlaybackService.pushWidgetUpdate] was
 * moved off the main thread for in Batch 34. This class is a plain BroadcastReceiver
 * (AppWidgetProvider extends it), so [onUpdate]/[onAppWidgetOptionsChanged] run on the main
 * thread by default — that Batch 34 fix never covered these two call sites. Worse here:
 * [onAppWidgetOptionsChanged] fires repeatedly while the user is actively dragging the widget's
 * resize handles, so a blocking decode could stack up main-thread work call after call.
 * [android.content.BroadcastReceiver.goAsync] is the documented way to keep doing work after
 * onReceive-family callbacks return without blocking the caller; [providerScope] backs it with
 * a real coroutine on IO, and [android.content.BroadcastReceiver.PendingResult.finish] always
 * runs in `finally` so the system's wakelock for this broadcast is released even if updateAll
 * throws.
 */
class PlayerWidgetProvider : AppWidgetProvider() {
    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllAsync(context, "onUpdate ids=${appWidgetIds.joinToString(",")}")
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // Fires live as the user drags the widget's resize handles — re-picking the layout
        // immediately means it doesn't wait for the next natural update to adapt.
        // Batch 593 — pemicu dicatat (diagnostik marquee): launcher yang mengirim opsi berulang (mis. tiap
        // halaman layar beranda tampil) akan memicu update beruntun -> marquee judul diulang dari awal.
        updateAllAsync(
            context,
            "onAppWidgetOptionsChanged id=$appWidgetId " +
                "min=${newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)}x" +
                "${newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)}dp " +
                "maks=${newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1)}x" +
                "${newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1)}dp"
        )
    }

    private fun updateAllAsync(context: Context, trigger: String) {
        val pendingResult = goAsync()
        providerScope.launch(Dispatchers.IO) {
            try {
                // Batch 593 — dicatat di thread IO (bukan Main): AppLogger menulis berkas secara sinkron.
                AppLogger.w("PlayerWidgetProvider", "Pemicu update: $trigger")
                WidgetUpdater.updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
