package com.mo.bell.data.models

import java.time.LocalTime

/**
 * نموذج بيانات الحصة القادمة
 */
data class NextClass(
    val scheduleId: Long,
    val day: String,
    val period: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val className: String?,
    val subjectName: String?,
    val teacherName: String,
    val timeUntilStart: Long, // بالثواني حتى بداية الحصة
    val isToday: Boolean, // هل الحصة اليوم أم في يوم آخر
    val dayName: String // اسم اليوم بالعربية
) {
    /**
     * الحصول على الوقت المتبقي حتى بداية الحصة بصيغة نصية
     */
    fun getFormattedTimeUntilStart(): String {
        val hours = timeUntilStart / 3600
        val minutes = (timeUntilStart % 3600) / 60
        val seconds = timeUntilStart % 60
        
        return when {
            hours > 24 -> {
                val days = hours / 24
                val remainingHours = hours % 24
                "${days}د ${remainingHours}س"
            }
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * الحصول على وصف الوقت المتبقي
     */
    fun getTimeDescription(): String {
        return when {
            !isToday -> "غداً في $dayName"
            timeUntilStart <= 0 -> "الآن"
            timeUntilStart <= 300 -> "خلال ${timeUntilStart / 60} دقائق" // 5 دقائق
            timeUntilStart <= 3600 -> "خلال ${timeUntilStart / 60} دقيقة" // ساعة
            timeUntilStart <= 7200 -> "خلال ساعة و${(timeUntilStart % 3600) / 60} دقيقة" // ساعتين
            else -> "في ${getFormattedTimeUntilStart()}"
        }
    }
    
    /**
     * الحصول على أولوية التنبيه
     */
    fun getNotificationPriority(): NotificationPriority {
        return when {
            timeUntilStart <= 0 -> NotificationPriority.URGENT
            timeUntilStart <= 300 -> NotificationPriority.HIGH // 5 دقائق
            timeUntilStart <= 900 -> NotificationPriority.MEDIUM // 15 دقيقة
            timeUntilStart <= 1800 -> NotificationPriority.LOW // 30 دقيقة
            else -> NotificationPriority.NONE
        }
    }
    
    /**
     * التحقق من ضرورة إرسال تنبيه
     */
    fun shouldNotify(): Boolean {
        return isToday && getNotificationPriority() != NotificationPriority.NONE
    }
    
    /**
     * الحصول على رسالة التنبيه
     */
    fun getNotificationMessage(): String {
        val classInfo = buildString {
            append("الحصة ${period}")
            className?.let { append(" - $it") }
            subjectName?.let { append(" ($it)") }
        }
        
        return when {
            timeUntilStart <= 0 -> "بدأت $classInfo الآن"
            timeUntilStart <= 300 -> "ستبدأ $classInfo خلال ${timeUntilStart / 60} دقائق"
            timeUntilStart <= 900 -> "تذكير: $classInfo خلال ${timeUntilStart / 60} دقيقة"
            else -> "الحصة القادمة: $classInfo"
        }
    }
}

/**
 * أولوية التنبيه
 */
enum class NotificationPriority {
    NONE,    // لا حاجة لتنبيه
    LOW,     // تنبيه منخفض
    MEDIUM,  // تنبيه متوسط
    HIGH,    // تنبيه عالي
    URGENT   // تنبيه عاجل
}

/**
 * نموذج بيانات مبسط للحصة القادمة
 */
data class SimpleNextClass(
    val period: Int,
    val className: String?,
    val subjectName: String?,
    val startTime: String,
    val timeUntilStart: String,
    val dayName: String,
    val isToday: Boolean
)

