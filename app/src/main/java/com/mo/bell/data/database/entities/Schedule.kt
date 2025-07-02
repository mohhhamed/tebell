package com.mo.bell.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * كيان الجدول في قاعدة البيانات
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Teacher::class,
            parentColumns = ["id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["teacher_id"]),
        Index(value = ["day", "period"]),
        Index(value = ["start_time", "end_time"])
    ]
)
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "teacher_id")
    val teacherId: Long,
    
    @ColumnInfo(name = "day")
    val day: String,
    
    @ColumnInfo(name = "period")
    val period: Int,
    
    @ColumnInfo(name = "start_time")
    val startTime: String, // تنسيق HH:mm
    
    @ColumnInfo(name = "end_time")
    val endTime: String, // تنسيق HH:mm
    
    @ColumnInfo(name = "class_name")
    val className: String? = null,
    
    @ColumnInfo(name = "subject_name")
    val subjectName: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = LocalDateTime.now().toString(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = LocalDateTime.now().toString()
) {
    /**
     * الحصول على وقت البداية كـ LocalTime
     */
    fun getStartTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    /**
     * الحصول على وقت النهاية كـ LocalTime
     */
    fun getEndTimeAsLocalTime(): LocalTime {
        return LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    /**
     * حساب مدة الحصة بالدقائق
     */
    fun getDurationInMinutes(): Long {
        val start = getStartTimeAsLocalTime()
        val end = getEndTimeAsLocalTime()
        return java.time.Duration.between(start, end).toMinutes()
    }
    
    /**
     * الحصول على مدة الحصة بصيغة نصية
     */
    fun getFormattedDuration(): String {
        val duration = getDurationInMinutes()
        val hours = duration / 60
        val minutes = duration % 60
        
        return when {
            hours > 0 -> "${hours}س ${minutes}د"
            else -> "${minutes}د"
        }
    }
    
    /**
     * التحقق من أن الحصة جارية الآن
     */
    fun isCurrentlyActive(): Boolean {
        val now = LocalTime.now()
        val start = getStartTimeAsLocalTime()
        val end = getEndTimeAsLocalTime()
        
        return now.isAfter(start) && now.isBefore(end)
    }
    
    /**
     * التحقق من أن الحصة ستبدأ قريباً
     */
    fun isStartingSoon(minutesBefore: Int = 15): Boolean {
        val now = LocalTime.now()
        val start = getStartTimeAsLocalTime()
        val warningTime = start.minusMinutes(minutesBefore.toLong())
        
        return now.isAfter(warningTime) && now.isBefore(start)
    }
    
    /**
     * الحصول على وصف الحصة
     */
    fun getDescription(): String {
        return buildString {
            append("الحصة $period")
            className?.let { append(" - $it") }
            subjectName?.let { append(" ($it)") }
        }
    }
    
    /**
     * الحصول على وصف الوقت
     */
    fun getTimeDescription(): String {
        return "$startTime - $endTime"
    }
    
    /**
     * التحقق من تداخل الحصة مع حصة أخرى
     */
    fun overlapsWith(other: Schedule): Boolean {
        if (day != other.day) return false
        
        val thisStart = getStartTimeAsLocalTime()
        val thisEnd = getEndTimeAsLocalTime()
        val otherStart = other.getStartTimeAsLocalTime()
        val otherEnd = other.getEndTimeAsLocalTime()
        
        return thisStart.isBefore(otherEnd) && otherStart.isBefore(thisEnd)
    }
    
    /**
     * التحقق من صحة بيانات الحصة
     */
    fun isValid(): Boolean {
        return try {
            val start = getStartTimeAsLocalTime()
            val end = getEndTimeAsLocalTime()
            
            day.isNotBlank() &&
            period > 0 &&
            start.isBefore(end) &&
            getDurationInMinutes() >= 5 // الحد الأدنى 5 دقائق
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * إنشاء نسخة محدثة من الحصة
     */
    fun updated(
        day: String = this.day,
        period: Int = this.period,
        startTime: String = this.startTime,
        endTime: String = this.endTime,
        className: String? = this.className,
        subjectName: String? = this.subjectName,
        isActive: Boolean = this.isActive
    ): Schedule {
        return copy(
            day = day,
            period = period,
            startTime = startTime,
            endTime = endTime,
            className = className,
            subjectName = subjectName,
            isActive = isActive,
            updatedAt = LocalDateTime.now().toString()
        )
    }
    
    companion object {
        /**
         * إنشاء حصة جديدة
         */
        fun create(
            teacherId: Long,
            day: String,
            period: Int,
            startTime: String,
            endTime: String,
            className: String? = null,
            subjectName: String? = null
        ): Schedule {
            return Schedule(
                teacherId = teacherId,
                day = day.trim(),
                period = period,
                startTime = startTime.trim(),
                endTime = endTime.trim(),
                className = className?.trim(),
                subjectName = subjectName?.trim()
            )
        }
        
        /**
         * أيام الأسبوع المدعومة
         */
        val SUPPORTED_DAYS = listOf(
            "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", 
            "الخميس", "الجمعة", "السبت"
        )
        
        /**
         * التحقق من صحة اسم اليوم
         */
        fun isValidDay(day: String): Boolean {
            return SUPPORTED_DAYS.contains(day)
        }
        
        /**
         * تحويل اسم اليوم من الإنجليزية للعربية
         */
        fun translateDayToArabic(englishDay: String): String {
            return when (englishDay.lowercase()) {
                "sunday" -> "الأحد"
                "monday" -> "الاثنين"
                "tuesday" -> "الثلاثاء"
                "wednesday" -> "الأربعاء"
                "thursday" -> "الخميس"
                "friday" -> "الجمعة"
                "saturday" -> "السبت"
                else -> englishDay
            }
        }
    }
}

