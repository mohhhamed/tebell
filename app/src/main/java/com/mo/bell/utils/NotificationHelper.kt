package com.mo.bell.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mo.bell.R
import com.mo.bell.activities.MainActivity

object NotificationHelper {
    private const val LOCATION_CHANNEL_ID = "location_channel"
    private const val SCHEDULE_CHANNEL_ID = "schedule_channel"
    private const val FOREGROUND_CHANNEL_ID = "foreground_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val locationChannel = NotificationChannel(LOCATION_CHANNEL_ID, context.getString(R.string.location_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            val scheduleChannel = NotificationChannel(SCHEDULE_CHANNEL_ID, context.getString(R.string.schedule_channel_name), NotificationManager.IMPORTANCE_HIGH)
            val foregroundChannel = NotificationChannel(FOREGROUND_CHANNEL_ID, context.getString(R.string.foreground_channel_name), NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(locationChannel)
            notificationManager.createNotificationChannel(scheduleChannel)
            notificationManager.createNotificationChannel(foregroundChannel)
        }
    }

    fun createForegroundNotification(context: Context): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.foreground_notification_title))
            .setContentText(context.getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.ic_bell)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun sendNotification(context: Context, title: String, message: String, isSchedule: Boolean = false) {
        val channelId = if (isSchedule) SCHEDULE_CHANNEL_ID else LOCATION_CHANNEL_ID
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsManager = SettingsManager(context)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (isSchedule) {
            val ringtoneName = settingsManager.getAppRingtoneName()
            if (ringtoneName != null) {
                val resourceId = context.resources.getIdentifier(ringtoneName, "raw", context.packageName)
                if (resourceId != 0) {
                    val soundUri = Uri.parse("android.resource://${context.packageName}/$resourceId")
                    builder.setSound(soundUri)
                }
            }
            if (settingsManager.isVibrateEnabled()) {
                builder.setVibrate(longArrayOf(0, 500, 250, 500))
            }
        }
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}