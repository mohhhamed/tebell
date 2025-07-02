package com.mo.bell.data

import com.google.gson.annotations.SerializedName

data class TeacherSchedule(
    @SerializedName("teacher_name")
    val teacherName: String,
    val schedule: List<Lesson>
)

data class Lesson(
    val day: String,
    val period: Int,
    @SerializedName("start_time")
    val startTime: String,
    @SerializedName("end_time")
    val endTime: String
) : java.io.Serializable