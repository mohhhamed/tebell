package com.mo.bell.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.mo.bell.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    /**
     * بدء خدمة العمل في الخلفية
     */
    fun startBackgroundService() {
        if (!settingsRepository.isBackgroundServiceEnabled()) {
            return
        }

        val intent = Intent(context, BellBackgroundService::class.java).apply {
            action = BellBackgroundService.ACTION_START_SERVICE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * إيقاف خدمة العمل في الخلفية
     */
    fun stopBackgroundService() {
        val intent = Intent(context, BellBackgroundService::class.java).apply {
            action = BellBackgroundService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }

    /**
     * بدء خدمة مراقبة الموقع
     */
    fun startLocationService() {
        if (!settingsRepository.isLocationEnabled()) {
            return
        }

        val intent = Intent(context, com.mo.bell.location.LocationService::class.java).apply {
            action = com.mo.bell.location.LocationService.ACTION_START_LOCATION_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * إيقاف خدمة مراقبة الموقع
     */
    fun stopLocationService() {
        val intent = Intent(context, com.mo.bell.location.LocationService::class.java).apply {
            action = com.mo.bell.location.LocationService.ACTION_STOP_LOCATION_MONITORING
        }
        context.startService(intent)
    }

    /**
     * بدء جميع الخدمات المطلوبة
     */
    fun startAllServices() {
        startBackgroundService()
        
        if (settingsRepository.isLocationEnabled()) {
            startLocationService()
        }
    }

    /**
     * إيقاف جميع الخدمات
     */
    fun stopAllServices() {
        stopBackgroundService()
        stopLocationService()
    }

    /**
     * إعادة تشغيل الخدمات
     */
    fun restartServices() {
        stopAllServices()
        // انتظار قصير للتأكد من إيقاف الخدمات
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startAllServices()
        }, 1000)
    }

    /**
     * فحص ما إذا كانت خدمة العمل في الخلفية تعمل
     */
    fun isBackgroundServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BellBackgroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * فحص ما إذا كانت خدمة الموقع تعمل
     */
    fun isLocationServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (com.mo.bell.location.LocationService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * الحصول على حالة الخدمات
     */
    fun getServicesStatus(): ServicesStatus {
        return ServicesStatus(
            backgroundServiceRunning = isBackgroundServiceRunning(),
            locationServiceRunning = isLocationServiceRunning(),
            backgroundServiceEnabled = settingsRepository.isBackgroundServiceEnabled(),
            locationEnabled = settingsRepository.isLocationEnabled()
        )
    }
}

/**
 * حالة الخدمات
 */
data class ServicesStatus(
    val backgroundServiceRunning: Boolean,
    val locationServiceRunning: Boolean,
    val backgroundServiceEnabled: Boolean,
    val locationEnabled: Boolean
)

