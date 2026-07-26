package io.github.taxledgr.runecompanion.toolkit

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class HiscoreRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val preferences = ToolkitPreferences(applicationContext)
        val originalProfile = preferences.load().trackedPlayer
        if (!originalProfile.autoRefreshEnabled || originalProfile.username.isBlank()) {
            return Result.success()
        }

        return runCatching { HiscoreClient().lookup(originalProfile.username) }
            .fold(
                onSuccess = { summary ->
                    val latestData = preferences.load()
                    val latestProfile = latestData.trackedPlayer
                    if (
                        latestProfile.autoRefreshEnabled &&
                        latestProfile.username.equals(originalProfile.username, ignoreCase = true)
                    ) {
                        preferences.save(
                            latestData.copy(
                                trackedPlayer = latestProfile.copy(
                                    baseline = latestProfile.baseline ?: summary,
                                    latest = summary,
                                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                                ),
                            ),
                        )
                    }
                    Result.success()
                },
                onFailure = { Result.retry() },
            )
    }
}

object HiscoreRefreshScheduler {
    fun sync(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<HiscoreRefreshWorker>(
            BACKGROUND_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private const val WORK_NAME = "tracked-player-hiscores"
    const val BACKGROUND_INTERVAL_MINUTES = 15L
}
