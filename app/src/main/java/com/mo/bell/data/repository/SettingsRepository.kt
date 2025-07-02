package com.mo.bell.data.repository

import android.content.SharedPreferences
import com.mo.bell.data.database.dao.SettingsDao
import com.mo.bell.data.database.entities.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VOLUME_LEVEL = "volume_level"
        private const val KEY_SOUND_DURATION = "sound_duration"
        private const val KEY_VIBRATION_MODE = "vibration_mode"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
        private const val KEY_SCHOOL_LATITUDE = "school_latitude"
        private const val KEY_SCHOOL_LONGITUDE = "school_longitude"
        private const val KEY_ACTIVATION_RADIUS = "activation_radius"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_MANUAL_MODE = "manual_mode"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_BACKGROUND_SERVICE_ENABLED = "background_service_enabled"
    }

    /**
     * الحصول على جميع الإعدادات
     */
    suspend fun getAllSettings(): Settings? {
        return settingsDao.getSettings()
    }

    /**
     * الحصول على الإعدادات كـ Flow
     */
    fun getSettingsFlow(): Flow<Settings?> {
        return settingsDao.getSettingsFlow()
    }

    /**
     * حفظ الإعدادات
     */
    suspend fun saveSettings(settings: Settings) {
        settingsDao.insertOrUpdateSettings(settings)
    }

    // إعدادات الصوت
    fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun getVolumeLevel(): Int {
        return sharedPreferences.getInt(KEY_VOLUME_LEVEL, 70)
    }

    fun setVolumeLevel(level: Int) {
        sharedPreferences.edit().putInt(KEY_VOLUME_LEVEL, level).apply()
    }

    fun getSoundDuration(): Int {
        return sharedPreferences.getInt(KEY_SOUND_DURATION, 5)
    }

    fun setSoundDuration(duration: Int) {
        sharedPreferences.edit().putInt(KEY_SOUND_DURATION, duration).apply()
    }

    fun getVibrationMode(): VibrationMode {
        val mode = sharedPreferences.getString(KEY_VIBRATION_MODE, VibrationMode.SOUND_WITH_VIBRATION.name)
        return try {
            VibrationMode.valueOf(mode ?: VibrationMode.SOUND_WITH_VIBRATION.name)
        } catch (e: Exception) {
            VibrationMode.SOUND_WITH_VIBRATION
        }
    }

    fun setVibrationMode(mode: VibrationMode) {
        sharedPreferences.edit().putString(KEY_VIBRATION_MODE, mode.name).apply()
    }

    // إعدادات الموقع
    fun isLocationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_LOCATION_ENABLED, true)
    }

    fun setLocationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply()
    }

    fun getSchoolLocation(): SchoolLocation? {
        val latitude = sharedPreferences.getFloat(KEY_SCHOOL_LATITUDE, Float.NaN)
        val longitude = sharedPreferences.getFloat(KEY_SCHOOL_LONGITUDE, Float.NaN)
        
        return if (!latitude.isNaN() && !longitude.isNaN()) {
            SchoolLocation(latitude.toDouble(), longitude.toDouble())
        } else {
            null
        }
    }

    fun setSchoolLocation(latitude: Double, longitude: Double) {
        sharedPreferences.edit()
            .putFloat(KEY_SCHOOL_LATITUDE, latitude.toFloat())
            .putFloat(KEY_SCHOOL_LONGITUDE, longitude.toFloat())
            .apply()
    }

    fun getActivationRadius(): Double {
        return sharedPreferences.getFloat(KEY_ACTIVATION_RADIUS, 100f).toDouble()
    }

    fun setActivationRadius(radius: Double) {
        sharedPreferences.edit().putFloat(KEY_ACTIVATION_RADIUS, radius.toFloat()).apply()
    }

    // إعدادات التطبيق
    fun isDarkModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun isManualModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_MANUAL_MODE, false)
    }

    fun setManualModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_MANUAL_MODE, enabled).apply()
    }

    fun isNotificationEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun isBackgroundServiceEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BACKGROUND_SERVICE_ENABLED, true)
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BACKGROUND_SERVICE_ENABLED, enabled).apply()
    }

    /**
     * إعادة تعيين جميع الإعدادات للقيم الافتراضية
     */
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * تصدير الإعدادات
     */
    fun exportSettings(): Map<String, Any> {
        return sharedPreferences.all
    }

    /**
     * استيراد الإعدادات
     */
    fun importSettings(settings: Map<String, Any>) {
        val editor = sharedPreferences.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }

    /**
     * الحصول على ملخص الإعدادات
     */
    fun getSettingsSummary(): SettingsSummary {
        return SettingsSummary(
            soundEnabled = isSoundEnabled(),
            volumeLevel = getVolumeLevel(),
            soundDuration = getSoundDuration(),
            vibrationMode = getVibrationMode(),
            locationEnabled = isLocationEnabled(),
            schoolLocationSet = getSchoolLocation() != null,
            activationRadius = getActivationRadius(),
            darkModeEnabled = isDarkModeEnabled(),
            manualModeEnabled = isManualModeEnabled(),
            notificationEnabled = isNotificationEnabled(),
            backgroundServiceEnabled = isBackgroundServiceEnabled()
        )
    }
}

/**
 * أنماط الاهتزاز
 */
enum class VibrationMode {
    SOUND_WITH_VIBRATION,
    SOUND_ONLY,
    VIBRATION_ONLY,
    SILENT
}

/**
 * موقع المدرسة
 */
data class SchoolLocation(
    val latitude: Double,
    val longitude: Double
)

/**
 * ملخص الإعدادات
 */
data class SettingsSummary(
    val soundEnabled: Boolean,
    val volumeLevel: Int,
    val soundDuration: Int,
    val vibrationMode: VibrationMode,
    val locationEnabled: Boolean,
    val schoolLocationSet: Boolean,
    val activationRadius: Double,
    val darkModeEnabled: Boolean,
    val manualModeEnabled: Boolean,
    val notificationEnabled: Boolean,
    val backgroundServiceEnabled: Boolean
)

