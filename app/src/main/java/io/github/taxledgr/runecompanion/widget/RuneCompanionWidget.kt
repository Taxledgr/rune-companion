package io.github.taxledgr.runecompanion.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.features.FeaturePreferences
import io.github.taxledgr.runecompanion.features.skillOrEmpty
import java.text.NumberFormat

class RuneCompanionWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views(context))
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RuneCompanionWidget::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, views(context))
            }
        }

        private fun views(context: Context): RemoteViews {
            val data = FeaturePreferences(context).load()
            val profile = data.accounts.firstOrNull {
                it.username.equals(data.selectedAccount, true)
            } ?: data.accounts.firstOrNull()
            val latest = profile?.latest?.summary
            val subtitle = if (latest == null) {
                "Tap to add a tracked OSRS account"
            } else {
                val overall = latest.skillOrEmpty("Overall")
                "Total ${overall.level}  •  ${NumberFormat.getIntegerInstance().format(overall.xp)} XP"
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            return RemoteViews(context.packageName, R.layout.widget_rune_companion).apply {
                setTextViewText(R.id.widget_title, profile?.username ?: "Rune Companion")
                setTextViewText(R.id.widget_subtitle, subtitle)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
        }
    }
}

