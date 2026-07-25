package io.github.taxledgr.runecompanion.features

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.taxledgr.runecompanion.toolkit.HiscoreClient
import io.github.taxledgr.runecompanion.widget.RuneCompanionWidget
import java.util.concurrent.TimeUnit

object FeatureRefreshScheduler {
    fun sync(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        manager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<FeatureRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build(),
        )
    }

    private const val WORK_NAME = "feature-account-refresh"
}

class FeatureRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = FeaturePreferences(applicationContext)
        val data = preferences.load()
        val client = HiscoreClient()
        var successCount = 0
        val updatedAccounts = data.accounts.map { account ->
            if (!account.autoRefresh || account.username.isBlank()) return@map account
            runCatching { client.lookup(account.username) }.fold(
                onSuccess = { summary ->
                    successCount += 1
                    account.copy(
                        snapshots = compactSnapshots(
                            account.snapshots +
                                StatSnapshot(System.currentTimeMillis(), summary),
                        ),
                    )
                },
                onFailure = { account },
            )
        }
        if (successCount > 0) {
            preferences.save(data.copy(accounts = updatedAccounts))
            RuneCompanionWidget.updateAll(applicationContext)
            return Result.success()
        }
        return if (data.accounts.none { it.autoRefresh }) Result.success() else Result.retry()
    }
}
