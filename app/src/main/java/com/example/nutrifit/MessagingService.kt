package com.example.nutrifit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Store the device token so push notifications can target this user
        val data = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(user.uid)
            .collection("fcm")
            .document("deviceToken")
            .set(data)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        NotificationHelper.ensureChannel(this)

        // Android 13+ notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "NutriFit"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new notification."

        val notification = NotificationCompat.Builder(this, ReminderReceiver.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(2001, notification)
    }
}

