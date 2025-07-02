package com.mo.bell.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mo.bell.data.database.entities.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الوصول لبيانات الجداول
 */
@Dao
interface ScheduleDao {
    
    /**
     * إدراج حصة جديدة
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long
    
    /**
     * إدراج عدة حصص
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>): List<Long>
    
    /**
     * تحديث حصة
     */
    @Update
    suspend fun updateSchedule(schedule: Schedule): Int
    
    /**
     * حذف حصة
     */
    @Delete
    suspend fun deleteSchedule(schedule: Schedule): Int
    
    /**
     * حذف حصة بالمعرف
     */
    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: Long): Int
    
    /**
     * حذف جميع حصص مدرس
     */
    @Query("DELETE FROM schedules WHERE teacher_id = :teacherId")
    suspend fun deleteSchedulesByTeacher(teacherId: Long): Int
    
    /**
     * حذف جميع الحصص
     */
    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules(): Int
    
    /**
     * الحصول على حصة بالمعرف
     */
    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getScheduleById(scheduleId: Long): Schedule?
    
    /**
     * الحصول على حصة بالمعرف كـ LiveData
     */
    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    fun getScheduleByIdLiveData(scheduleId: Long): LiveData<Schedule?>
    
    /**
     * الحصول على جميع حصص مدرس
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND is_active = 1 ORDER BY day, period ASC")
    suspend fun getSchedulesByTeacher(teacherId: Long): List<Schedule>
    
    /**
     * الحصول على جميع حصص مدرس كـ LiveData
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND is_active = 1 ORDER BY day, period ASC")
    fun getSchedulesByTeacherLiveData(teacherId: Long): LiveData<List<Schedule>>
    
    /**
     * الحصول على جميع حصص مدرس كـ Flow
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND is_active = 1 ORDER BY day, period ASC")
    fun getSchedulesByTeacherFlow(teacherId: Long): Flow<List<Schedule>>
    
    /**
     * الحصول على حصص يوم معين لمدرس
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND day = :day AND is_active = 1 ORDER BY period ASC")
    suspend fun getSchedulesByDay(teacherId: Long, day: String): List<Schedule>
    
    /**
     * الحصول على حصص يوم معين كـ LiveData
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND day = :day AND is_active = 1 ORDER BY period ASC")
    fun getSchedulesByDayLiveData(teacherId: Long, day: String): LiveData<List<Schedule>>
    
    /**
     * الحصول على حصة معينة في يوم وفترة محددة
     */
    @Query("SELECT * FROM schedules WHERE teacher_id = :teacherId AND day = :day AND period = :period AND is_active = 1 LIMIT 1")
    suspend fun getScheduleByDayAndPeriod(teacherId: Long, day: String, period: Int): Schedule?
    
    /**
     * الحصول على الحصة الحالية
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND day = :currentDay 
        AND is_active = 1
        AND time(:currentTime) BETWEEN time(start_time) AND time(end_time)
        LIMIT 1
    """)
    suspend fun getCurrentSchedule(teacherId: Long, currentDay: String, currentTime: String): Schedule?
    
    /**
     * الحصول على الحصة الحالية كـ LiveData
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND day = :currentDay 
        AND is_active = 1
        AND time(:currentTime) BETWEEN time(start_time) AND time(end_time)
        LIMIT 1
    """)
    fun getCurrentScheduleLiveData(teacherId: Long, currentDay: String, currentTime: String): LiveData<Schedule?>
    
    /**
     * الحصول على الحصة القادمة
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND ((day = :currentDay AND time(start_time) > time(:currentTime)) 
             OR (day > :currentDay))
        AND is_active = 1
        ORDER BY 
            CASE day 
                WHEN 'الأحد' THEN 1 
                WHEN 'الاثنين' THEN 2 
                WHEN 'الثلاثاء' THEN 3 
                WHEN 'الأربعاء' THEN 4 
                WHEN 'الخميس' THEN 5 
                WHEN 'الجمعة' THEN 6 
                WHEN 'السبت' THEN 7 
            END, 
            period ASC 
        LIMIT 1
    """)
    suspend fun getNextSchedule(teacherId: Long, currentDay: String, currentTime: String): Schedule?
    
    /**
     * الحصول على الحصة القادمة كـ LiveData
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND ((day = :currentDay AND time(start_time) > time(:currentTime)) 
             OR (day > :currentDay))
        AND is_active = 1
        ORDER BY 
            CASE day 
                WHEN 'الأحد' THEN 1 
                WHEN 'الاثنين' THEN 2 
                WHEN 'الثلاثاء' THEN 3 
                WHEN 'الأربعاء' THEN 4 
                WHEN 'الخميس' THEN 5 
                WHEN 'الجمعة' THEN 6 
                WHEN 'السبت' THEN 7 
            END, 
            period ASC 
        LIMIT 1
    """)
    fun getNextScheduleLiveData(teacherId: Long, currentDay: String, currentTime: String): LiveData<Schedule?>
    
    /**
     * الحصول على حصص اليوم
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND day = :day 
        AND is_active = 1
        ORDER BY period ASC
    """)
    suspend fun getTodaySchedules(teacherId: Long, day: String): List<Schedule>
    
    /**
     * الحصول على حصص اليوم كـ LiveData
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND day = :day 
        AND is_active = 1
        ORDER BY period ASC
    """)
    fun getTodaySchedulesLiveData(teacherId: Long, day: String): LiveData<List<Schedule>>
    
    /**
     * البحث في الحصص
     */
    @Query("""
        SELECT * FROM schedules 
        WHERE teacher_id = :teacherId 
        AND is_active = 1
        AND (class_name LIKE '%' || :query || '%' 
             OR subject_name LIKE '%' || :query || '%'
             OR day LIKE '%' || :query || '%')
        ORDER BY day, period ASC
    """)
    suspend fun searchSchedules(teacherId: Long, query: String): List<Schedule>
    
    /**
     * الحصول على جميع المواد
     */
    @Query("SELECT DISTINCT subject_name FROM schedules WHERE teacher_id = :teacherId AND subject_name IS NOT NULL ORDER BY subject_name ASC")
    suspend fun getSubjectsByTeacher(teacherId: Long): List<String>
    
    /**
     * الحصول على جميع الصفوف
     */
    @Query("SELECT DISTINCT class_name FROM schedules WHERE teacher_id = :teacherId AND class_name IS NOT NULL ORDER BY class_name ASC")
    suspend fun getClassesByTeacher(teacherId: Long): List<String>
    
    /**
     * الحصول على عدد الحصص
     */
    @Query("SELECT COUNT(*) FROM schedules WHERE teacher_id = :teacherId AND is_active = 1")
    suspend fun getSchedulesCount(teacherId: Long): Int
    
    /**
     * الحصول على عدد حصص اليوم
     */
    @Query("SELECT COUNT(*) FROM schedules WHERE teacher_id = :teacherId AND day = :day AND is_active = 1")
    suspend fun getDaySchedulesCount(teacherId: Long, day: String): Int
    
    /**
     * الحصول على الأيام التي بها حصص
     */
    @Query("SELECT DISTINCT day FROM schedules WHERE teacher_id = :teacherId AND is_active = 1 ORDER BY CASE day WHEN 'الأحد' THEN 1 WHEN 'الاثنين' THEN 2 WHEN 'الثلاثاء' THEN 3 WHEN 'الأربعاء' THEN 4 WHEN 'الخميس' THEN 5 WHEN 'الجمعة' THEN 6 WHEN 'السبت' THEN 7 END")
    suspend fun getActiveDays(teacherId: Long): List<String>
    
    /**
     * تفعيل حصة
     */
    @Query("UPDATE schedules SET is_active = 1, updated_at = :updatedAt WHERE id = :scheduleId")
    suspend fun activateSchedule(scheduleId: Long, updatedAt: String): Int
    
    /**
     * إلغاء تفعيل حصة
     */
    @Query("UPDATE schedules SET is_active = 0, updated_at = :updatedAt WHERE id = :scheduleId")
    suspend fun deactivateSchedule(scheduleId: Long, updatedAt: String): Int
    
    /**
     * التحقق من تداخل الحصص
     */
    @Query("""
        SELECT COUNT(*) FROM schedules 
        WHERE teacher_id = :teacherId 
        AND day = :day 
        AND is_active = 1
        AND id != :excludeId
        AND ((time(start_time) < time(:endTime) AND time(end_time) > time(:startTime)))
    """)
    suspend fun checkScheduleOverlap(
        teacherId: Long, 
        day: String, 
        startTime: String, 
        endTime: String, 
        excludeId: Long = 0
    ): Int
    
    /**
     * إحصائيات الجدول
     */
    @Query("""
        SELECT 
            COUNT(*) as totalClasses,
            COUNT(DISTINCT day) as activeDays,
            COUNT(DISTINCT subject_name) as subjects,
            COUNT(DISTINCT class_name) as classes,
            MIN(start_time) as earliestClass,
            MAX(end_time) as latestClass
        FROM schedules 
        WHERE teacher_id = :teacherId AND is_active = 1
    """)
    suspend fun getScheduleStatistics(teacherId: Long): ScheduleStatistics
    
    /**
     * الحصول على الحصص المتداخلة
     */
    @Query("""
        SELECT s1.*, s2.id as conflictId 
        FROM schedules s1, schedules s2 
        WHERE s1.teacher_id = :teacherId 
        AND s2.teacher_id = :teacherId
        AND s1.id != s2.id
        AND s1.day = s2.day
        AND s1.is_active = 1 
        AND s2.is_active = 1
        AND ((time(s1.start_time) < time(s2.end_time) AND time(s1.end_time) > time(s2.start_time)))
        ORDER BY s1.day, s1.period
    """)
    suspend fun getConflictingSchedules(teacherId: Long): List<ScheduleConflict>
}

/**
 * إحصائيات الجدول
 */
data class ScheduleStatistics(
    val totalClasses: Int,
    val activeDays: Int,
    val subjects: Int,
    val classes: Int,
    val earliestClass: String?,
    val latestClass: String?
)

/**
 * تداخل الحصص
 */
data class ScheduleConflict(
    val id: Long,
    val teacherId: Long,
    val day: String,
    val period: Int,
    val startTime: String,
    val endTime: String,
    val className: String?,
    val subjectName: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val conflictId: Long
)

