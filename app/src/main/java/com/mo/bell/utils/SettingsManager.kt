package com.mo.bell.utils

import android.content.Context
import com.google.gson.Gson
import com.mo.bell.data.Lesson

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val KEY_TEACHER_NAME = "teacher_name"
        const val KEY_SCHOOL_NAME = "school_name"
        const val KEY_SCHEDULE_JSON = "schedule_json"
        const val KEY_SCHOOL_LAT = "school_lat"
        const val KEY_SCHOOL_LON = "school_lon"
        const val KEY_VIBRATE = "vibrate_enabled"
        const val KEY_GEOFENCE_RADIUS = "geofence_radius"
        const val KEY_APP_RINGTONE_NAME = "app_ringtone_name" // المفتاح الجديد
    }

    fun save(key: String, value: Any?) {
        prefs.edit().apply {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> putString(key, gson.toJson(value))
            }
            apply()
        }
    }

    fun getTeacherName(): String = prefs.getString(KEY_TEACHER_NAME, "") ?: ""
    fun getSchoolName(): String = prefs.getString(KEY_SCHOOL_NAME, "") ?: ""

    fun getSchedule(): List<Lesson> {
        val json = prefs.getString(KEY_SCHEDULE_JSON, null) ?: return emptyList()
        return gson.fromJson(json, Array<Lesson>::class.java)?.toList() ?: emptyList()
    }

    fun getSchoolLocation(): Pair<Double, Double>? {
        val latBits = prefs.getLong(KEY_SCHOOL_LAT, -1)
        val lonBits = prefs.getLong(KEY_SCHOOL_LON, -1)
        if (latBits == -1L || lonBits == -1L) return null
        return Pair(Double.fromBits(latBits), Double.fromBits(lonBits))
    }

    fun isVibrateEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATE, true)

    fun getGeofenceRadius(): Float = prefs.getFloat(KEY_GEOFENCE_RADIUS, 50f)

    fun getAppRingtoneName(): String? = prefs.getString(KEY_APP_RINGTONE_NAME, null)
}