package com.example.echo.presentation.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.repository.EchoRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

@HiltWorker
class EchoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: EchoRepositoryImpl,
    private val notificationManager: EchoNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext

            // 1. Check for New Echoes
            // We fetch the latest echoes. If the ID of the first one is different from last saved, it's new.
            val echoes = repository.getEchoes().firstOrNull()
            if (!echoes.isNullOrEmpty()) {
                val latestEcho = echoes.first()
                val lastId = SharedPreferencesManager.getLastEchoId(context)

                if (lastId != latestEcho.id) {
                    // New Post Detected!
                    notificationManager.showNotification(
                        "New Resonance Detected",
                        "A new voice echoes in the void...",
                        1001
                    )
                    // Save this as the new last seen
                    latestEcho.id?.let { SharedPreferencesManager.setLastEchoId(context, it) }
                }
            }

            // 2. Check for Replies (Notifications)
            val userNotifications = repository.getUserNotifications()
            val lastCount = SharedPreferencesManager.getLastReplyCount(context)

            if (userNotifications.size > lastCount) {
                // New Reply Detected!
                notificationManager.showNotification(
                    "The Void Answered",
                    "Someone has replied to your whisper.",
                    1002
                )
                SharedPreferencesManager.setLastReplyCount(context, userNotifications.size)
            }

            // 3. Random Engagement Prompt (10% chance)
            // Only if nothing else happened, to avoid spamming
            if (Random.nextInt(100) < 10) {
                notificationManager.showNotification(
                    "The Silence is Loud",
                    "Have something on your mind? Echo it.",
                    1003
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}