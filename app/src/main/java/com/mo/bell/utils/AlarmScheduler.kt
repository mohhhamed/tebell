package com.mo.bell.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mo.bell.data.Lesson
import com.mo.bell.receivers.AlarmReceiver
import java.util.*

object AlarmScheduler {
    fun scheduleAlarmsForToday(context: Context) {
        val settings = SettingsManager(context)
        val schedule = settings.getSchedule()
        if (schedule.isEmpty()) return

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("ar"))
        val todayLessons = schedule.filter { it.day.equals(dayOfWeek, ignoreCase = true) }

        todayLessons.forEach { lesson ->
            scheduleAlarm(context, lesson, isStart = true)
            scheduleAlarm(context, lesson, isStart = false)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settings = SettingsManager(context)
        val schedule = settings.getSchedule()
        schedule.forEach { lesson ->
            val startIntent = Intent(context, AlarmReceiver::class.java)
            val startRequestCode = createRequestCode(lesson, isStart = true)
            val startPendingIntent = PendingIntent.getBroadcast(context, startRequestCode, startIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (startPendingIntent != null) {
                alarmManager.cancel(startPendingIntent)
                startPendingIntent.cancel()
            }
            val endIntent = Intent(context, AlarmReceiver::class.java)
            val endRequestCode = createRequestCode(lesson, isStart = false)
            val endPendingIntent = PendingIntent.getBroadcast(context, endRequestCode, endIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (endPendingIntent != null) {
                alarmManager.cancel(endPendingIntent)
                endPendingIntent.cancel()
            }
        }
    }

    private fun scheduleAlarm(context: Context, lesson: Lesson, isStart: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeString = if (isStart) lesson.startTime else lesson.endTime
        val parts = timeString.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            return
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_LESSON_PERIOD, lesson.period)
            putExtra(AlarmReceiver.EXTRA_LESSON_DAY, lesson.day)
            putExtra(AlarmReceiver.EXTRA_IS_START, isStart)
        }
        val requestCode = createRequestCode(lesson, isStart)
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun createRequestCode(lesson: Lesson, isStart: Boolean): Int {
        val startEndFlag = if (isStart) 1 else 2
        return (lesson.day.hashCode().mod(1000)) * 100 + lesson.period * 10 + startEndFlag
    }
}