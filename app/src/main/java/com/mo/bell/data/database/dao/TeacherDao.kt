package com.mo.bell.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mo.bell.data.database.entities.Teacher
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الوصول لبيانات المدرسين
 */
@Dao
interface TeacherDao {
    
    /**
     * إدراج مدرس جديد
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long
    
    /**
     * إدراج عدة مدرسين
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>): List<Long>
    
    /**
     * تحديث بيانات مدرس
     */
    @Update
    suspend fun updateTeacher(teacher: Teacher): Int
    
    /**
     * حذف مدرس
     */
    @Delete
    suspend fun deleteTeacher(teacher: Teacher): Int
    
    /**
     * حذف مدرس بالمعرف
     */
    @Query("DELETE FROM teachers WHERE id = :teacherId")
    suspend fun deleteTeacherById(teacherId: Long): Int
    
    /**
     * حذف جميع المدرسين
     */
    @Query("DELETE FROM teachers")
    suspend fun deleteAllTeachers(): Int
    
    /**
     * الحصول على مدرس بالمعرف
     */
    @Query("SELECT * FROM teachers WHERE id = :teacherId")
    suspend fun getTeacherById(teacherId: Long): Teacher?
    
    /**
     * الحصول على مدرس بالمعرف كـ LiveData
     */
    @Query("SELECT * FROM teachers WHERE id = :teacherId")
    fun getTeacherByIdLiveData(teacherId: Long): LiveData<Teacher?>
    
    /**
     * الحصول على مدرس بالمعرف كـ Flow
     */
    @Query("SELECT * FROM teachers WHERE id = :teacherId")
    fun getTeacherByIdFlow(teacherId: Long): Flow<Teacher?>
    
    /**
     * الحصول على مدرس بالاسم
     */
    @Query("SELECT * FROM teachers WHERE name = :name LIMIT 1")
    suspend fun getTeacherByName(name: String): Teacher?
    
    /**
     * البحث عن مدرسين بالاسم
     */
    @Query("SELECT * FROM teachers WHERE name LIKE '%' || :name || '%' ORDER BY name ASC")
    suspend fun searchTeachersByName(name: String): List<Teacher>
    
    /**
     * الحصول على جميع المدرسين
     */
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    suspend fun getAllTeachers(): List<Teacher>
    
    /**
     * الحصول على جميع المدرسين كـ LiveData
     */
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachersLiveData(): LiveData<List<Teacher>>
    
    /**
     * الحصول على جميع المدرسين كـ Flow
     */
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachersFlow(): Flow<List<Teacher>>
    
    /**
     * الحصول على المدرسين النشطين فقط
     */
    @Query("SELECT * FROM teachers WHERE is_active = 1 ORDER BY name ASC")
    suspend fun getActiveTeachers(): List<Teacher>
    
    /**
     * الحصول على المدرسين النشطين كـ LiveData
     */
    @Query("SELECT * FROM teachers WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveTeachersLiveData(): LiveData<List<Teacher>>
    
    /**
     * الحصول على المدرسين حسب المدرسة
     */
    @Query("SELECT * FROM teachers WHERE school_name = :schoolName ORDER BY name ASC")
    suspend fun getTeachersBySchool(schoolName: String): List<Teacher>
    
    /**
     * الحصول على عدد المدرسين
     */
    @Query("SELECT COUNT(*) FROM teachers")
    suspend fun getTeachersCount(): Int
    
    /**
     * الحصول على عدد المدرسين النشطين
     */
    @Query("SELECT COUNT(*) FROM teachers WHERE is_active = 1")
    suspend fun getActiveTeachersCount(): Int
    
    /**
     * الحصول على آخر مدرس تم إضافته
     */
    @Query("SELECT * FROM teachers ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestTeacher(): Teacher?
    
    /**
     * الحصول على المدرس الأول (للتطبيق أحادي المدرس)
     */
    @Query("SELECT * FROM teachers WHERE is_active = 1 ORDER BY created_at ASC LIMIT 1")
    suspend fun getFirstActiveTeacher(): Teacher?
    
    /**
     * الحصول على المدرس الأول كـ LiveData
     */
    @Query("SELECT * FROM teachers WHERE is_active = 1 ORDER BY created_at ASC LIMIT 1")
    fun getFirstActiveTeacherLiveData(): LiveData<Teacher?>
    
    /**
     * الحصول على المدرس الأول كـ Flow
     */
    @Query("SELECT * FROM teachers WHERE is_active = 1 ORDER BY created_at ASC LIMIT 1")
    fun getFirstActiveTeacherFlow(): Flow<Teacher?>
    
    /**
     * تفعيل مدرس
     */
    @Query("UPDATE teachers SET is_active = 1, updated_at = :updatedAt WHERE id = :teacherId")
    suspend fun activateTeacher(teacherId: Long, updatedAt: String): Int
    
    /**
     * إلغاء تفعيل مدرس
     */
    @Query("UPDATE teachers SET is_active = 0, updated_at = :updatedAt WHERE id = :teacherId")
    suspend fun deactivateTeacher(teacherId: Long, updatedAt: String): Int
    
    /**
     * تحديث اسم المدرس
     */
    @Query("UPDATE teachers SET name = :name, updated_at = :updatedAt WHERE id = :teacherId")
    suspend fun updateTeacherName(teacherId: Long, name: String, updatedAt: String): Int
    
    /**
     * تحديث اسم المدرسة للمدرس
     */
    @Query("UPDATE teachers SET school_name = :schoolName, updated_at = :updatedAt WHERE id = :teacherId")
    suspend fun updateTeacherSchool(teacherId: Long, schoolName: String?, updatedAt: String): Int
    
    /**
     * التحقق من وجود مدرس بالاسم
     */
    @Query("SELECT EXISTS(SELECT 1 FROM teachers WHERE name = :name)")
    suspend fun teacherExistsByName(name: String): Boolean
    
    /**
     * التحقق من وجود مدرس بالمعرف
     */
    @Query("SELECT EXISTS(SELECT 1 FROM teachers WHERE id = :teacherId)")
    suspend fun teacherExistsById(teacherId: Long): Boolean
    
    /**
     * الحصول على أسماء جميع المدارس
     */
    @Query("SELECT DISTINCT school_name FROM teachers WHERE school_name IS NOT NULL ORDER BY school_name ASC")
    suspend fun getAllSchoolNames(): List<String>
    
    /**
     * إحصائيات المدرسين
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN is_active = 1 THEN 1 END) as active,
            COUNT(DISTINCT school_name) as schools
        FROM teachers
    """)
    suspend fun getTeacherStatistics(): TeacherStatistics
}

/**
 * إحصائيات المدرسين
 */
data class TeacherStatistics(
    val total: Int,
    val active: Int,
    val schools: Int
)

