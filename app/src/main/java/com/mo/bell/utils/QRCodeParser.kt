package com.mo.bell.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mo.bell.data.models.CompactScheduleJson
import com.mo.bell.data.models.ScheduleJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRCodeParser @Inject constructor(
    private val gson: Gson
) {

    /**
     * تحليل رمز QR واستخراج بيانات الجدول
     */
    fun parseQRCode(qrCodeData: String): ScheduleJson? {
        return try {
            // محاولة تحليل البيانات كجدول مضغوط أولاً
            val compactSchedule = parseCompactSchedule(qrCodeData)
            if (compactSchedule != null) {
                convertCompactToFull(compactSchedule)
            } else {
                // محاولة تحليل البيانات كجدول كامل
                parseFullSchedule(qrCodeData)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * تحليل الجدول المضغوط
     */
    private fun parseCompactSchedule(data: String): CompactScheduleJson? {
        return try {
            gson.fromJson(data, CompactScheduleJson::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * تحليل الجدول الكامل
     */
    private fun parseFullSchedule(data: String): ScheduleJson? {
        return try {
            gson.fromJson(data, ScheduleJson::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * تحويل الجدول المضغوط إلى جدول كامل
     */
    private fun convertCompactToFull(compact: CompactScheduleJson): ScheduleJson {
        val fullSchedule = ScheduleJson(
            teacherName = compact.teacherName,
            schoolName = compact.schoolName,
            schedule = mutableMapOf()
        )

        // تحويل البيانات المضغوطة
        compact.schedule.forEach { (dayKey, daySchedule) ->
            val dayOfWeek = dayKey.toIntOrNull() ?: return@forEach
            val classes = mutableListOf<ScheduleJson.ClassItem>()

            daySchedule.forEach { compactClass ->
                // تحليل البيانات المضغوطة
                val parts = compactClass.split("|")
                if (parts.size >= 4) {
                    val periodNumber = parts[0].toIntOrNull() ?: 0
                    val startTime = parts[1]
                    val endTime = parts[2]
                    val className = parts[3]
                    val subjectName = if (parts.size > 4) parts[4] else ""

                    classes.add(
                        ScheduleJson.ClassItem(
                            periodNumber = periodNumber,
                            startTime = startTime,
                            endTime = endTime,
                            className = className,
                            subjectName = subjectName
                        )
                    )
                }
            }

            fullSchedule.schedule[dayOfWeek.toString()] = classes
        }

        return fullSchedule
    }

    /**
     * التحقق من صحة بيانات الجدول
     */
    fun validateScheduleData(schedule: ScheduleJson): Boolean {
        try {
            // التحقق من وجود اسم المدرس
            if (schedule.teacherName.isBlank()) {
                return false
            }

            // التحقق من وجود جدول
            if (schedule.schedule.isEmpty()) {
                return false
            }

            // التحقق من صحة بيانات كل يوم
            schedule.schedule.forEach { (dayKey, classes) ->
                val dayOfWeek = dayKey.toIntOrNull()
                if (dayOfWeek == null || dayOfWeek !in 0..6) {
                    return false
                }

                classes.forEach { classItem ->
                    // التحقق من صحة الأوقات
                    if (!isValidTimeFormat(classItem.startTime) || 
                        !isValidTimeFormat(classItem.endTime)) {
                        return false
                    }

                    // التحقق من أن وقت البداية قبل وقت النهاية
                    if (!isStartTimeBeforeEndTime(classItem.startTime, classItem.endTime)) {
                        return false
                    }

                    // التحقق من وجود اسم الصف
                    if (classItem.className.isBlank()) {
                        return false
                    }
                }
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * التحقق من صحة تنسيق الوقت
     */
    private fun isValidTimeFormat(time: String): Boolean {
        val timeRegex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        return timeRegex.matches(time)
    }

    /**
     * التحقق من أن وقت البداية قبل وقت النهاية
     */
    private fun isStartTimeBeforeEndTime(startTime: String, endTime: String): Boolean {
        try {
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            
            val startHour = startParts[0].toInt()
            val startMinute = startParts[1].toInt()
            val endHour = endParts[0].toInt()
            val endMinute = endParts[1].toInt()
            
            val startTotalMinutes = startHour * 60 + startMinute
            val endTotalMinutes = endHour * 60 + endMinute
            
            return startTotalMinutes < endTotalMinutes
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * إنشاء رمز QR مضغوط من الجدول الكامل
     */
    fun generateCompactQRData(schedule: ScheduleJson): String {
        val compactSchedule = CompactScheduleJson(
            teacherName = schedule.teacherName,
            schoolName = schedule.schoolName,
            schedule = mutableMapOf()
        )

        schedule.schedule.forEach { (dayKey, classes) ->
            val compactClasses = classes.map { classItem ->
                "${classItem.periodNumber}|${classItem.startTime}|${classItem.endTime}|${classItem.className}|${classItem.subjectName}"
            }
            compactSchedule.schedule[dayKey] = compactClasses
        }

        return gson.toJson(compactSchedule)
    }

    /**
     * إنشاء رمز QR كامل من الجدول
     */
    fun generateFullQRData(schedule: ScheduleJson): String {
        return gson.toJson(schedule)
    }
}

