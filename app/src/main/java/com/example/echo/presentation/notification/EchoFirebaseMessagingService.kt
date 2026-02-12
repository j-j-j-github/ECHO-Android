package com.example.echo.presentation.notification

import android.util.Log
import com.example.echo.presentation.notifications.EchoNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EchoFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var echoNotificationManager: EchoNotificationManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        // ✅ Always log raw payload
        Log.d("FCM_RAW", "From: ${remoteMessage.from}")
        Log.d("FCM_RAW", "Data: ${remoteMessage.data}")
        Log.d("FCM_RAW", "Notification: ${remoteMessage.notification}")

        try {
            val data = remoteMessage.data

            if (data.isNullOrEmpty()) {
                Log.d("FCM_DEBUG", "No data payload received.")
                return
            }

            val echoId = data["echo_id"] ?: data["echoId"]

            // 🔥 VERY IMPORTANT — explicitly handle type
            val type = data["type"] ?: "unknown"

            val title = data["title"]
                ?: remoteMessage.notification?.title
                ?: "Echo"

            val body = data["body"]
                ?: remoteMessage.notification?.body
                ?: "New activity..."

            echoNotificationManager.showNotification(
                title = title,
                message = body,
                notificationId = 0, // manager handles stable ID
                echoId = echoId,
                type = type
            )

        } catch (e: Exception) {
            Log.e("FCM_CRASH", "Error", e)
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM_DEBUG", "New Token: $token")
    }
}