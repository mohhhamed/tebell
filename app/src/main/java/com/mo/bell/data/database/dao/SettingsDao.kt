package com.mo.bell.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mo.bell.data.database.entities.Settings
import com.mo.bell.data.database.entities.SettingType
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الوصول لبيانات الإعدادات
 */
@Dao
interface SettingsDao {
    
    /**
     * إدراج إعداد جديد
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: Settings): Long
    
    /**
     * إدراج عدة إعدادات
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<Settings>): List<Long>
    
    /**
     * تحديث إعداد
     */
    @Update
    suspend fun updateSetting(setting: Settings): Int
    
    /**
     * حذف إعداد
     */
    @Delete
    suspend fun deleteSetting(setting: Settings): Int
    
    /**
     * حذف إعداد بالمفتاح
     */
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteSettingByKey(key: String): Int
    
    /**
     * حذف جميع الإعدادات
     */
    @Query("DELETE FROM settings")
    suspend fun deleteAllSettings(): Int
    
    /**
     * الحصول على إعداد بالمفتاح
     */
    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): Settings?
    
    /**
     * الحصول على إعداد بالمفتاح كـ LiveData
     */
    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    fun getSettingByKeyLiveData(key: String): LiveData<Settings?>
    
    /**
     * الحصول على إعداد بالمفتاح كـ Flow
     */
    @Query("SELECT * FROM settings WHERE key = :key LIMIT 1")
    fun getSettingByKeyFlow(key: String): Flow<Settings?>
    
    /**
     * الحصول على قيمة إعداد كـ String
     */
    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?
    
    /**
     * الحصول على قيمة إعداد كـ String مع قيمة افتراضية
     */
    @Query("SELECT COALESCE((SELECT value FROM settings WHERE key = :key LIMIT 1), :defaultValue)")
    suspend fun getSettingValueOrDefault(key: String, defaultValue: String): String
    
    /**
     * الحصول على جميع الإعدادات
     */
    @Query("SELECT * FROM settings ORDER BY key ASC")
    suspend fun getAllSettings(): List<Settings>
    
    /**
     * الحصول على جميع الإعدادات كـ LiveData
     */
    @Query("SELECT * FROM settings ORDER BY key ASC")
    fun getAllSettingsLiveData(): LiveData<List<Settings>>
    
    /**
     * الحصول على جميع الإعدادات كـ Flow
     */
    @Query("SELECT * FROM settings ORDER BY key ASC")
    fun getAllSettingsFlow(): Flow<List<Settings>>
    
    /**
     * الحصول على الإعدادات حسب النوع
     */
    @Query("SELECT * FROM settings WHERE type = :type ORDER BY key ASC")
    suspend fun getSettingsByType(type: SettingType): List<Settings>
    
    /**
     * البحث في الإعدادات
     */
    @Query("SELECT * FROM settings WHERE key LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY key ASC")
    suspend fun searchSettings(query: String): List<Settings>
    
    /**
     * تحديث قيمة إعداد
     */
    @Query("UPDATE settings SET value = :value, updated_at = :updatedAt WHERE key = :key")
    suspend fun updateSettingValue(key: String, value: String, updatedAt: String): Int
    
    /**
     * إدراج أو تحديث إعداد
     */
    @Query("INSERT OR REPLACE INTO settings (key, value, type, description, updated_at) VALUES (:key, :value, :type, :description, :updatedAt)")
    suspend fun upsertSetting(key: String, value: String, type: SettingType, description: String?, updatedAt: String): Long
    
    /**
     * التحقق من وجود إعداد
     */
    @Query("SELECT EXISTS(SELECT 1 FROM settings WHERE key = :key)")
    suspend fun settingExists(key: String): Boolean
    
    /**
     * الحصول على عدد الإعدادات
     */
    @Query("SELECT COUNT(*) FROM settings")
    suspend fun getSettingsCount(): Int
    
    /**
     * الحصول على آخر تحديث للإعدادات
     */
    @Query("SELECT MAX(updated_at) FROM settings")
    suspend fun getLastUpdateTime(): String?
    
    // إعدادات محددة مع قيم افتراضية
    
    /**
     * الحصول على اسم المدرس
     */
    @Query("SELECT COALESCE((SELECT value FROM settings WHERE key = 'teacher_name' LIMIT 1), '')")
    suspend fun getTeacherName(): String
    
    /**
     * الحصول على اسم المدرسة
     */
    @Query("SELECT COALESCE((SELECT value FROM settings WHERE key = 'school_name' LIMIT 1), '')")
    suspend fun getSchoolName(): String
    
    /**
     * الحصول على مستوى الصوت
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'volume_level' LIMIT 1), 70)")
    suspend fun getVolumeLevel(): Int
    
    /**
     * الحصول على مدة الصوت
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'sound_duration' LIMIT 1), 5)")
    suspend fun getSoundDuration(): Int
    
    /**
     * الحصول على نمط الاهتزاز
     */
    @Query("SELECT COALESCE((SELECT value FROM settings WHERE key = 'vibration_mode' LIMIT 1), 'SOUND_WITH_VIBRATION')")
    suspend fun getVibrationMode(): String
    
    /**
     * الحصول على نطاق الموقع
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'location_radius' LIMIT 1), 100)")
    suspend fun getLocationRadius(): Int
    
    /**
     * التحقق من تفعيل الصوت
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'sound_enabled' LIMIT 1), 1)")
    suspend fun isSoundEnabled(): Boolean
    
    /**
     * التحقق من تفعيل الموقع
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'location_enabled' LIMIT 1), 1)")
    suspend fun isLocationEnabled(): Boolean
    
    /**
     * التحقق من تفعيل الإشعارات
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'notifications_enabled' LIMIT 1), 1)")
    suspend fun isNotificationsEnabled(): Boolean
    
    /**
     * التحقق من الوضع اليدوي
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'manual_mode' LIMIT 1), 0)")
    suspend fun isManualMode(): Boolean
    
    /**
     * التحقق من الوضع الليلي
     */
    @Query("SELECT COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'dark_mode' LIMIT 1), 0)")
    suspend fun isDarkMode(): Boolean
    
    /**
     * الحصول على إعدادات الصوت
     */
    @Query("""
        SELECT 
            COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'volume_level' LIMIT 1), 70) as volumeLevel,
            COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'sound_duration' LIMIT 1), 5) as soundDuration,
            COALESCE((SELECT value FROM settings WHERE key = 'vibration_mode' LIMIT 1), 'SOUND_WITH_VIBRATION') as vibrationMode,
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'sound_enabled' LIMIT 1), 1) as soundEnabled,
            COALESCE((SELECT value FROM settings WHERE key = 'ringtone_uri' LIMIT 1), '') as ringtoneUri
    """)
    suspend fun getSoundSettings(): SoundSettings
    
    /**
     * الحصول على إعدادات الموقع
     */
    @Query("""
        SELECT 
            COALESCE((SELECT CAST(value AS REAL) FROM settings WHERE key = 'school_latitude' LIMIT 1), 0.0) as latitude,
            COALESCE((SELECT CAST(value AS REAL) FROM settings WHERE key = 'school_longitude' LIMIT 1), 0.0) as longitude,
            COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'location_radius' LIMIT 1), 100) as radius,
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'location_enabled' LIMIT 1), 1) as enabled,
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'location_notifications' LIMIT 1), 1) as notifications
    """)
    suspend fun getLocationSettings(): LocationSettings
    
    /**
     * الحصول على إعدادات الإشعارات
     */
    @Query("""
        SELECT 
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'notifications_enabled' LIMIT 1), 1) as enabled,
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'start_notification' LIMIT 1), 1) as startNotification,
            COALESCE((SELECT CAST(value AS BOOLEAN) FROM settings WHERE key = 'end_notification' LIMIT 1), 1) as endNotification,
            COALESCE((SELECT CAST(value AS INTEGER) FROM settings WHERE key = 'reminder_minutes' LIMIT 1), 15) as reminderMinutes,
            COALESCE((SELECT value FROM settings WHERE key = 'notification_priority' LIMIT 1), 'HIGH') as priority
    """)
    suspend fun getNotificationSettings(): NotificationSettings
}

/**
 * إعدادات الصوت
 */
data class SoundSettings(
    val volumeLevel: Int,
    val soundDuration: Int,
    val vibrationMode: String,
    val soundEnabled: Boolean,
    val ringtoneUri: String
)

/**
 * إعدادات الموقع
 */
data class LocationSettings(
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val enabled: Boolean,
    val notifications: Boolean
)

/**
 * إعدادات الإشعارات
 */
data class NotificationSettings(
    val enabled: Boolean,
    val startNotification: Boolean,
    val endNotification: Boolean,
    val reminderMinutes: Int,
    val priority: String
)

