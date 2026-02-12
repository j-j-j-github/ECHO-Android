package com.example.echo.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        const val CHANNEL_ID = "void_channel"
        const val CHANNEL_NAME = "Echoes from the Void"

        // ✅ NEW: Rate limit constant (Increase this number if you want more)
        const val MAX_ECHOES_PER_HOUR = 10
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
        // -----------------------------------------------------------------------
        // ✅ 1. RATE LIMIT LOGIC (NEW ECHOES ONLY)
        // -----------------------------------------------------------------------
        if (type == "new_echo") {
            val prefs = context.getSharedPreferences("void_rate_limit", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val windowStart = prefs.getLong("window_start", 0L)
            val count = prefs.getInt("count", 0)

            // If 1 hour (3600000 ms) has passed, reset the window
            if (now - windowStart > 3_600_000) {
                prefs.edit()
                    .putLong("window_start", now)
                    .putInt("count", 1)
                    .apply()
            } else {
                // If we are within the hour, check the count
                if (count >= MAX_ECHOES_PER_HOUR) {
                    Log.d("EchoNotif", "Rate limit reached ($count/$MAX_ECHOES_PER_HOUR). Suppressing notification.")
                    return // ⛔ STOP HERE: Too many echoes, suppress to avoid spam flagging
                }
                // Increment count and proceed
                prefs.edit().putInt("count", count + 1).apply()
            }
        }

        // --- STABLE PER-ECHO NOTIFICATION ID ---
        val finalNotifId = echoId?.hashCode() ?: notificationId

        var finalTitle = title
        var finalBody = message

        // ✅ BLOCK A: NEW ECHO (Force Title Here)
        if (echoId != null && type == "new_echo") {
            // 1. Reset reply count
            SharedPreferencesManager.resetUnreadReplyCount(context, echoId)

            // 2. FORCE the title locally to prevent mismatches
            finalTitle = "A New Echo Appeared"
            // The body can stay as the preview text from server
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
            // ✅ Changed to MESSAGE (Android handles this better than SOCIAL for delivery)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
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