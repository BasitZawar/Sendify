package com.smartswitch.fcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartswitch.R
import com.smartswitch.utils.DismissReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FirebaseMessaging : FirebaseMessagingService() {
    private lateinit var pendingIntent: PendingIntent

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: ""
            val message = notification.body ?: ""
            val uri = notification.imageUrl ?: ""

            CoroutineScope(Dispatchers.IO).launch {
                showNotification(title, message, uri.toString())
            }
        }
    }

    private fun showNotification(title: String, message: String, imageUrl: String?) {
//      val notificationId = System.currentTimeMillis().toInt()
        val imageBitmap = imageUrl?.let { getBitmapFromUrl(it) }
        val notificationId = 101
        val dismissIntent = Intent(this, DismissReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
        }
        pendingIntent = PendingIntent.getBroadcast(
            this, 0, dismissIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fcm_channel_id", "Notification Channel", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "fcm_channel_id").setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app icon
            .setAutoCancel(true).setContentIntent(pendingIntent).setTimeoutAfter(200000).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(imageBitmap)
            ).setPriority(NotificationCompat.PRIORITY_HIGH).build()
        notificationManager.notify(notificationId, notification)
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}


