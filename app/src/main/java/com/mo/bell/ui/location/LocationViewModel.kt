package com.mo.bell.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.bell.data.repository.CurrentLocation
import com.mo.bell.data.repository.LocationRepository
import com.mo.bell.data.repository.SchoolLocation
import com.mo.bell.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _locationEnabled = MutableStateFlow(true)
    val locationEnabled: StateFlow<Boolean> = _locationEnabled.asStateFlow()

    private val _activationRadius = MutableStateFlow(100.0)
    val activationRadius: StateFlow<Double> = _activationRadius.asStateFlow()

    private val _currentLocation = MutableStateFlow<CurrentLocation?>(null)
    val currentLocation: StateFlow<CurrentLocation?> = _currentLocation.asStateFlow()

    private val _schoolLocation = MutableStateFlow<SchoolLocation?>(null)
    val schoolLocation: StateFlow<SchoolLocation?> = _schoolLocation.asStateFlow()

    private val _locationStatus = MutableStateFlow("")
    val locationStatus: StateFlow<String> = _locationStatus.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _locationEnabled.value = settingsRepository.isLocationEnabled()
        _activationRadius.value = settingsRepository.getActivationRadius()
        _schoolLocation.value = settingsRepository.getSchoolLocation()
        _hasLocationPermission.value = locationRepository.hasLocationPermission()
    }

    fun refreshLocationData() {
        viewModelScope.launch {
            getCurrentLocation()
            updateLocationStatus()
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        _locationEnabled.value = enabled
        settingsRepository.setLocationEnabled(enabled)
        
        if (enabled) {
            refreshLocationData()
        } else {
            _locationStatus.value = "الموقع الجغرافي معطل"
        }
    }

    fun setActivationRadius(radius: Double) {
        _activationRadius.value = radius
        settingsRepository.setActivationRadius(radius)
        updateLocationStatus()
    }

    fun getCurrentLocation() {
        if (!_hasLocationPermission.value) {
            _message.value = "يرجى منح صلاحية الوصول للموقع أولاً"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    _currentLocation.value = location
                    _message.value = "تم تحديد الموقع الحالي بنجاح"
                    updateLocationStatus()
                } else {
                    _message.value = "فشل في تحديد الموقع الحالي"
                }
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء تحديد الموقع: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setCurrentLocationAsSchool() {
        val current = _currentLocation.value
        if (current == null) {
            _message.value = "يرجى تحديد الموقع الحالي أولاً"
            return
        }

        settingsRepository.setSchoolLocation(current.latitude, current.longitude)
        _schoolLocation.value = SchoolLocation(current.latitude, current.longitude)
        _message.value = "تم تعيين الموقع الحالي كموقع المدرسة"
        updateLocationStatus()
    }

    fun testLocationDetection() {
        val current = _currentLocation.value
        val school = _schoolLocation.value
        
        if (current == null) {
            _message.value = "يرجى تحديد الموقع الحالي أولاً"
            return
        }
        
        if (school == null) {
            _message.value = "يرجى تعيين موقع المدرسة أولاً"
            return
        }

        viewModelScope.launch {
            try {
                val distance = locationRepository.calculateDistance(
                    current.latitude,
                    current.longitude,
                    school.latitude,
                    school.longitude
                )
                
                val isAtSchool = distance <= _activationRadius.value
                val status = if (isAtSchool) {
                    "داخل المدرسة"
                } else {
                    "خارج المدرسة"
                }
                
                _message.value = "نتيجة الاختبار: $status (المسافة: ${distance.toInt()} متر)"
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء اختبار الموقع: ${e.message}"
            }
        }
    }

    private suspend fun updateLocationStatus() {
        if (!_locationEnabled.value) {
            _locationStatus.value = "الموقع الجغرافي معطل"
            return
        }

        if (!_hasLocationPermission.value) {
            _locationStatus.value = "لا توجد صلاحية للوصول للموقع"
            return
        }

        val current = _currentLocation.value
        val school = _schoolLocation.value

        if (current == null) {
            _locationStatus.value = "لم يتم تحديد الموقع الحالي"
            return
        }

        if (school == null) {
            _locationStatus.value = "لم يتم تعيين موقع المدرسة"
            return
        }

        try {
            val distance = locationRepository.calculateDistance(
                current.latitude,
                current.longitude,
                school.latitude,
                school.longitude
            )

            val isAtSchool = distance <= _activationRadius.value
            _locationStatus.value = if (isAtSchool) {
                "داخل المدرسة (${distance.toInt()} متر)"
            } else {
                "خارج المدرسة (${distance.toInt()} متر)"
            }
        } catch (e: Exception) {
            _locationStatus.value = "خطأ في حساب المسافة"
        }
    }

    fun onLocationPermissionGranted() {
        _hasLocationPermission.value = true
        refreshLocationData()
    }

    fun setLocationPermissionStatus(hasPermission: Boolean) {
        _hasLocationPermission.value = hasPermission
        if (hasPermission) {
            refreshLocationData()
        } else {
            _locationStatus.value = "لا توجد صلاحية للوصول للموقع"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

