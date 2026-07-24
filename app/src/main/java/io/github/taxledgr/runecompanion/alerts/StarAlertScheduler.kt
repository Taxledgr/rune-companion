package io.github.taxledgr.runecompanion.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object StarAlertScheduler {
    fun sync(context: Context, enabled: Boolean, runImmediately: Boolean = false) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWork = PeriodicWorkRequestBuilder<StarAlertWorker>(
            15,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork,
        )

        if (runImmediately) {
            val immediateWork = OneTimeWorkRequestBuilder<StarAlertWorker>()
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWork,
            )
        }
    }

    private const val PERIODIC_WORK_NAME = "shooting-star-alerts-periodic"
    private const val IMMEDIATE_WORK_NAME = "shooting-star-alerts-immediate"
}
