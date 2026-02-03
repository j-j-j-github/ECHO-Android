package com.example.echo.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.echo.R
import com.example.echo.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class EchoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // 1. Check if message contains a data payload (echo_id)
        val data = remoteMessage.data
        val echoId = data["echo_id"]

        // 2. Check if message contains a notification payload
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Echo", it.body ?: "New resonance...", echoId)
        }
    }

    override fun onNewToken(token: String) {
        // Token is handled in MainActivity, so we can leave this empty or log it
    }

    private fun sendNotification(title: String, messageBody: String, echoId: String?) {
        // 3. Create Intent to open MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // 4. IMPORTANT: Pass the echo_id to the activity
        if (echoId != null) {
            intent.putExtra("echo_id", echoId)
        }

        // 5. Create PendingIntent (The "Click" Action)
        val requestCode = Random.nextInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "echo_notification_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure you have a valid icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Required for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Echo Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(requestCode, notificationBuilder.build())
    }
}