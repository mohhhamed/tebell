package com.mo.bell.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.bell.data.database.entities.Teacher
import com.mo.bell.data.repository.SettingsRepository
import com.mo.bell.data.repository.TeacherRepository
import com.mo.bell.service.ServiceManager
import com.mo.bell.service.ServicesStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val teacherRepository: TeacherRepository,
    private val serviceManager: ServiceManager
) : ViewModel() {

    private val _backgroundServiceEnabled = MutableStateFlow(true)
    val backgroundServiceEnabled: StateFlow<Boolean> = _backgroundServiceEnabled.asStateFlow()

    private val _manualModeEnabled = MutableStateFlow(false)
    val manualModeEnabled: StateFlow<Boolean> = _manualModeEnabled.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(false)
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _teacherInfo = MutableStateFlow<Teacher?>(null)
    val teacherInfo: StateFlow<Teacher?> = _teacherInfo.asStateFlow()

    private val _servicesStatus = MutableStateFlow(ServicesStatus(false, false, false, false))
    val servicesStatus: StateFlow<ServicesStatus> = _servicesStatus.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadSettings()
        loadTeacherInfo()
        refreshServicesStatus()
    }

    private fun loadSettings() {
        _backgroundServiceEnabled.value = settingsRepository.isBackgroundServiceEnabled()
        _manualModeEnabled.value = settingsRepository.isManualModeEnabled()
        _darkModeEnabled.value = settingsRepository.isDarkModeEnabled()
    }

    private fun loadTeacherInfo() {
        viewModelScope.launch {
            try {
                val teacher = teacherRepository.getCurrentTeacher()
                _teacherInfo.value = teacher
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء تحميل معلومات المدرس"
            }
        }
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        _backgroundServiceEnabled.value = enabled
        settingsRepository.setBackgroundServiceEnabled(enabled)
        
        if (enabled) {
            serviceManager.startBackgroundService()
            _message.value = "تم تفعيل الخدمة في الخلفية"
        } else {
            serviceManager.stopBackgroundService()
            _message.value = "تم إيقاف الخدمة في الخلفية"
        }
        
        refreshServicesStatus()
    }

    fun setManualModeEnabled(enabled: Boolean) {
        _manualModeEnabled.value = enabled
        settingsRepository.setManualModeEnabled(enabled)
        
        if (enabled) {
            _message.value = "تم تفعيل الوضع اليدوي - لن يتم تشغيل الجرس تلقائياً"
        } else {
            _message.value = "تم إلغاء الوضع اليدوي - سيتم تشغيل الجرس تلقائياً"
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        settingsRepository.setDarkModeEnabled(enabled)
        
        // يمكن إضافة منطق تطبيق الوضع الليلي هنا
        _message.value = if (enabled) {
            "تم تفعيل الوضع الليلي"
        } else {
            "تم تفعيل الوضع النهاري"
        }
    }

    fun updateTeacherInfo(name: String, subject: String, email: String, phone: String) {
        if (name.isBlank()) {
            _message.value = "يرجى إدخال اسم المدرس"
            return
        }

        viewModelScope.launch {
            try {
                val teacher = Teacher(
                    name = name,
                    subject = subject,
                    email = email,
                    phone = phone
                )
                
                teacherRepository.saveTeacher(teacher)
                _teacherInfo.value = teacher
                _message.value = "تم حفظ معلومات المدرس بنجاح"
                
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء حفظ معلومات المدرس: ${e.message}"
            }
        }
    }

    fun refreshServicesStatus() {
        viewModelScope.launch {
            try {
                _servicesStatus.value = serviceManager.getServicesStatus()
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء فحص حالة الخدمات"
            }
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                // إيقاف الخدمات أولاً
                serviceManager.stopAllServices()
                
                // إعادة تعيين الإعدادات
                settingsRepository.resetAllSettings()
                
                // إعادة تحميل الإعدادات
                loadSettings()
                refreshServicesStatus()
                
                _message.value = "تم إعادة تعيين جميع الإعدادات بنجاح"
                
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء إعادة تعيين الإعدادات: ${e.message}"
            }
        }
    }

    fun exportSettings() {
        viewModelScope.launch {
            try {
                // يمكن إضافة منطق تصدير الإعدادات هنا
                _message.value = "سيتم إضافة ميزة تصدير الإعدادات قريباً"
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء تصدير الإعدادات: ${e.message}"
            }
        }
    }

    fun importSettings() {
        viewModelScope.launch {
            try {
                // يمكن إضافة منطق استيراد الإعدادات هنا
                _message.value = "سيتم إضافة ميزة استيراد الإعدادات قريباً"
            } catch (e: Exception) {
                _message.value = "حدث خطأ أثناء استيراد الإعدادات: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

