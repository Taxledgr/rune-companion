package io.github.taxledgr.runecompanion.alerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.data.ShootingStar
import io.github.taxledgr.runecompanion.util.reportAge

class StarAlertNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = StarAlertPreferences(appContext)

    @SuppressLint("MissingPermission")
    fun notifyForMatches(stars: List<ShootingStar>) {
        synchronized(notificationLock) {
            val settings = preferences.load()
            if (!settings.enabled || !canPostNotifications()) return

            val matchingStars = stars.filter(settings::matches)
            val currentIds = matchingStars.map { it.alertId() }.toSet()
            val seenIds = preferences.readSeenIds()
            val unseenStars = matchingStars.filter { it.alertId() !in seenIds }

            preferences.saveSeenIds(currentIds)
            if (unseenStars.isEmpty()) return

            createChannel()
            NotificationManagerCompat.from(appContext).notify(
                ALERT_NOTIFICATION_ID,
                buildNotification(unseenStars),
            )
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(appContext).cancel(ALERT_NOTIFICATION_ID)
    }

    private fun buildNotification(stars: List<ShootingStar>) =
        NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_rune)
            .setContentTitle(
                if (stars.size == 1) {
                    val star = stars.first()
                    appContext.getString(
                        R.string.star_alert_single_title,
                        star.tier,
                        star.world,
                    )
                } else {
                    appContext.resources.getQuantityString(
                        R.plurals.star_alert_multiple_title,
                        stars.size,
                        stars.size,
                    )
                },
            )
            .setContentText(notificationSummary(stars))
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationSummary(stars)))
            .setContentIntent(openAppIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()

    private fun notificationSummary(stars: List<ShootingStar>): String =
        stars.take(MAX_STARS_IN_SUMMARY).joinToString(separator = "\n") { star ->
            appContext.getString(
                R.string.star_alert_summary_line,
                star.world,
                star.tier,
                star.locationName,
                reportAge(star.calledAt),
            )
        }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        appContext,
        2,
        Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                appContext.getString(R.string.star_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = appContext.getString(R.string.star_alert_channel_description)
            },
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun ShootingStar.alertId(): String =
        "$world:$locationId:${calledAt.epochSecond}"

    private companion object {
        const val ALERT_CHANNEL_ID = "shooting_star_matches"
        const val ALERT_NOTIFICATION_ID = 2001
        const val MAX_STARS_IN_SUMMARY = 3
        val notificationLock = Any()
    }
}
