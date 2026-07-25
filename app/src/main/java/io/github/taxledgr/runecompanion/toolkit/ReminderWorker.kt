package io.github.taxledgr.runecompanion.toolkit

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.taxledgr.runecompanion.MainActivity
import io.github.taxledgr.runecompanion.R
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        ReminderNotifier(applicationContext).notify(reminderId, title)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_REMINDER_ID = "reminder_id"
    }
}

object ReminderScheduler {
    fun schedule(context: Context, reminder: CompanionReminder) {
        val delay = (reminder.endsAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_TITLE to reminder.title,
                    ReminderWorker.KEY_REMINDER_ID to reminder.id,
                ),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(reminderId))
    }

    private fun workName(reminderId: String) = "companion-reminder-$reminderId"
}

private class ReminderNotifier(context: Context) {
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    fun notify(reminderId: String, title: String) {
        if (!canPostNotifications()) return
        createChannel()
        val openIntent = PendingIntent.getActivity(
            appContext,
            reminderId.hashCode(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(appContext).notify(
            reminderId.hashCode(),
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_rune)
                .setContentTitle(title)
                .setContentText("Your Rune Companion timer is ready.")
                .setContentIntent(openIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat.Builder(appContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_rune)
                        .setContentTitle("Rune Companion")
                        .setContentText("A timer is ready")
                        .build(),
                )
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun createChannel() {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Gameplay timers",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Farming, birdhouse, daily, and custom timer alerts"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "gameplay_timers"
    }
}
