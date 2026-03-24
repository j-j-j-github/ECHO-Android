package com.example.echo.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
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
        // ✅ Still using v2 to ensure the custom sound is registered by the OS
        const val CHANNEL_ID = "void_channel_v2"
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

                val soundUri = Uri.parse("android.resource://${context.packageName}/raw/echo_ping")

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                // Attach sound to the channel (Required for Android 8.0+)
                setSound(soundUri, audioAttributes)
                enableVibration(true)
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
        // --- RATE LIMITS REMOVED ---
        // Notifications will now always fire regardless of frequency.

        // --- STABLE PER-ECHO NOTIFICATION ID ---
        val finalNotifId = echoId?.hashCode() ?: notificationId

        var finalTitle = title
        var finalBody = message

        // Handle Title/Count Logic for Echoes
        if (echoId != null && type == "new_echo") {
            SharedPreferencesManager.resetUnreadReplyCount(context, echoId)
            finalTitle = "A New Echo Appeared"
        }

        // Handle Title/Count Logic for Replies
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

        val soundUri = Uri.parse("android.resource://${context.packageName}/raw/echo_ping")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(finalTitle)
            .setContentText(finalBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF000000.toInt())

        // ✅ FIX FOR DOUBLE SOUND:
        // Only set the sound on the builder for devices older than Android Oreo (API 26).
        // For Android 8.0+, the system uses the sound defined in the NotificationChannel.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(soundUri)
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(finalNotifId, builder.build())
        } catch (e: SecurityException) {
            Log.e("EchoNotif", "Permission missing for notification", e)
        }
    }
}