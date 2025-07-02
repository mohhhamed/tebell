package com.mo.bell.data.models

import com.google.gson.annotations.SerializedName

/**
 * نموذج بيانات JSON للجدول المفصل
 * يستخدم لاستيراد الجداول من ملفات JSON العادية
 */
data class ScheduleJson(
    @SerializedName("teacher_name")
    val teacherName: String,
    
    @SerializedName("school_name")
    val schoolName: String? = null,
    
    @SerializedName("schedule")
    val schedule: List<ScheduleItemJson>
)

/**
 * عنصر واحد في الجدول المفصل
 */
data class ScheduleItemJson(
    @SerializedName("day")
    val day: String,
    
    @SerializedName("period")
    val period: Int,
    
    @SerializedName("start_time")
    val startTime: String,
    
    @SerializedName("end_time")
    val endTime: String,
    
    @SerializedName("class_name")
    val className: String? = null,
    
    @SerializedName("subject_name")
    val subjectName: String? = null
)

