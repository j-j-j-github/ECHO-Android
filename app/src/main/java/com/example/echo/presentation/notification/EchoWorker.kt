package com.example.echo.presentation.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.domain.repository.EchoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

@HiltWorker
class EchoWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: EchoRepository,
    private val notificationManager: EchoNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext

            // 1. Check for New Echoes
            val echoes = repository.getEchoes().firstOrNull()
            if (!echoes.isNullOrEmpty()) {
                val latestEcho = echoes.first()
                val lastId = SharedPreferencesManager.getLastEchoId(context)

                if (lastId != latestEcho.id) {
                    notificationManager.showNotification(
                        "New Resonance Detected",
                        "A new voice echoes in the void...",
                        1001
                    )
                    latestEcho.id?.let { SharedPreferencesManager.setLastEchoId(context, it) }
                }
            }

            // 2. Check for Replies (Notifications)
            val userNotifications = repository.getUserNotifications()
            val lastCount = SharedPreferencesManager.getLastReplyCount(context)

            if (userNotifications.size > lastCount) {
                notificationManager.showNotification(
                    "The Void Answered",
                    "Souls have whispered to your Echo.",
                    1002
                )
                SharedPreferencesManager.setLastReplyCount(context, userNotifications.size)
            }

            // 3. Random Engagement Prompt (10% chance)
            if (Random.nextInt(100) < 10) {

                // ✅ PREMIUM DYNAMIC NOTIFICATION LIST
                val engagementPrompts = listOf(
                    Pair("The Silence is Loud", "Have something on your mind? Echo it."),
                    Pair("A Quiet Check-In", "How are you feeling today? The void is listening."),
                    Pair("Is it feeling too much?", "You don't have to carry it alone. Release it here."),
                    Pair("End of the Day", "How was your day? Leave your thoughts behind before you rest."),
                    Pair("A Moment of Pause", "Take a deep breath. What frequency are you on right now?"),
                    Pair("Weight of the World", "If it's heavy, set it down for a while."),
                    Pair("Unspoken Words", "What is something you wish you could say out loud?"),
                    Pair("Seek the Silence", "Find a quiet space and tune into your thoughts.")
                )

                // Pick a random prompt from the list
                val randomPrompt = engagementPrompts.random()

                notificationManager.showNotification(
                    randomPrompt.first, // Title
                    randomPrompt.second, // Body
                    1003
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}