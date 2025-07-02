package com.mo.bell.utils

import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeUtils @Inject constructor() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    /**
     * الحصول على يوم الأسبوع الحالي
     */
    fun getCurrentDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }
    }

    /**
     * تحويل النص إلى وقت
     */
    fun parseTime(timeString: String): Calendar {
        val calendar = Calendar.getInstance()
        try {
            val time = timeFormat.parse(timeString)
            time?.let {
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = it
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            // في حالة الخطأ، إرجاع الوقت الحالي
        }
        return calendar
    }

    /**
     * تنسيق الوقت للعرض
     */
    fun formatTime(timeString: String): String {
        return try {
            val time = timeFormat.parse(timeString)
            time?.let { displayTimeFormat.format(it) } ?: timeString
        } catch (e: Exception) {
            timeString
        }
    }

    /**
     * تنسيق الوقت للعرض من Calendar
     */
    fun formatTime(calendar: Calendar): String {
        return displayTimeFormat.format(calendar.time)
    }

    /**
     * فحص ما إذا كان الوقت الحالي ضمن نطاق معين
     */
    fun isTimeInRange(currentTime: Calendar, startTime: String, endTime: String): Boolean {
        val start = parseTime(startTime)
        val end = parseTime(endTime)
        
        // تعيين نفس التاريخ لجميع الأوقات للمقارنة
        val current = Calendar.getInstance()
        current.set(Calendar.YEAR, start.get(Calendar.YEAR))
        current.set(Calendar.MONTH, start.get(Calendar.MONTH))
        current.set(Calendar.DAY_OF_MONTH, start.get(Calendar.DAY_OF_MONTH))
        current.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
        current.set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
        current.set(Calendar.SECOND, 0)
        current.set(Calendar.MILLISECOND, 0)

        return current.timeInMillis >= start.timeInMillis && current.timeInMillis <= end.timeInMillis
    }

    /**
     * فحص ما إذا كان الوقت بعد الوقت الحالي
     */
    fun isAfterCurrentTime(timeString: String): Boolean {
        val currentTime = Calendar.getInstance()
        val targetTime = parseTime(timeString)
        
        // تعيين نفس التاريخ للمقارنة
        targetTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
        targetTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
        targetTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

        return targetTime.timeInMillis > currentTime.timeInMillis
    }

    /**
     * فحص ما إذا كان الوقت الأول بعد الوقت الثاني
     */
    fun isAfterTime(time1: String, time2: String): Boolean {
        val t1 = parseTime(time1)
        val t2 = parseTime(time2)
        return t1.timeInMillis > t2.timeInMillis
    }

    /**
     * حساب نسبة التقدم في الحصة
     */
    fun calculateClassProgress(currentTime: Calendar, startTime: String, endTime: String): Int {
        val start = parseTime(startTime)
        val end = parseTime(endTime)
        
        val current = Calendar.getInstance()
        current.set(Calendar.YEAR, start.get(Calendar.YEAR))
        current.set(Calendar.MONTH, start.get(Calendar.MONTH))
        current.set(Calendar.DAY_OF_MONTH, start.get(Calendar.DAY_OF_MONTH))
        current.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
        current.set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
        current.set(Calendar.SECOND, 0)
        current.set(Calendar.MILLISECOND, 0)

        val totalDuration = end.timeInMillis - start.timeInMillis
        val elapsed = current.timeInMillis - start.timeInMillis

        return if (totalDuration > 0) {
            ((elapsed.toDouble() / totalDuration.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    /**
     * حساب الدقائق المتبقية حتى وقت معين
     */
    fun calculateRemainingMinutes(currentTime: Calendar, endTime: String): Int {
        val end = parseTime(endTime)
        
        val current = Calendar.getInstance()
        current.set(Calendar.YEAR, end.get(Calendar.YEAR))
        current.set(Calendar.MONTH, end.get(Calendar.MONTH))
        current.set(Calendar.DAY_OF_MONTH, end.get(Calendar.DAY_OF_MONTH))
        current.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
        current.set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
        current.set(Calendar.SECOND, 0)
        current.set(Calendar.MILLISECOND, 0)

        val remainingMillis = end.timeInMillis - current.timeInMillis
        return (remainingMillis / (1000 * 60)).toInt().coerceAtLeast(0)
    }

    /**
     * حساب الدقائق حتى وقت معين
     */
    fun calculateMinutesUntil(currentTime: Calendar, targetTime: String): Int {
        val target = parseTime(targetTime)
        
        val current = Calendar.getInstance()
        current.set(Calendar.YEAR, target.get(Calendar.YEAR))
        current.set(Calendar.MONTH, target.get(Calendar.MONTH))
        current.set(Calendar.DAY_OF_MONTH, target.get(Calendar.DAY_OF_MONTH))
        current.set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
        current.set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
        current.set(Calendar.SECOND, 0)
        current.set(Calendar.MILLISECOND, 0)

        val diffMillis = target.timeInMillis - current.timeInMillis
        return (diffMillis / (1000 * 60)).toInt().coerceAtLeast(0)
    }

    /**
     * حساب مدة الحصة بالدقائق
     */
    fun calculateClassDuration(startTime: String, endTime: String): Int {
        val start = parseTime(startTime)
        val end = parseTime(endTime)
        
        val durationMillis = end.timeInMillis - start.timeInMillis
        return (durationMillis / (1000 * 60)).toInt()
    }

    /**
     * تحويل الدقائق إلى نص مقروء
     */
    fun formatDuration(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes دقيقة"
            minutes == 60 -> "ساعة واحدة"
            minutes < 120 -> "ساعة و${minutes - 60} دقيقة"
            else -> {
                val hours = minutes / 60
                val remainingMinutes = minutes % 60
                if (remainingMinutes == 0) {
                    "$hours ساعات"
                } else {
                    "$hours ساعات و$remainingMinutes دقيقة"
                }
            }
        }
    }

    /**
     * تحويل رقم اليوم إلى اسم اليوم
     */
    fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> "الأحد"
            1 -> "الاثنين"
            2 -> "الثلاثاء"
            3 -> "الأربعاء"
            4 -> "الخميس"
            5 -> "الجمعة"
            6 -> "السبت"
            else -> "غير محدد"
        }
    }

    /**
     * فحص ما إذا كان اليوم يوم عمل
     */
    fun isWorkDay(dayOfWeek: Int): Boolean {
        // أيام العمل من الأحد إلى الخميس (0-4)
        return dayOfWeek in 0..4
    }

    /**
     * الحصول على الوقت الحالي كنص
     */
    fun getCurrentTimeString(): String {
        return timeFormat.format(Date())
    }

    /**
     * الحصول على التاريخ الحالي كنص
     */
    fun getCurrentDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

