package com.mo.bell.data.models

import com.mo.bell.data.database.entities.Schedule
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * نموذج بيانات الجدول الأسبوعي
 */
data class WeeklySchedule(
    val teacherName: String,
    val schoolName: String?,
    val days: Map<String, DaySchedule>
) {
    /**
     * الحصول على جدول يوم معين
     */
    fun getDaySchedule(dayName: String): DaySchedule? {
        return days[dayName]
    }
    
    /**
     * الحصول على جميع الحصص مرتبة حسب اليوم والوقت
     */
    fun getAllClassesSorted(): List<Schedule> {
        val allClasses = mutableListOf<Schedule>()
        
        // ترتيب أيام الأسبوع
        val dayOrder = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        
        dayOrder.forEach { day ->
            days[day]?.let { daySchedule ->
                allClasses.addAll(daySchedule.classes.sortedBy { it.period })
            }
        }
        
        return allClasses
    }
    
    /**
     * الحصول على إحصائيات الجدول
     */
    fun getStatistics(): ScheduleStatistics {
        val allClasses = getAllClassesSorted()
        val totalClasses = allClasses.size
        val daysWithClasses = days.values.count { it.classes.isNotEmpty() }
        val subjects = allClasses.mapNotNull { it.subjectName }.distinct()
        val classNames = allClasses.mapNotNull { it.className }.distinct()
        
        // حساب متوسط الحصص في اليوم
        val averageClassesPerDay = if (daysWithClasses > 0) {
            totalClasses.toFloat() / daysWithClasses
        } else 0f
        
        // العثور على أكثر يوم ازدحاماً
        val busiestDay = days.maxByOrNull { it.value.classes.size }?.key
        
        // حساب إجمالي ساعات التدريس
        val totalHours = allClasses.sumOf { schedule ->
            val start = LocalTime.parse(schedule.startTime)
            val end = LocalTime.parse(schedule.endTime)
            java.time.Duration.between(start, end).toMinutes()
        } / 60.0
        
        return ScheduleStatistics(
            totalClasses = totalClasses,
            daysWithClasses = daysWithClasses,
            subjects = subjects,
            classNames = classNames,
            averageClassesPerDay = averageClassesPerDay,
            busiestDay = busiestDay,
            totalTeachingHours = totalHours
        )
    }
}

/**
 * جدول يوم واحد
 */
data class DaySchedule(
    val dayName: String,
    val classes: List<Schedule>
) {
    /**
     * الحصول على الحصص مرتبة حسب الوقت
     */
    fun getClassesSortedByTime(): List<Schedule> {
        return classes.sortedBy { it.period }
    }
    
    /**
     * الحصول على الحصة في فترة معينة
     */
    fun getClassByPeriod(period: Int): Schedule? {
        return classes.find { it.period == period }
    }
    
    /**
     * التحقق من وجود حصص في هذا اليوم
     */
    fun hasClasses(): Boolean = classes.isNotEmpty()
    
    /**
     * الحصول على أول حصة في اليوم
     */
    fun getFirstClass(): Schedule? = classes.minByOrNull { it.period }
    
    /**
     * الحصول على آخر حصة في اليوم
     */
    fun getLastClass(): Schedule? = classes.maxByOrNull { it.period }
    
    /**
     * الحصول على الفجوات بين الحصص
     */
    fun getBreaks(): List<BreakPeriod> {
        val sortedClasses = getClassesSortedByTime()
        val breaks = mutableListOf<BreakPeriod>()
        
        for (i in 0 until sortedClasses.size - 1) {
            val currentClass = sortedClasses[i]
            val nextClass = sortedClasses[i + 1]
            
            val currentEnd = LocalTime.parse(currentClass.endTime)
            val nextStart = LocalTime.parse(nextClass.startTime)
            
            if (currentEnd.isBefore(nextStart)) {
                breaks.add(
                    BreakPeriod(
                        startTime = currentEnd,
                        endTime = nextStart,
                        duration = java.time.Duration.between(currentEnd, nextStart).toMinutes()
                    )
                )
            }
        }
        
        return breaks
    }
}

/**
 * فترة استراحة بين الحصص
 */
data class BreakPeriod(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val duration: Long // بالدقائق
) {
    fun getFormattedDuration(): String {
        val hours = duration / 60
        val minutes = duration % 60
        
        return when {
            hours > 0 -> "${hours}س ${minutes}د"
            else -> "${minutes}د"
        }
    }
}

/**
 * إحصائيات الجدول
 */
data class ScheduleStatistics(
    val totalClasses: Int,
    val daysWithClasses: Int,
    val subjects: List<String>,
    val classNames: List<String>,
    val averageClassesPerDay: Float,
    val busiestDay: String?,
    val totalTeachingHours: Double
)

/**
 * نموذج مبسط لعرض الجدول
 */
data class SimpleWeeklySchedule(
    val teacherName: String,
    val schoolName: String?,
    val totalClasses: Int,
    val daysWithClasses: Int,
    val subjects: List<String>
)

