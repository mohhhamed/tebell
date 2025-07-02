package com.mo.bell.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * كيان المدرس في قاعدة البيانات
 */
@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "school_name")
    val schoolName: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = LocalDateTime.now().toString(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = LocalDateTime.now().toString()
) {
    /**
     * الحصول على الاسم المختصر للمدرس
     */
    fun getShortName(): String {
        return name.split(" ").take(2).joinToString(" ")
    }
    
    /**
     * الحصول على الأحرف الأولى من الاسم
     */
    fun getInitials(): String {
        return name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }
    
    /**
     * التحقق من صحة بيانات المدرس
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && name.length >= 2
    }
    
    /**
     * إنشاء نسخة محدثة من المدرس
     */
    fun updated(
        name: String = this.name,
        schoolName: String? = this.schoolName,
        isActive: Boolean = this.isActive
    ): Teacher {
        return copy(
            name = name,
            schoolName = schoolName,
            isActive = isActive,
            updatedAt = LocalDateTime.now().toString()
        )
    }
    
    companion object {
        /**
         * إنشاء مدرس جديد
         */
        fun create(name: String, schoolName: String? = null): Teacher {
            return Teacher(
                name = name.trim(),
                schoolName = schoolName?.trim()
            )
        }
        
        /**
         * مدرس افتراضي للاختبار
         */
        fun default(): Teacher {
            return Teacher(
                name = "مدرس تجريبي",
                schoolName = "مدرسة تجريبية"
            )
        }
    }
}

