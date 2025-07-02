package com.mo.bell.data.models

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * نموذج بيانات الحصة الحالية
 */
data class CurrentClass(
    val scheduleId: Long,
    val day: String,
    val period: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val className: String?,
    val subjectName: String?,
    val teacherName: String,
    val timeRemaining: Long, // بالثواني
    val progressPercentage: Float // نسبة التقدم في الحصة (0-100)
) {
    /**
     * التحقق من أن الحصة ما زالت جارية
     */
    fun isStillActive(): Boolean {
        val now = LocalTime.now()
        return now.isAfter(startTime) && now.isBefore(endTime)
    }
    
    /**
     * الحصول على الوقت المتبقي بصيغة نصية
     */
    fun getFormattedTimeRemaining(): String {
        val hours = timeRemaining / 3600
        val minutes = (timeRemaining % 3600) / 60
        val seconds = timeRemaining % 60
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * الحصول على وصف حالة الحصة
     */
    fun getStatusDescription(): String {
        return when {
            timeRemaining <= 0 -> "انتهت الحصة"
            timeRemaining <= 300 -> "ستنتهي الحصة قريباً" // 5 دقائق
            timeRemaining <= 600 -> "أقل من 10 دقائق متبقية" // 10 دقائق
            else -> "الحصة جارية"
        }
    }
    
    /**
     * الحصول على لون الحالة
     */
    fun getStatusColor(): String {
        return when {
            timeRemaining <= 0 -> "#F44336" // أحمر
            timeRemaining <= 300 -> "#FF9800" // برتقالي
            timeRemaining <= 600 -> "#FFC107" // أصفر
            else -> "#4CAF50" // أخضر
        }
    }
}

/**
 * حالة الحصة الحالية
 */
enum class ClassStatus {
    NOT_STARTED,    // لم تبدأ بعد
    IN_PROGRESS,    // جارية
    ENDING_SOON,    // ستنتهي قريباً
    ENDED           // انتهت
}

/**
 * نموذج بيانات مبسط للحصة الحالية
 */
data class SimpleCurrentClass(
    val period: Int,
    val className: String?,
    val subjectName: String?,
    val startTime: String,
    val endTime: String,
    val timeRemaining: String,
    val status: ClassStatus
)

