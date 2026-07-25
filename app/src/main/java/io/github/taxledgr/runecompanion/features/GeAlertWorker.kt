package io.github.taxledgr.runecompanion.features

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import io.github.taxledgr.runecompanion.toolkit.PriceClient
import io.github.taxledgr.runecompanion.toolkit.PriceWatchItem
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

object GeAlertScheduler {
    fun sync(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        manager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GeAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build(),
        )
    }

    private const val WORK_NAME = "ge-price-alerts"
}

class GeAlertWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val preferences = FeaturePreferences(applicationContext)
        val data = preferences.load()
        if (data.geAlerts.isEmpty()) return Result.success()
        val latest = PriceClient().latest(
            data.geAlerts.map { PriceWatchItem(it.itemId, it.itemName) }.distinctBy { it.id },
        ).associateBy { it.id }
        val triggered = mutableListOf<GeAlert>()
        val alerts = data.geAlerts.map { alert ->
            val price = latest[alert.itemId]?.high ?: latest[alert.itemId]?.low
            val met = price?.let {
                if (alert.alertWhenAbove) it >= alert.targetPrice else it <= alert.targetPrice
            } ?: false
            if (met && !alert.targetWasMet) triggered += alert.copy(latestPrice = price)
            alert.copy(latestPrice = price, targetWasMet = met)
        }
        preferences.save(data.copy(geAlerts = alerts))
        if (triggered.isNotEmpty()) notifyTargets(triggered)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )

    @SuppressLint("MissingPermission")
    private fun notifyTargets(alerts: List<GeAlert>) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GE price targets", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val summary = alerts.take(3).joinToString("\n") {
            "${it.itemName}: ${NumberFormat.getIntegerInstance().format(it.latestPrice)} gp"
        }
        val intent = PendingIntent.getActivity(
            applicationContext,
            91,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(applicationContext).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_rune)
                .setContentTitle(
                    if (alerts.size == 1) "GE target reached" else "${alerts.size} GE targets reached",
                )
                .setContentText(summary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "ge_price_targets"
        const val NOTIFICATION_ID = 3101
    }
}
