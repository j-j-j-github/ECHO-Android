package com.example.echo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.echo.presentation.notifications.EchoWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EchoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Configure WorkManager to use Hilt's Factory
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupWorker()
    }

    private fun setupWorker() {
        // Constraints: Network must be connected
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Run every 15 minutes (Minimum interval allowed by Android)
        val workRequest = PeriodicWorkRequestBuilder<EchoWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // Enqueue unique work (keeps existing if already running)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EchoSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}