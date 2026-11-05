package `in`.tuwi.yogaphone.usage

import android.content.Context
import androidx.work.*
import `in`.tuwi.yogaphone.YogaApplication
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class UsageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        (applicationContext as YogaApplication).usageRepository.check(archiveYesterday = true)
        Result.success()
    } catch (_: Exception) { Result.retry() }

    companion object {
        fun schedule(context: Context) {
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork("usage_periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES).build())
            val now = ZonedDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(5)
            manager.enqueueUniquePeriodicWork("usage_daily", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UsageWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS).build())
        }
    }
}
