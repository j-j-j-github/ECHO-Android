package com.example.echo.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.echo.R
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EchoNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "void_channel"
        const val CHANNEL_NAME = "Echoes from the Void"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications about new echoes and resonances"
                enableLights(true)
                lightColor = android.graphics.Color.WHITE
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        title: String,
        message: String,
        notificationId: Int,
        echoId: String? = null,
        type: String? = null
    ) {

        // --- STABLE PER-ECHO NOTIFICATION ID ---
        val finalNotifId = echoId?.hashCode() ?: notificationId

        var finalTitle = title
        var finalBody = message

        // ✅ RESET COUNTS FOR NEW ECHO (CRITICAL FIX)
        if (echoId != null && type == "new_echo") {
            SharedPreferencesManager.resetUnreadReplyCount(context, echoId)
        }

        // --- COUNT LOGIC (ONLY FOR REPLIES) ---
        if (echoId != null && type == "reply") {
            val count = SharedPreferencesManager.incrementUnreadReplyCount(context, echoId)

            if (count > 1) {
                finalTitle = "New Resonances"
                finalBody = "$count people resonated with your echo"
            } else {
                finalTitle = "The Void Answered"
                finalBody = message
            }
        }

        // --- UNIQUE INTENT PER ECHO ---
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (echoId != null) {
                putExtra("echo_id", echoId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            finalNotifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(finalTitle)
            .setContentText(finalBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF000000.toInt())

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(finalNotifId, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}