package com.mo.bell.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.mo.bell.R
import com.mo.bell.data.repository.SettingsRepository
import com.mo.bell.data.repository.VibrationMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var currentSoundJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        initializeVibrator()
    }

    private fun initializeVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * تشغيل صوت التنبيه
     */
    fun playBellSound(
        soundType: BellSoundType = BellSoundType.DEFAULT,
        duration: Int? = null,
        volume: Int? = null
    ) {
        val vibrationMode = settingsRepository.getVibrationMode()
        val soundEnabled = settingsRepository.isSoundEnabled()
        val actualDuration = duration ?: settingsRepository.getSoundDuration()
        val actualVolume = volume ?: settingsRepository.getVolumeLevel()

        when (vibrationMode) {
            VibrationMode.SOUND_WITH_VIBRATION -> {
                if (soundEnabled) playSound(soundType, actualDuration, actualVolume)
                playVibration(actualDuration)
            }
            VibrationMode.SOUND_ONLY -> {
                if (soundEnabled) playSound(soundType, actualDuration, actualVolume)
            }
            VibrationMode.VIBRATION_ONLY -> {
                playVibration(actualDuration)
            }
            VibrationMode.SILENT -> {
                // لا تشغيل صوت أو اهتزاز
            }
        }
    }

    /**
     * تشغيل الصوت
     */
    private fun playSound(soundType: BellSoundType, duration: Int, volume: Int) {
        stopCurrentSound()

        try {
            val soundUri = getSoundUri(soundType)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                
                setDataSource(context, soundUri)
                setVolume(volume / 100f, volume / 100f)
                isLooping = true
                prepare()
                start()
            }

            // إيقاف الصوت بعد المدة المحددة
            currentSoundJob = coroutineScope.launch {
                delay(duration * 1000L)
                stopCurrentSound()
            }

        } catch (e: Exception) {
            // في حالة فشل تشغيل الصوت المخصص، استخدم صوت النظام
            playSystemSound(duration, volume)
        }
    }

    /**
     * تشغيل صوت النظام الافتراضي
     */
    private fun playSystemSound(duration: Int, volume: Int) {
        try {
            val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, defaultRingtoneUri)
            
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = (maxVolume * volume / 100f).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
            
            ringtone.play()
            
            currentSoundJob = coroutineScope.launch {
                delay(duration * 1000L)
                ringtone.stop()
            }
        } catch (e: Exception) {
            // فشل في تشغيل أي صوت
        }
    }

    /**
     * تشغيل الاهتزاز
     */
    private fun playVibration(duration: Int) {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // نمط اهتزاز متقطع
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                
                val vibrationEffect = VibrationEffect.createWaveform(
                    pattern,
                    amplitudes,
                    0 // تكرار من البداية
                )
                vib.vibrate(vibrationEffect)
                
                // إيقاف الاهتزاز بعد المدة المحددة
                coroutineScope.launch {
                    delay(duration * 1000L)
                    vib.cancel()
                }
            } else {
                // للإصدارات الأقدم
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
                
                coroutineScope.launch {
                    delay(duration * 1000L)
                    vib.cancel()
                }
            }
        }
    }

    /**
     * الحصول على URI الصوت حسب النوع
     */
    private fun getSoundUri(soundType: BellSoundType): Uri {
        return when (soundType) {
            BellSoundType.DEFAULT -> getResourceUri(R.raw.bell_default)
            BellSoundType.CLASSIC -> getResourceUri(R.raw.bell_classic)
            BellSoundType.MODERN -> getResourceUri(R.raw.bell_modern)
            BellSoundType.CHIME -> getResourceUri(R.raw.bell_chime)
            BellSoundType.DIGITAL -> getResourceUri(R.raw.bell_digital)
            BellSoundType.CUSTOM -> getCustomSoundUri()
        }
    }

    /**
     * الحصول على URI للموارد
     */
    private fun getResourceUri(@RawRes resourceId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resourceId")
    }

    /**
     * الحصول على URI للصوت المخصص
     */
    private fun getCustomSoundUri(): Uri {
        // يمكن للمستخدم تحديد صوت مخصص من الجهاز
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    /**
     * إيقاف الصوت الحالي
     */
    fun stopCurrentSound() {
        currentSoundJob?.cancel()
        currentSoundJob = null
        
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
        
        vibrator?.cancel()
    }

    /**
     * اختبار الصوت
     */
    fun testSound(
        soundType: BellSoundType = BellSoundType.DEFAULT,
        duration: Int = 3
    ) {
        playBellSound(soundType, duration)
    }

    /**
     * فحص ما إذا كان الصوت يعمل حالياً
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true || currentSoundJob?.isActive == true
    }

    /**
     * الحصول على قائمة الأصوات المتاحة
     */
    fun getAvailableSounds(): List<SoundInfo> {
        return listOf(
            SoundInfo(BellSoundType.DEFAULT, "الجرس الافتراضي", "صوت جرس مدرسي تقليدي"),
            SoundInfo(BellSoundType.CLASSIC, "الجرس الكلاسيكي", "صوت جرس معدني كلاسيكي"),
            SoundInfo(BellSoundType.MODERN, "الجرس العصري", "صوت جرس إلكتروني عصري"),
            SoundInfo(BellSoundType.CHIME, "الجرس الموسيقي", "صوت جرس موسيقي لطيف"),
            SoundInfo(BellSoundType.DIGITAL, "الجرس الرقمي", "صوت تنبيه رقمي حديث"),
            SoundInfo(BellSoundType.CUSTOM, "صوت مخصص", "اختر صوت من جهازك")
        )
    }

    /**
     * تنظيف الموارد
     */
    fun cleanup() {
        stopCurrentSound()
        currentSoundJob?.cancel()
    }
}

/**
 * أنواع أصوات الجرس
 */
enum class BellSoundType {
    DEFAULT,
    CLASSIC,
    MODERN,
    CHIME,
    DIGITAL,
    CUSTOM
}

/**
 * معلومات الصوت
 */
data class SoundInfo(
    val type: BellSoundType,
    val name: String,
    val description: String
)

