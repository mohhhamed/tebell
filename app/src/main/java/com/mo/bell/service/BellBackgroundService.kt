package com.mo.bell.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.mo.bell.audio.SoundManager
import com.mo.bell.data.repository.LocationRepository
import com.mo.bell.data.repository.ScheduleRepository
import com.mo.bell.data.repository.SettingsRepository
import com.mo.bell.notifications.NotificationManager
import com.mo.bell.utils.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BellBackgroundService : Service() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var locationRepository: LocationRepository
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    @Inject
    lateinit var soundManager: SoundManager
    
    @Inject
    lateinit var timeUtils: TimeUtils

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scheduleMonitoringJob: Job? = null
    private var locationMonitoringJob: Job? = null
    
    private var lastCheckedMinute = -1
    private var currentClassId: Long? = null
    private var isAtSchool = false

    companion object {
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
        private const val SCHEDULE_CHECK_INTERVAL = 30000L // 30 ثانية
        private const val LOCATION_CHECK_INTERVAL = 60000L // دقيقة واحدة
        private const val NOTIFICATION_ID = 3001
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, notificationManager.showServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> startBackgroundMonitoring()
            ACTION_STOP_SERVICE -> stopBackgroundMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startBackgroundMonitoring() {
        if (!settingsRepository.isBackgroundServiceEnabled()) {
            stopSelf()
            return
        }

        startScheduleMonitoring()
        startLocationMonitoring()
    }

    private fun startScheduleMonitoring() {
        if (scheduleMonitoringJob?.isActive == true) return

        scheduleMonitoringJob = serviceScope.launch {
            while (true) {
                try {
                    checkScheduleAndTriggerBell()
                    delay(SCHEDULE_CHECK_INTERVAL)
                } catch (e: Exception) {
                    // تسجيل الخطأ ومتابعة المراقبة
                    delay(SCHEDULE_CHECK_INTERVAL)
                }
            }
        }
    }

    private fun startLocationMonitoring() {
        if (!settingsRepository.isLocationEnabled()) return
        if (locationMonitoringJob?.isActive == true) return

        locationMonitoringJob = serviceScope.launch {
            while (true) {
                try {
                    checkLocationStatus()
                    delay(LOCATION_CHECK_INTERVAL)
                } catch (e: Exception) {
                    delay(LOCATION_CHECK_INTERVAL)
                }
            }
        }
    }

    private suspend fun checkScheduleAndTriggerBell() {
        val currentTime = Calendar.getInstance()
        val currentMinute = currentTime.get(Calendar.MINUTE)
        val currentDayOfWeek = timeUtils.getCurrentDayOfWeek()
        
        // فحص مرة واحدة فقط في كل دقيقة
        if (currentMinute == lastCheckedMinute) return
        lastCheckedMinute = currentMinute

        // التحقق من الوضع اليدوي
        if (settingsRepository.isManualModeEnabled()) return

        // التحقق من وجود الموقع إذا كان مفعلاً
        if (settingsRepository.isLocationEnabled() && !isAtSchool) return

        val todaySchedule = scheduleRepository.getScheduleForDay(currentDayOfWeek)
        if (todaySchedule.isEmpty()) return

        val currentTimeString = timeUtils.getCurrentTimeString()

        // البحث عن الحصة الحالية
        val currentClass = todaySchedule.find { classItem ->
            timeUtils.isTimeInRange(
                currentTime,
                classItem.startTime,
                classItem.endTime
            )
        }

        if (currentClass != null) {
            // هناك حصة حالية
            handleCurrentClass(currentClass, currentTimeString)
        } else {
            // لا توجد حصة حالية، التحقق من بداية حصة جديدة
            val nextClass = todaySchedule.find { classItem ->
                classItem.startTime == currentTimeString
            }

            if (nextClass != null) {
                handleClassStart(nextClass)
            }
        }
    }

    private suspend fun handleCurrentClass(
        classItem: com.mo.bell.data.database.entities.Schedule,
        currentTimeString: String
    ) {
        // التحقق من نهاية الحصة
        if (classItem.endTime == currentTimeString) {
            handleClassEnd(classItem)
            currentClassId = null
        } else if (currentClassId != classItem.id) {
            // حصة جديدة بدأت
            handleClassStart(classItem)
        }

        // تحديث إشعار الحصة الحالية
        updateCurrentClassNotification(classItem, currentTimeString)
    }

    private suspend fun handleClassStart(classItem: com.mo.bell.data.database.entities.Schedule) {
        currentClassId = classItem.id

        // تشغيل الجرس
        if (settingsRepository.isSoundEnabled()) {
            soundManager.playBellSound()
        }

        // إرسال إشعار بداية الحصة
        notificationManager.showClassStartNotification(
            classItem.className,
            classItem.subjectName,
            timeUtils.formatTime(classItem.endTime)
        )
    }

    private suspend fun handleClassEnd(classItem: com.mo.bell.data.database.entities.Schedule) {
        // تشغيل الجرس
        if (settingsRepository.isSoundEnabled()) {
            soundManager.playBellSound()
        }

        // البحث عن الحصة القادمة
        val currentDayOfWeek = timeUtils.getCurrentDayOfWeek()
        val todaySchedule = scheduleRepository.getScheduleForDay(currentDayOfWeek)
        val nextClass = todaySchedule.find { nextClassItem ->
            timeUtils.isAfterTime(nextClassItem.startTime, classItem.endTime)
        }

        // إرسال إشعار نهاية الحصة
        notificationManager.showClassEndNotification(
            classItem.className,
            classItem.subjectName,
            nextClass?.className
        )

        // إلغاء إشعار الحصة الحالية
        notificationManager.cancelCurrentClassNotification()
    }

    private suspend fun updateCurrentClassNotification(
        classItem: com.mo.bell.data.database.entities.Schedule,
        currentTimeString: String
    ) {
        val currentTime = Calendar.getInstance()
        val progressPercentage = timeUtils.calculateClassProgress(
            currentTime,
            classItem.startTime,
            classItem.endTime
        )
        val remainingMinutes = timeUtils.calculateRemainingMinutes(
            currentTime,
            classItem.endTime
        )

        notificationManager.showCurrentClassNotification(
            classItem.className,
            classItem.subjectName,
            remainingMinutes,
            progressPercentage
        )
    }

    private suspend fun checkLocationStatus() {
        if (!settingsRepository.isLocationEnabled()) return

        val schoolLocation = settingsRepository.getSchoolLocation() ?: return
        val currentLocation = locationRepository.getCurrentLocation() ?: return

        val distance = locationRepository.calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            schoolLocation.latitude,
            schoolLocation.longitude
        )

        val activationRadius = settingsRepository.getActivationRadius()
        val newIsAtSchool = distance <= activationRadius

        // تحديث الحالة فقط عند التغيير
        if (newIsAtSchool != isAtSchool) {
            isAtSchool = newIsAtSchool
            
            // إرسال إشعار تغيير الموقع
            notificationManager.showLocationStatusNotification(isAtSchool, distance)
        }
    }

    private fun stopBackgroundMonitoring() {
        scheduleMonitoringJob?.cancel()
        locationMonitoringJob?.cancel()
        soundManager.stopCurrentSound()
        notificationManager.cancelAllNotifications()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundMonitoring()
        serviceScope.cancel()
    }
}

