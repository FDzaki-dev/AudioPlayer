package com.rudi.audioplayer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.updateAll(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // Fires live as the user drags the widget's resize handles — re-picking the layout
        // immediately means it doesn't wait for the next natural update to adapt.
        WidgetUpdater.updateAll(context)
    }
}
