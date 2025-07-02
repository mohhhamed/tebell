package com.mo.bell.data.repository

import com.mo.bell.data.database.dao.ScheduleDao
import com.mo.bell.data.database.dao.TeacherDao
import com.mo.bell.data.database.entities.Schedule
import com.mo.bell.data.database.entities.Teacher
import com.mo.bell.data.models.ScheduleJson
import com.mo.bell.data.models.WeeklySchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val teacherDao: TeacherDao
) {

    /**
     * الحصول على جدول يوم معين
     */
    suspend fun getScheduleForDay(dayOfWeek: Int): List<Schedule> {
        return scheduleDao.getScheduleForDay(dayOfWeek)
    }

    /**
     * الحصول على الجدول الأسبوعي كاملاً
     */
    suspend fun getWeeklySchedule(): WeeklySchedule {
        val scheduleMap = mutableMapOf<Int, List<Schedule>>()
        
        for (day in 0..6) {
            scheduleMap[day] = scheduleDao.getScheduleForDay(day)
        }
        
        val teacher = teacherDao.getCurrentTeacher()
        
        return WeeklySchedule(
            teacherName = teacher?.name ?: "",
            schoolName = teacher?.schoolName ?: "",
            schedule = scheduleMap
        )
    }

    /**
     * الحصول على جدول يوم معين كـ Flow
     */
    fun getScheduleForDayFlow(dayOfWeek: Int): Flow<List<Schedule>> {
        return scheduleDao.getScheduleForDayFlow(dayOfWeek)
    }

    /**
     * الحصول على جميع الجداول
     */
    fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules()
    }

    /**
     * إضافة حصة جديدة
     */
    suspend fun insertSchedule(schedule: Schedule): Long {
        return scheduleDao.insertSchedule(schedule)
    }

    /**
     * تحديث حصة موجودة
     */
    suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.updateSchedule(schedule)
    }

    /**
     * حذف حصة
     */
    suspend fun deleteSchedule(schedule: Schedule) {
        scheduleDao.deleteSchedule(schedule)
    }

    /**
     * حذف جميع حصص يوم معين
     */
    suspend fun deleteSchedulesForDay(dayOfWeek: Int) {
        scheduleDao.deleteSchedulesForDay(dayOfWeek)
    }

    /**
     * حذف جميع الجداول
     */
    suspend fun deleteAllSchedules() {
        scheduleDao.deleteAllSchedules()
    }

    /**
     * استيراد جدول من JSON
     */
    suspend fun importSchedule(scheduleJson: ScheduleJson) {
        // حذف الجداول الموجودة
        deleteAllSchedules()
        
        // تحديث معلومات المدرس
        val teacher = Teacher(
            id = 1,
            name = scheduleJson.teacherName,
            schoolName = scheduleJson.schoolName
        )
        teacherDao.insertOrUpdateTeacher(teacher)
        
        // إدراج الجداول الجديدة
        scheduleJson.schedule.forEach { (dayKey, classes) ->
            val dayOfWeek = dayKey.toIntOrNull() ?: return@forEach
            
            classes.forEach { classItem ->
                val schedule = Schedule(
                    dayOfWeek = dayOfWeek,
                    periodNumber = classItem.periodNumber,
                    startTime = classItem.startTime,
                    endTime = classItem.endTime,
                    className = classItem.className,
                    subjectName = classItem.subjectName
                )
                insertSchedule(schedule)
            }
        }
    }

    /**
     * تصدير الجدول إلى JSON
     */
    suspend fun exportSchedule(): ScheduleJson {
        val teacher = teacherDao.getCurrentTeacher()
        val scheduleMap = mutableMapOf<String, List<ScheduleJson.ClassItem>>()
        
        for (day in 0..6) {
            val daySchedule = getScheduleForDay(day)
            if (daySchedule.isNotEmpty()) {
                val classItems = daySchedule.map { schedule ->
                    ScheduleJson.ClassItem(
                        periodNumber = schedule.periodNumber,
                        startTime = schedule.startTime,
                        endTime = schedule.endTime,
                        className = schedule.className,
                        subjectName = schedule.subjectName
                    )
                }
                scheduleMap[day.toString()] = classItems
            }
        }
        
        return ScheduleJson(
            teacherName = teacher?.name ?: "",
            schoolName = teacher?.schoolName ?: "",
            schedule = scheduleMap
        )
    }

    /**
     * البحث في الجداول
     */
    suspend fun searchSchedules(query: String): List<Schedule> {
        return scheduleDao.searchSchedules("%$query%")
    }

    /**
     * الحصول على الحصة الحالية
     */
    suspend fun getCurrentClass(dayOfWeek: Int, currentTime: String): Schedule? {
        return scheduleDao.getCurrentClass(dayOfWeek, currentTime)
    }

    /**
     * الحصول على الحصة القادمة
     */
    suspend fun getNextClass(dayOfWeek: Int, currentTime: String): Schedule? {
        return scheduleDao.getNextClass(dayOfWeek, currentTime)
    }

    /**
     * فحص وجود تضارب في الأوقات
     */
    suspend fun hasTimeConflict(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        excludeId: Long? = null
    ): Boolean {
        return scheduleDao.hasTimeConflict(dayOfWeek, startTime, endTime, excludeId)
    }

    /**
     * الحصول على إحصائيات الجدول
     */
    suspend fun getScheduleStatistics(): ScheduleStatistics {
        val totalClasses = scheduleDao.getTotalClassesCount()
        val classesByDay = mutableMapOf<Int, Int>()
        
        for (day in 0..6) {
            classesByDay[day] = scheduleDao.getClassesCountForDay(day)
        }
        
        return ScheduleStatistics(
            totalClasses = totalClasses,
            classesByDay = classesByDay
        )
    }
}

/**
 * إحصائيات الجدول
 */
data class ScheduleStatistics(
    val totalClasses: Int,
    val classesByDay: Map<Int, Int>
)

