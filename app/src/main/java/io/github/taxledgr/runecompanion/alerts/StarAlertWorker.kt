package io.github.taxledgr.runecompanion.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.taxledgr.runecompanion.data.StarRepository
import io.github.taxledgr.runecompanion.data.WorldDirectoryClient

class StarAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        if (!StarAlertPreferences(applicationContext).load().enabled) {
            return Result.success()
        }

        return runCatching {
            runCatching { WorldDirectoryClient().fetch() }
                .onSuccess { worlds ->
                    StarFilterPreferences(applicationContext).updateWorldSafety(worlds)
                }
            val feed = StarRepository.latest()
            StarAlertNotifier(applicationContext).notifyForMatches(feed.stars)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
