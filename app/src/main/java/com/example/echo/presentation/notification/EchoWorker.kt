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
import java.util.concurrent.TimeUnit
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
            val now = System.currentTimeMillis()

            val localPrefs = context.getSharedPreferences("EchoWorkerPrefs", Context.MODE_PRIVATE)

            // Tracks the absolute last time ANY notification fired (for Engagement's 2h rule)
            var absoluteLastNotif = SharedPreferencesManager.getLastNotificationTimestamp(context)

            fun markAbsoluteNotifSent() {
                absoluteLastNotif = now
                SharedPreferencesManager.setLastNotificationTimestamp(context, now)
            }

            // ==========================================================
            // 1. RESONANCES -> Instant, Bypasses all Cooldowns
            // ==========================================================
            val userNotifications = repository.getUserNotifications()
            val lastCount = SharedPreferencesManager.getLastReplyCount(context)

            if (userNotifications.size > lastCount) {
                notificationManager.showNotification(
                    "The Void Answered",
                    "Souls have whispered to your Echo.",
                    1002,
                    type = "reply"
                )
                SharedPreferencesManager.setLastReplyCount(context, userNotifications.size)
                markAbsoluteNotifSent()
            }

            // ==========================================================
            // SYSTEM NOTIFICATIONS (Energy, Echo, Engagement)
            // Rule: Only ONE can fire per cycle, and only if 30 mins have passed.
            // ==========================================================
            val lastSystemNotif = localPrefs.getLong("last_system_notif", 0L)
            val minSystemGap = TimeUnit.MINUTES.toMillis(30)

            // If 30 minutes haven't passed since the last non-resonance ping, we exit early
            if (now - lastSystemNotif >= minSystemGap) {
                var systemNotifSentThisCycle = false

                // ----------------------------------------------------------
                // 2. ENERGY DRAIN -> 18 Hours (Highest System Priority)
                // ----------------------------------------------------------
                val lastLogin: Long = SharedPreferencesManager.getLastLoginTimestamp(context)

                if (!systemNotifSentThisCycle && now - lastLogin > TimeUnit.HOURS.toMillis(18)) {
                    notificationManager.showNotification(
                        "Soul Energy Fading",
                        "Your energy is draining. Return to the void to stay vibrant.",
                        1004
                    )
                    SharedPreferencesManager.setLastLoginTimestamp(context, now)
                    localPrefs.edit().putLong("last_system_notif", now).apply()
                    markAbsoluteNotifSent()
                    systemNotifSentThisCycle = true
                }

                // ----------------------------------------------------------
                // 3. GLOBAL ECHOES -> 4 Hours (Second System Priority)
                // ----------------------------------------------------------
                val lastEchoNotif = localPrefs.getLong("last_echo_notif", 0L)

                if (!systemNotifSentThisCycle && now - lastEchoNotif >= TimeUnit.HOURS.toMillis(4)) {
                    val echoes = repository.getEchoes().firstOrNull()
                    if (!echoes.isNullOrEmpty()) {
                        val latestEcho = echoes.first()
                        val lastId = SharedPreferencesManager.getLastEchoId(context)

                        if (lastId != latestEcho.id) {
                            notificationManager.showNotification(
                                "New Resonance Detected",
                                "A new voice echoes in the void...",
                                1001,
                                echoId = latestEcho.id,
                                type = "new_echo"
                            )
                            latestEcho.id?.let { SharedPreferencesManager.setLastEchoId(context, it) }
                            localPrefs.edit().putLong("last_echo_notif", now).apply()
                            localPrefs.edit().putLong("last_system_notif", now).apply()
                            markAbsoluteNotifSent()
                            systemNotifSentThisCycle = true
                        }
                    }
                }

                // ----------------------------------------------------------
                // 4. ENGAGEMENT PROMPTS -> 10% Chance OR 1x Daily
                // Rule: Must be 2 hours of absolute silence (including resonances)
                // ----------------------------------------------------------
                if (!systemNotifSentThisCycle && now - absoluteLastNotif >= TimeUnit.HOURS.toMillis(2)) {

                    val lastEngagement = localPrefs.getLong("last_engagement_notif", 0L)

                    // Fire if 24 hours passed OR 10% chance hit
                    if (now - lastEngagement >= TimeUnit.DAYS.toMillis(1) || Random.nextInt(100) < 10) {

                        val engagementPrompts = listOf(
                            Pair("The Silence is Loud", "Have something on your mind? Echo it."),
                            Pair("A Quiet Check-In", "How are you feeling today? The void is listening."),
                            Pair("Is it feeling too much?", "You don't have to carry it alone. Release it here."),
                            Pair("A Moment of Pause", "Take a deep breath. What frequency are you on right now?"),
                            Pair("Weight of the World", "If it's heavy, set it down for a while."),
                            Pair("Seek the Silence", "Find a quiet space and tune into your thoughts.")
                        )

                        val prompt = engagementPrompts.random()
                        notificationManager.showNotification(prompt.first, prompt.second, 1003)

                        localPrefs.edit().putLong("last_engagement_notif", now).apply()
                        localPrefs.edit().putLong("last_system_notif", now).apply()
                        markAbsoluteNotifSent()
                        systemNotifSentThisCycle = true
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}