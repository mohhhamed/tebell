package com.mo.bell.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.bell.data.models.CurrentClass
import com.mo.bell.data.models.NextClass
import com.mo.bell.data.repository.ScheduleRepository
import com.mo.bell.data.repository.SettingsRepository
import com.mo.bell.data.repository.LocationRepository
import com.mo.bell.utils.TimeUtils
import com.mo.bell.utils.QRCodeParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val timeUtils: TimeUtils,
    private val qrCodeParser: QRCodeParser
) : ViewModel() {

    private val _currentClass = MutableStateFlow<CurrentClass?>(null)
    val currentClass: StateFlow<CurrentClass?> = _currentClass.asStateFlow()

    private val _nextClass = MutableStateFlow<NextClass?>(null)
    val nextClass: StateFlow<NextClass?> = _nextClass.asStateFlow()

    private val _locationStatus = MutableStateFlow(LocationStatus.UNKNOWN)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshData()
        startPeriodicUpdates()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                updateCurrentAndNextClass()
                updateLocationStatus()
            } catch (e: Exception) {
                _errorMessage.value = "حدث خطأ أثناء تحديث البيانات: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateCurrentAndNextClass() {
        val currentTime = Calendar.getInstance()
        val currentDayOfWeek = timeUtils.getCurrentDayOfWeek()
        
        // الحصول على جدول اليوم
        val todaySchedule = scheduleRepository.getScheduleForDay(currentDayOfWeek)
        
        if (todaySchedule.isEmpty()) {
            _currentClass.value = null
            _nextClass.value = null
            return
        }

        // البحث عن الحصة الحالية
        val currentClassData = todaySchedule.find { classItem ->
            timeUtils.isTimeInRange(
                currentTime,
                classItem.startTime,
                classItem.endTime
            )
        }

        if (currentClassData != null) {
            // هناك حصة حالية
            val progressPercentage = timeUtils.calculateClassProgress(
                currentTime,
                currentClassData.startTime,
                currentClassData.endTime
            )
            val remainingMinutes = timeUtils.calculateRemainingMinutes(
                currentTime,
                currentClassData.endTime
            )

            _currentClass.value = CurrentClass(
                className = currentClassData.className,
                subjectName = currentClassData.subjectName,
                startTime = timeUtils.formatTime(currentClassData.startTime),
                endTime = timeUtils.formatTime(currentClassData.endTime),
                progressPercentage = progressPercentage,
                remainingMinutes = remainingMinutes
            )

            // البحث عن الحصة القادمة بعد الحصة الحالية
            val nextClassData = todaySchedule.find { classItem ->
                timeUtils.isAfterTime(classItem.startTime, currentClassData.endTime)
            }

            if (nextClassData != null) {
                val minutesUntilStart = timeUtils.calculateMinutesUntil(
                    currentTime,
                    nextClassData.startTime
                )

                _nextClass.value = NextClass(
                    className = nextClassData.className,
                    subjectName = nextClassData.subjectName,
                    startTime = timeUtils.formatTime(nextClassData.startTime),
                    endTime = timeUtils.formatTime(nextClassData.endTime),
                    minutesUntilStart = minutesUntilStart
                )
            } else {
                _nextClass.value = null
            }
        } else {
            // لا توجد حصة حالية، البحث عن الحصة القادمة
            _currentClass.value = null

            val nextClassData = todaySchedule.find { classItem ->
                timeUtils.isAfterCurrentTime(classItem.startTime)
            }

            if (nextClassData != null) {
                val minutesUntilStart = timeUtils.calculateMinutesUntil(
                    currentTime,
                    nextClassData.startTime
                )

                _nextClass.value = NextClass(
                    className = nextClassData.className,
                    subjectName = nextClassData.subjectName,
                    startTime = timeUtils.formatTime(nextClassData.startTime),
                    endTime = timeUtils.formatTime(nextClassData.endTime),
                    minutesUntilStart = minutesUntilStart
                )
            } else {
                _nextClass.value = null
            }
        }
    }

    private suspend fun updateLocationStatus() {
        try {
            val isLocationEnabled = settingsRepository.isLocationEnabled()
            if (!isLocationEnabled) {
                _locationStatus.value = LocationStatus.DISABLED
                return
            }

            val currentLocation = locationRepository.getCurrentLocation()
            val schoolLocation = settingsRepository.getSchoolLocation()

            if (currentLocation == null || schoolLocation == null) {
                _locationStatus.value = LocationStatus.UNKNOWN
                return
            }

            val distance = locationRepository.calculateDistance(
                currentLocation.latitude,
                currentLocation.longitude,
                schoolLocation.latitude,
                schoolLocation.longitude
            )

            val activationRadius = settingsRepository.getActivationRadius()
            
            _locationStatus.value = if (distance <= activationRadius) {
                LocationStatus.AT_SCHOOL
            } else {
                LocationStatus.AWAY_FROM_SCHOOL
            }
        } catch (e: Exception) {
            _locationStatus.value = LocationStatus.UNKNOWN
        }
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60000) // تحديث كل دقيقة
                updateCurrentAndNextClass()
            }
        }
    }

    fun handleQRCodeResult(qrCodeData: String) {
        viewModelScope.launch {
            try {
                val scheduleData = qrCodeParser.parseQRCode(qrCodeData)
                if (scheduleData != null) {
                    scheduleRepository.importSchedule(scheduleData)
                    refreshData()
                    _errorMessage.value = "تم استيراد الجدول بنجاح من رمز QR"
                } else {
                    _errorMessage.value = "رمز QR غير صالح أو لا يحتوي على بيانات جدول"
                }
            } catch (e: Exception) {
                _errorMessage.value = "حدث خطأ أثناء معالجة رمز QR: ${e.message}"
            }
        }
    }

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            updateLocationStatus()
        }
    }

    fun onNotificationPermissionGranted() {
        // يمكن إضافة منطق إضافي هنا
    }

    fun setLocationStatus(status: LocationStatus) {
        _locationStatus.value = status
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

