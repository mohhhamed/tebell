package com.mo.bell.location

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.mo.bell.data.repository.LocationRepository
import com.mo.bell.data.repository.SettingsRepository
import com.mo.bell.notifications.NotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    lateinit var locationRepository: LocationRepository
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var notificationManager: NotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var locationMonitoringJob: Job? = null
    private var lastLocationStatus: LocationStatus? = null

    companion object {
        const val ACTION_START_LOCATION_MONITORING = "START_LOCATION_MONITORING"
        const val ACTION_STOP_LOCATION_MONITORING = "STOP_LOCATION_MONITORING"
        private const val LOCATION_UPDATE_INTERVAL = 30000L // 30 ثانية
        private const val NOTIFICATION_ID = 2001
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, notificationManager.showServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_MONITORING -> startLocationMonitoring()
            ACTION_STOP_LOCATION_MONITORING -> stopLocationMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationMonitoring() {
        if (locationMonitoringJob?.isActive == true) return

        locationMonitoringJob = serviceScope.launch {
            while (true) {
                try {
                    checkLocationStatus()
                    delay(LOCATION_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    // تسجيل الخطأ ومتابعة المراقبة
                    delay(LOCATION_UPDATE_INTERVAL)
                }
            }
        }
    }

    private suspend fun checkLocationStatus() {
        if (!settingsRepository.isLocationEnabled()) {
            return
        }

        val schoolLocation = settingsRepository.getSchoolLocation()
        if (schoolLocation == null) {
            return
        }

        val currentLocation = locationRepository.getCurrentLocation()
        if (currentLocation == null) {
            return
        }

        val distance = locationRepository.calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            schoolLocation.latitude,
            schoolLocation.longitude
        )

        val activationRadius = settingsRepository.getActivationRadius()
        val isAtSchool = distance <= activationRadius

        val newStatus = if (isAtSchool) {
            LocationStatus.AT_SCHOOL
        } else {
            LocationStatus.AWAY_FROM_SCHOOL
        }

        // إرسال إشعار فقط عند تغيير الحالة
        if (newStatus != lastLocationStatus) {
            notificationManager.showLocationStatusNotification(isAtSchool, distance)
            lastLocationStatus = newStatus
            
            // حفظ الموقع في قاعدة البيانات
            saveLocationToDatabase(currentLocation, isAtSchool, distance)
        }
    }

    private suspend fun saveLocationToDatabase(
        currentLocation: com.mo.bell.data.repository.CurrentLocation,
        isAtSchool: Boolean,
        distance: Double
    ) {
        val locationEntity = com.mo.bell.data.database.entities.Location(
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude,
            accuracy = currentLocation.accuracy,
            isAtSchool = isAtSchool,
            distance = distance,
            timestamp = currentLocation.timestamp
        )
        
        locationRepository.saveLocation(locationEntity)
    }

    private fun stopLocationMonitoring() {
        locationMonitoringJob?.cancel()
        locationMonitoringJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationMonitoring()
        serviceScope.cancel()
    }
}

enum class LocationStatus {
    AT_SCHOOL,
    AWAY_FROM_SCHOOL,
    UNKNOWN
}

