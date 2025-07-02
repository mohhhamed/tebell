package com.mo.bell.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * كيان الإعدادات في قاعدة البيانات
 */
@Entity(
    tableName = "settings",
    indices = [Index(value = ["key"], unique = true)]
)
data class Settings(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "key")
    val key: String,
    
    @ColumnInfo(name = "value")
    val value: String,
    
    @ColumnInfo(name = "type")
    val type: SettingType = SettingType.STRING,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = LocalDateTime.now().toString()
) {
    /**
     * الحصول على القيمة كـ String
     */
    fun getStringValue(): String = value
    
    /**
     * الحصول على القيمة كـ Int
     */
    fun getIntValue(): Int? = value.toIntOrNull()
    
    /**
     * الحصول على القيمة كـ Long
     */
    fun getLongValue(): Long? = value.toLongOrNull()
    
    /**
     * الحصول على القيمة كـ Float
     */
    fun getFloatValue(): Float? = value.toFloatOrNull()
    
    /**
     * الحصول على القيمة كـ Boolean
     */
    fun getBooleanValue(): Boolean = value.toBoolean()
    
    /**
     * إنشاء نسخة محدثة من الإعداد
     */
    fun updated(newValue: String): Settings {
        return copy(
            value = newValue,
            updatedAt = LocalDateTime.now().toString()
        )
    }
    
    companion object {
        /**
         * إنشاء إعداد جديد
         */
        fun create(
            key: String,
            value: String,
            type: SettingType = SettingType.STRING,
            description: String? = null
        ): Settings {
            return Settings(
                key = key,
                value = value,
                type = type,
                description = description
            )
        }
        
        // مفاتيح الإعدادات الثابتة
        object Keys {
            // إعدادات المدرس
            const val TEACHER_NAME = "teacher_name"
            const val SCHOOL_NAME = "school_name"
            
            // إعدادات الصوت
            const val RINGTONE_URI = "ringtone_uri"
            const val VOLUME_LEVEL = "volume_level"
            const val SOUND_DURATION = "sound_duration"
            const val VIBRATION_MODE = "vibration_mode"
            const val SOUND_ENABLED = "sound_enabled"
            
            // إعدادات الموقع
            const val SCHOOL_LATITUDE = "school_latitude"
            const val SCHOOL_LONGITUDE = "school_longitude"
            const val LOCATION_RADIUS = "location_radius"
            const val LOCATION_ENABLED = "location_enabled"
            const val LOCATION_NOTIFICATIONS = "location_notifications"
            
            // إعدادات التنبيهات
            const val NOTIFICATIONS_ENABLED = "notifications_enabled"
            const val START_NOTIFICATION = "start_notification"
            const val END_NOTIFICATION = "end_notification"
            const val REMINDER_MINUTES = "reminder_minutes"
            
            // إعدادات التطبيق
            const val MANUAL_MODE = "manual_mode"
            const val DARK_MODE = "dark_mode"
            const val AUTO_START = "auto_start"
            const val BACKGROUND_SERVICE = "background_service"
            
            // إعدادات متقدمة
            const val BATTERY_OPTIMIZATION = "battery_optimization"
            const val NOTIFICATION_PRIORITY = "notification_priority"
            const val LANGUAGE = "language"
            const val FIRST_RUN = "first_run"
            const val APP_VERSION = "app_version"
        }
        
        // القيم الافتراضية
        object Defaults {
            const val VOLUME_LEVEL = "70"
            const val SOUND_DURATION = "5"
            const val VIBRATION_MODE = "SOUND_WITH_VIBRATION"
            const val LOCATION_RADIUS = "100"
            const val REMINDER_MINUTES = "15"
            const val NOTIFICATION_PRIORITY = "HIGH"
            const val LANGUAGE = "ar"
            
            val SOUND_ENABLED = true.toString()
            val LOCATION_ENABLED = true.toString()
            val NOTIFICATIONS_ENABLED = true.toString()
            val START_NOTIFICATION = true.toString()
            val END_NOTIFICATION = true.toString()
            val LOCATION_NOTIFICATIONS = true.toString()
            val MANUAL_MODE = false.toString()
            val DARK_MODE = false.toString()
            val AUTO_START = true.toString()
            val BACKGROUND_SERVICE = true.toString()
            val BATTERY_OPTIMIZATION = false.toString()
            val FIRST_RUN = true.toString()
        }
        
        /**
         * إنشاء الإعدادات الافتراضية
         */
        fun createDefaults(): List<Settings> {
            return listOf(
                // إعدادات الصوت
                create(Keys.VOLUME_LEVEL, Defaults.VOLUME_LEVEL, SettingType.INT, "مستوى الصوت"),
                create(Keys.SOUND_DURATION, Defaults.SOUND_DURATION, SettingType.INT, "مدة الصوت بالثواني"),
                create(Keys.VIBRATION_MODE, Defaults.VIBRATION_MODE, SettingType.STRING, "نمط الاهتزاز"),
                create(Keys.SOUND_ENABLED, Defaults.SOUND_ENABLED, SettingType.BOOLEAN, "تفعيل الصوت"),
                
                // إعدادات الموقع
                create(Keys.LOCATION_RADIUS, Defaults.LOCATION_RADIUS, SettingType.INT, "نطاق الموقع بالمتر"),
                create(Keys.LOCATION_ENABLED, Defaults.LOCATION_ENABLED, SettingType.BOOLEAN, "تفعيل الموقع"),
                create(Keys.LOCATION_NOTIFICATIONS, Defaults.LOCATION_NOTIFICATIONS, SettingType.BOOLEAN, "إشعارات الموقع"),
                
                // إعدادات التنبيهات
                create(Keys.NOTIFICATIONS_ENABLED, Defaults.NOTIFICATIONS_ENABLED, SettingType.BOOLEAN, "تفعيل الإشعارات"),
                create(Keys.START_NOTIFICATION, Defaults.START_NOTIFICATION, SettingType.BOOLEAN, "إشعار بداية الحصة"),
                create(Keys.END_NOTIFICATION, Defaults.END_NOTIFICATION, SettingType.BOOLEAN, "إشعار نهاية الحصة"),
                create(Keys.REMINDER_MINUTES, Defaults.REMINDER_MINUTES, SettingType.INT, "دقائق التذكير"),
                
                // إعدادات التطبيق
                create(Keys.MANUAL_MODE, Defaults.MANUAL_MODE, SettingType.BOOLEAN, "الوضع اليدوي"),
                create(Keys.DARK_MODE, Defaults.DARK_MODE, SettingType.BOOLEAN, "الوضع الليلي"),
                create(Keys.AUTO_START, Defaults.AUTO_START, SettingType.BOOLEAN, "التشغيل التلقائي"),
                create(Keys.BACKGROUND_SERVICE, Defaults.BACKGROUND_SERVICE, SettingType.BOOLEAN, "الخدمة في الخلفية"),
                
                // إعدادات متقدمة
                create(Keys.BATTERY_OPTIMIZATION, Defaults.BATTERY_OPTIMIZATION, SettingType.BOOLEAN, "تحسين البطارية"),
                create(Keys.NOTIFICATION_PRIORITY, Defaults.NOTIFICATION_PRIORITY, SettingType.STRING, "أولوية الإشعارات"),
                create(Keys.LANGUAGE, Defaults.LANGUAGE, SettingType.STRING, "اللغة"),
                create(Keys.FIRST_RUN, Defaults.FIRST_RUN, SettingType.BOOLEAN, "التشغيل الأول")
            )
        }
    }
}

/**
 * أنواع الإعدادات
 */
enum class SettingType {
    STRING,
    INT,
    LONG,
    FLOAT,
    BOOLEAN
}

/**
 * أنماط الاهتزاز
 */
enum class VibrationMode {
    SOUND_WITH_VIBRATION,  // صوت مع اهتزاز
    SOUND_ONLY,            // صوت فقط
    VIBRATION_ONLY,        // اهتزاز فقط
    SILENT                 // صامت
}

/**
 * أولوية الإشعارات
 */
enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

