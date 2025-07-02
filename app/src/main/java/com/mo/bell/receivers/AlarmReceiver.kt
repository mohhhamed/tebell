package com.mo.bell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mo.bell.R
import com.mo.bell.utils.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_LESSON_PERIOD = "EXTRA_LESSON_PERIOD"
        const val EXTRA_LESSON_DAY = "EXTRA_LESSON_DAY"
        const val EXTRA_IS_START = "EXTRA_IS_START"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val period = intent.getIntExtra(EXTRA_LESSON_PERIOD, 0)
        val day = intent.getStringExtra(EXTRA_LESSON_DAY) ?: ""
        val isStart = intent.getBooleanExtra(EXTRA_IS_START, true)

        val title: String
        val message: String

        if (isStart) {
            title = context.getString(R.string.lesson_start_notification_title)
            message = context.getString(R.string.lesson_start_notification_text, period, day)
        } else {
            title = context.getString(R.string.lesson_end_notification_title)
            message = context.getString(R.string.lesson_end_notification_text, period, day)
        }
        NotificationHelper.sendNotification(context, title, message, isSchedule = true)
    }
}