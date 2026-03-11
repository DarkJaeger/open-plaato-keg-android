package com.openplaato.keg.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.openplaato.keg.R

object PourNotificationHelper {
    const val CHANNEL_ID = "pour_events"

    fun createChannel(context: Context) {
        val ch = NotificationChannel(CHANNEL_ID, "Pour Events", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Notifies when a pour is detected on a keg"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun showPourNotification(context: Context, label: String, amount: Int, unit: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pour on $label")
            .setContentText("Last pour · $amount $unit")
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(label.hashCode(), n)
    }
}
