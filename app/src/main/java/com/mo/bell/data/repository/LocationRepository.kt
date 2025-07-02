package com.mo.bell.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mo.bell.data.database.dao.LocationDao
import com.mo.bell.data.database.entities.Location as LocationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.*

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationDao: LocationDao
) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    /**
     * الحصول على الموقع الحالي
     */
    suspend fun getCurrentLocation(): CurrentLocation? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                CurrentLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy.toDouble(),
                    timestamp = location.time
                )
            } else {
                // محاولة الحصول على موقع جديد
                requestNewLocation()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * طلب موقع جديد
     */
    private suspend fun requestNewLocation(): CurrentLocation? {
        if (!hasLocationPermission()) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10000L // 10 ثواني
                ).apply {
                    setMaxUpdates(1)
                    setMaxUpdateDelayMillis(15000L) // 15 ثانية كحد أقصى
                }.build()

                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            val currentLocation = CurrentLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy.toDouble(),
                                timestamp = location.time
                            )
                            continuation.resume(currentLocation)
                        } else {
                            continuation.resume(null)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )

                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }

            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    /**
     * حساب المسافة بين نقطتين بالمتر
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // نصف قطر الأرض بالمتر

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * فحص ما إذا كان الموقع ضمن نطاق معين
     */
    suspend fun isWithinRadius(
        targetLatitude: Double,
        targetLongitude: Double,
        radiusMeters: Double
    ): Boolean {
        val currentLocation = getCurrentLocation() ?: return false
        
        val distance = calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            targetLatitude,
            targetLongitude
        )
        
        return distance <= radiusMeters
    }

    /**
     * حفظ موقع في قاعدة البيانات
     */
    suspend fun saveLocation(location: LocationEntity): Long {
        return locationDao.insertLocation(location)
    }

    /**
     * الحصول على آخر موقع محفوظ
     */
    suspend fun getLastSavedLocation(): LocationEntity? {
        return locationDao.getLastLocation()
    }

    /**
     * الحصول على جميع المواقع المحفوظة
     */
    fun getAllLocations(): Flow<List<LocationEntity>> {
        return locationDao.getAllLocations()
    }

    /**
     * حذف المواقع القديمة
     */
    suspend fun deleteOldLocations(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        locationDao.deleteOldLocations(cutoffTime)
    }

    /**
     * فحص صلاحيات الموقع
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * فحص ما إذا كان GPS مفعل
     */
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * فحص ما إذا كان موقع الشبكة مفعل
     */
    fun isNetworkLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * فحص ما إذا كان أي مزود موقع مفعل
     */
    fun isLocationEnabled(): Boolean {
        return isGpsEnabled() || isNetworkLocationEnabled()
    }

    /**
     * الحصول على دقة الموقع
     */
    suspend fun getLocationAccuracy(): LocationAccuracy {
        val location = getCurrentLocation()
        return when {
            location == null -> LocationAccuracy.UNAVAILABLE
            location.accuracy <= 5 -> LocationAccuracy.HIGH
            location.accuracy <= 20 -> LocationAccuracy.MEDIUM
            else -> LocationAccuracy.LOW
        }
    }

    /**
     * مراقبة تغييرات الموقع
     */
    suspend fun startLocationUpdates(
        intervalMs: Long = 30000L, // 30 ثانية
        onLocationUpdate: (CurrentLocation) -> Unit
    ) {
        if (!hasLocationPermission()) {
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMs
        ).build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val currentLocation = CurrentLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy.toDouble(),
                        timestamp = location.time
                    )
                    onLocationUpdate(currentLocation)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    /**
     * إيقاف مراقبة الموقع
     */
    fun stopLocationUpdates() {
        // سيتم تنفيذ هذا في الخدمة
    }
}

/**
 * الموقع الحالي
 */
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val timestamp: Long
)

/**
 * دقة الموقع
 */
enum class LocationAccuracy {
    HIGH,      // دقة عالية (أقل من 5 متر)
    MEDIUM,    // دقة متوسطة (5-20 متر)
    LOW,       // دقة منخفضة (أكثر من 20 متر)
    UNAVAILABLE // غير متوفر
}

