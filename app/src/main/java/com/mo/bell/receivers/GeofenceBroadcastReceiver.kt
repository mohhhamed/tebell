package com.mo.bell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.mo.bell.R
import com.mo.bell.utils.AlarmScheduler
import com.mo.bell.utils.NotificationHelper
import com.mo.bell.utils.SettingsManager

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            return
        }
        val geofenceTransition = geofencingEvent?.geofenceTransition
        val settings = SettingsManager(context)
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            AlarmScheduler.scheduleAlarmsForToday(context)
            val teacherName = settings.getTeacherName().ifBlank { "أستاذنا" }
            val schoolName = settings.getSchoolName().ifBlank { "المدرسة" }
            val welcomeMessage = context.getString(R.string.welcome_notification_text, teacherName, schoolName)
            NotificationHelper.sendNotification(context, context.getString(R.string.welcome_notification_title), welcomeMessage)
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            AlarmScheduler.cancelAllAlarms(context)
            val schoolName = settings.getSchoolName().ifBlank { "المدرسة" }
            val goodbyeMessage = context.getString(R.string.goodbye_notification_text, schoolName)
            NotificationHelper.sendNotification(context, context.getString(R.string.goodbye_notification_title), goodbyeMessage)
        }
    }
}