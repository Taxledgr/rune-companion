package io.github.taxledgr.runecompanion.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.taxledgr.runecompanion.data.StarRepository

class StarAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        if (!StarAlertPreferences(applicationContext).load().enabled) {
            return Result.success()
        }

        return runCatching {
            val feed = StarRepository.latest()
            StarAlertNotifier(applicationContext).notifyForMatches(feed.stars)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
