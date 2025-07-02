package com.mo.bell.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mo.bell.R
import com.mo.bell.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_BELL = "bell_notifications"
        private const val CHANNEL_ID_CLASS = "class_notifications"
        private const val CHANNEL_ID_LOCATION = "location_notifications"
        private const val CHANNEL_ID_SERVICE = "service_notifications"
        
        private const val NOTIFICATION_ID_BELL = 1001
        private const val NOTIFICATION_ID_CURRENT_CLASS = 1002
        private const val NOTIFICATION_ID_NEXT_CLASS = 1003
        private const val NOTIFICATION_ID_LOCATION = 1004
        private const val NOTIFICATION_ID_SERVICE = 1005
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * إنشاء قنوات الإشعارات
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BELL,
                    "تنبيهات الجرس",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات بداية ونهاية الحصص"
                    enableVibration(true)
                    setShowBadge(true)
                },
                
                NotificationChannel(
                    CHANNEL_ID_CLASS,
                    "معلومات الحصص",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "معلومات الحصة الحالية والقادمة"
                    setShowBadge(true)
                },
                
                NotificationChannel(
                    CHANNEL_ID_LOCATION,
                    "حالة الموقع",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "تحديثات حالة الموقع الجغرافي"
                    setShowBadge(false)
                },
                
                NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "خدمة التطبيق",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "إشعارات خدمة العمل في الخلفية"
                    setShowBadge(false)
                }
            )

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * إشعار بداية الحصة
     */
    fun showClassStartNotification(className: String, subjectName: String, endTime: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BELL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("بداية الحصة")
            .setContentText("$className - $subjectName")
            .setSubText("تنتهي في $endTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BELL, notification)
    }

    /**
     * إشعار نهاية الحصة
     */
    fun showClassEndNotification(className: String, subjectName: String, nextClassName: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (nextClassName != null) {
            "انتهت حصة $className - $subjectName\nالحصة القادمة: $nextClassName"
        } else {
            "انتهت حصة $className - $subjectName"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BELL)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("نهاية الحصة")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BELL, notification)
    }

    /**
     * إشعار الحصة الحالية (مستمر)
     */
    fun showCurrentClassNotification(
        className: String,
        subjectName: String,
        remainingMinutes: Int,
        progressPercentage: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CLASS)
            .setSmallIcon(R.drawable.ic_schedule)
            .setContentTitle("الحصة الحالية")
            .setContentText("$className - $subjectName")
            .setSubText("متبقي $remainingMinutes دقيقة")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, progressPercentage, false)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CURRENT_CLASS, notification)
    }

    /**
     * إشعار الحصة القادمة
     */
    fun showNextClassNotification(
        className: String,
        subjectName: String,
        minutesUntilStart: Int,
        startTime: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CLASS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("الحصة القادمة")
            .setContentText("$className - $subjectName")
            .setSubText("تبدأ خلال $minutesUntilStart دقيقة في $startTime")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_NEXT_CLASS, notification)
    }

    /**
     * إشعار حالة الموقع
     */
    fun showLocationStatusNotification(isAtSchool: Boolean, distance: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isAtSchool) "داخل المدرسة" else "خارج المدرسة"
        val text = if (isAtSchool) {
            "أنت الآن داخل نطاق المدرسة"
        } else {
            "أنت خارج نطاق المدرسة (${distance.toInt()} متر)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOCATION)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_LOCATION, notification)
    }

    /**
     * إشعار خدمة العمل في الخلفية
     */
    fun showServiceNotification(): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Bell يعمل في الخلفية")
            .setContentText("مراقبة الجدول المدرسي نشطة")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * إلغاء إشعار معين
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * إلغاء إشعار الحصة الحالية
     */
    fun cancelCurrentClassNotification() {
        notificationManager.cancel(NOTIFICATION_ID_CURRENT_CLASS)
    }

    /**
     * إلغاء إشعار الحصة القادمة
     */
    fun cancelNextClassNotification() {
        notificationManager.cancel(NOTIFICATION_ID_NEXT_CLASS)
    }

    /**
     * إلغاء إشعار الموقع
     */
    fun cancelLocationNotification() {
        notificationManager.cancel(NOTIFICATION_ID_LOCATION)
    }

    /**
     * إلغاء جميع الإشعارات
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * فحص ما إذا كانت الإشعارات مفعلة
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * فحص ما إذا كانت قناة معينة مفعلة
     */
    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = systemNotificationManager.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }
}

