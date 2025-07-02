package com.mo.bell.data.models

import com.google.gson.annotations.SerializedName

/**
 * نموذج بيانات JSON للجدول المضغوط
 * يستخدم لاستيراد الجداول من رموز QR المضغوطة
 */
data class CompactScheduleJson(
    @SerializedName("t")
    val teacherName: String,
    
    @SerializedName("sn")
    val schoolName: String? = null,
    
    @SerializedName("s")
    val schedule: List<CompactScheduleItemJson>,
    
    @SerializedName("pq")
    val partQuery: String? = null // للجداول المقسمة على عدة أجزاء مثل "1/2"
)

/**
 * عنصر واحد في الجدول المضغوط
 */
data class CompactScheduleItemJson(
    @SerializedName("d")
    val day: String,
    
    @SerializedName("p")
    val period: Int,
    
    @SerializedName("st")
    val startTime: String,
    
    @SerializedName("et")
    val endTime: String,
    
    @SerializedName("c")
    val className: String? = null,
    
    @SerializedName("sn")
    val subjectName: String? = null
)

/**
 * فئة مساعدة لتجميع أجزاء الجدول المقسم
 */
data class ScheduleParts(
    val parts: MutableList<CompactScheduleJson> = mutableListOf(),
    val totalParts: Int = 1,
    val currentParts: Int = 0
) {
    fun isComplete(): Boolean = currentParts >= totalParts
    
    fun addPart(part: CompactScheduleJson) {
        parts.add(part)
        // تحليل partQuery لمعرفة العدد الكلي والحالي
        part.partQuery?.let { pq ->
            val parts = pq.split("/")
            if (parts.size == 2) {
                val current = parts[0].toIntOrNull() ?: 1
                val total = parts[1].toIntOrNull() ?: 1
                // تحديث المعلومات إذا كانت أكبر
                if (total > totalParts) {
                    // totalParts = total (readonly, need to create new instance)
                }
            }
        }
    }
    
    fun combineSchedules(): CompactScheduleJson? {
        if (!isComplete() || parts.isEmpty()) return null
        
        val firstPart = parts.first()
        val allScheduleItems = mutableListOf<CompactScheduleItemJson>()
        
        // دمج جميع عناصر الجدول من كل الأجزاء
        parts.forEach { part ->
            allScheduleItems.addAll(part.schedule)
        }
        
        return CompactScheduleJson(
            teacherName = firstPart.teacherName,
            schoolName = firstPart.schoolName,
            schedule = allScheduleItems,
            partQuery = null // إزالة معلومات التقسيم بعد الدمج
        )
    }
}

