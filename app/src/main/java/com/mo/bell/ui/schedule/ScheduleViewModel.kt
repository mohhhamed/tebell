package com.mo.bell.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.bell.data.database.entities.Schedule
import com.mo.bell.data.repository.ScheduleRepository
import com.mo.bell.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val timeUtils: TimeUtils
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(timeUtils.getCurrentDayOfWeek())
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    private val _daySchedule = MutableStateFlow<List<Schedule>>(emptyList())
    val daySchedule: StateFlow<List<Schedule>> = _daySchedule.asStateFlow()

    private val _currentClass = MutableStateFlow<Schedule?>(null)
    val currentClass: StateFlow<Schedule?> = _currentClass.asStateFlow()

    private val _nextClass = MutableStateFlow<Schedule?>(null)
    val nextClass: StateFlow<Schedule?> = _nextClass.asStateFlow()

    private val _dayStats = MutableStateFlow(DayStats())
    val dayStats: StateFlow<DayStats> = _dayStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadScheduleForSelectedDay()
        refreshCurrentTime()
    }

    fun selectDay(day: Int) {
        _selectedDay.value = day
        loadScheduleForSelectedDay()
    }

    fun selectToday() {
        selectDay(timeUtils.getCurrentDayOfWeek())
    }

    fun refreshSchedule() {
        loadScheduleForSelectedDay()
        refreshCurrentTime()
    }

    fun refreshCurrentTime() {
        viewModelScope.launch {
            updateCurrentAndNextClass()
        }
    }

    private fun loadScheduleForSelectedDay() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val schedule = scheduleRepository.getScheduleForDay(_selectedDay.value)
                _daySchedule.value = schedule.sortedBy { it.startTime }
                
                calculateDayStats(schedule)
                updateCurrentAndNextClass()
                
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء تحميل الجدول: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateCurrentAndNextClass() {
        val schedule = _daySchedule.value
        if (schedule.isEmpty()) {
            _currentClass.value = null
            _nextClass.value = null
            return
        }

        val currentTime = Calendar.getInstance()
        val currentTimeString = timeUtils.getCurrentTimeString()

        // البحث عن الحصة الحالية
        val current = schedule.find { classItem ->
            timeUtils.isTimeInRange(
                currentTime,
                classItem.startTime,
                classItem.endTime
            )
        }

        _currentClass.value = current

        // البحث عن الحصة القادمة
        val next = if (current != null) {
            // إذا كان هناك حصة حالية، ابحث عن التالية
            schedule.find { classItem ->
                timeUtils.isAfterTime(classItem.startTime, current.endTime)
            }
        } else {
            // إذا لم تكن هناك حصة حالية، ابحث عن أول حصة بعد الوقت الحالي
            schedule.find { classItem ->
                timeUtils.isAfterTime(classItem.startTime, currentTimeString)
            }
        }

        _nextClass.value = next
    }

    private fun calculateDayStats(schedule: List<Schedule>) {
        val totalClasses = schedule.size
        val totalDurationMinutes = schedule.sumOf { classItem ->
            timeUtils.calculateDurationMinutes(classItem.startTime, classItem.endTime)
        }

        val currentStatus = when {
            _currentClass.value != null -> "جاري: ${_currentClass.value!!.subjectName}"
            _nextClass.value != null -> "القادم: ${_nextClass.value!!.subjectName}"
            else -> "لا توجد حصص اليوم"
        }

        _dayStats.value = DayStats(
            totalClasses = totalClasses,
            totalDurationMinutes = totalDurationMinutes,
            currentStatus = currentStatus
        )
    }

    fun importFromFile() {
        viewModelScope.launch {
            try {
                // يمكن إضافة منطق استيراد الملف هنا
                _message.value = "سيتم إضافة ميزة استيراد الملف قريباً"
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء الاستيراد: ${e.message}"
            }
        }
    }

    fun importFromQRCode() {
        viewModelScope.launch {
            try {
                // يمكن إضافة منطق استيراد QR Code هنا
                _message.value = "سيتم إضافة ميزة استيراد QR Code قريباً"
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء الاستيراد: ${e.message}"
            }
        }
    }

    fun exportSchedule() {
        viewModelScope.launch {
            try {
                val allSchedule = scheduleRepository.getAllSchedules()
                if (allSchedule.isEmpty()) {
                    _message.value = "لا يوجد جدول للتصدير"
                    return@launch
                }

                // يمكن إضافة منطق التصدير هنا
                _message.value = "سيتم إضافة ميزة التصدير قريباً"
                
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء التصدير: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class DayStats(
    val totalClasses: Int = 0,
    val totalDurationMinutes: Int = 0,
    val currentStatus: String = ""
)

