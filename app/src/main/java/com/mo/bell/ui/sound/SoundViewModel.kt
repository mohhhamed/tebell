package com.mo.bell.ui.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mo.bell.audio.BellSoundType
import com.mo.bell.audio.SoundInfo
import com.mo.bell.audio.SoundManager
import com.mo.bell.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _volumeLevel = MutableStateFlow(70)
    val volumeLevel: StateFlow<Int> = _volumeLevel.asStateFlow()

    private val _soundDuration = MutableStateFlow(5)
    val soundDuration: StateFlow<Int> = _soundDuration.asStateFlow()

    private val _selectedSoundType = MutableStateFlow(BellSoundType.DEFAULT)
    val selectedSoundType: StateFlow<BellSoundType> = _selectedSoundType.asStateFlow()

    private val _availableSounds = MutableStateFlow<List<SoundInfo>>(emptyList())
    val availableSounds: StateFlow<List<SoundInfo>> = _availableSounds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadSettings()
        loadAvailableSounds()
        startPlayingStatusMonitoring()
    }

    private fun loadSettings() {
        _soundEnabled.value = settingsRepository.isSoundEnabled()
        _volumeLevel.value = settingsRepository.getVolumeLevel()
        _soundDuration.value = settingsRepository.getSoundDuration()
        // يمكن إضافة تحميل نوع الصوت المحدد من الإعدادات
    }

    private fun loadAvailableSounds() {
        _availableSounds.value = soundManager.getAvailableSounds()
    }

    private fun startPlayingStatusMonitoring() {
        viewModelScope.launch {
            while (true) {
                _isPlaying.value = soundManager.isPlaying()
                kotlinx.coroutines.delay(500) // فحص كل نصف ثانية
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        settingsRepository.setSoundEnabled(enabled)
        
        if (!enabled) {
            stopSound()
        }
    }

    fun setVolumeLevel(level: Int) {
        _volumeLevel.value = level
        settingsRepository.setVolumeLevel(level)
    }

    fun setSoundDuration(duration: Int) {
        _soundDuration.value = duration
        settingsRepository.setSoundDuration(duration)
    }

    fun selectSound(soundType: BellSoundType) {
        _selectedSoundType.value = soundType
        // يمكن حفظ الاختيار في الإعدادات
    }

    fun testCurrentSound() {
        if (!_soundEnabled.value) {
            _message.value = "الصوت غير مفعل"
            return
        }

        soundManager.testSound(
            soundType = _selectedSoundType.value,
            duration = 3 // اختبار لمدة 3 ثواني
        )
        _message.value = "تم تشغيل الصوت للاختبار"
    }

    fun stopSound() {
        soundManager.stopCurrentSound()
        _message.value = "تم إيقاف الصوت"
    }

    fun applyQuietPreset() {
        setSoundEnabled(true)
        setVolumeLevel(30)
        setSoundDuration(3)
        selectSound(BellSoundType.CHIME)
        _message.value = "تم تطبيق الإعداد الهادئ"
    }

    fun applyNormalPreset() {
        setSoundEnabled(true)
        setVolumeLevel(70)
        setSoundDuration(5)
        selectSound(BellSoundType.DEFAULT)
        _message.value = "تم تطبيق الإعداد العادي"
    }

    fun applyLoudPreset() {
        setSoundEnabled(true)
        setVolumeLevel(100)
        setSoundDuration(8)
        selectSound(BellSoundType.CLASSIC)
        _message.value = "تم تطبيق الإعداد العالي"
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.cleanup()
    }
}

