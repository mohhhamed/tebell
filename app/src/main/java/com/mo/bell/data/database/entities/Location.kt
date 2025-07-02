package com.mo.bell.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import kotlin.math.*

/**
 * كيان الموقع في قاعدة البيانات
 */
@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "school_latitude")
    val schoolLatitude: Double,
    
    @ColumnInfo(name = "school_longitude")
    val schoolLongitude: Double,
    
    @ColumnInfo(name = "radius")
    val radius: Int = 100, // بالمتر
    
    @ColumnInfo(name = "school_name")
    val schoolName: String? = null,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = LocalDateTime.now().toString(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = LocalDateTime.now().toString()
) {
    /**
     * حساب المسافة بين موقع المدرسة وموقع آخر بالمتر
     */
    fun distanceTo(latitude: Double, longitude: Double): Double {
        val earthRadius = 6371000.0 // نصف قطر الأرض بالمتر
        
        val lat1Rad = Math.toRadians(schoolLatitude)
        val lat2Rad = Math.toRadians(latitude)
        val deltaLatRad = Math.toRadians(latitude - schoolLatitude)
        val deltaLonRad = Math.toRadians(longitude - schoolLongitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * التحقق من أن الموقع المعطى داخل نطاق المدرسة
     */
    fun isWithinRange(latitude: Double, longitude: Double): Boolean {
        return distanceTo(latitude, longitude) <= radius
    }
    
    /**
     * الحصول على وصف الموقع
     */
    fun getLocationDescription(): String {
        return buildString {
            schoolName?.let { append("$it - ") }
            append("${schoolLatitude.format(6)}, ${schoolLongitude.format(6)}")
            address?.let { append(" ($it)") }
        }
    }
    
    /**
     * الحصول على رابط خرائط جوجل
     */
    fun getGoogleMapsUrl(): String {
        return "https://maps.google.com/?q=$schoolLatitude,$schoolLongitude"
    }
    
    /**
     * التحقق من صحة بيانات الموقع
     */
    fun isValid(): Boolean {
        return schoolLatitude in -90.0..90.0 &&
               schoolLongitude in -180.0..180.0 &&
               radius in 10..1000 // نطاق معقول للمدرسة
    }
    
    /**
     * الحصول على حالة الموقع بناءً على الموقع الحالي
     */
    fun getLocationStatus(currentLatitude: Double, currentLongitude: Double): LocationStatus {
        return if (!isActive) {
            LocationStatus.DISABLED
        } else {
            val distance = distanceTo(currentLatitude, currentLongitude)
            when {
                distance <= radius -> LocationStatus.INSIDE
                distance <= radius * 1.5 -> LocationStatus.NEARBY
                else -> LocationStatus.OUTSIDE
            }
        }
    }
    
    /**
     * إنشاء نسخة محدثة من الموقع
     */
    fun updated(
        schoolLatitude: Double = this.schoolLatitude,
        schoolLongitude: Double = this.schoolLongitude,
        radius: Int = this.radius,
        schoolName: String? = this.schoolName,
        address: String? = this.address,
        isActive: Boolean = this.isActive
    ): Location {
        return copy(
            schoolLatitude = schoolLatitude,
            schoolLongitude = schoolLongitude,
            radius = radius,
            schoolName = schoolName,
            address = address,
            isActive = isActive,
            updatedAt = LocalDateTime.now().toString()
        )
    }
    
    companion object {
        /**
         * إنشاء موقع جديد
         */
        fun create(
            latitude: Double,
            longitude: Double,
            radius: Int = 100,
            schoolName: String? = null,
            address: String? = null
        ): Location {
            return Location(
                schoolLatitude = latitude,
                schoolLongitude = longitude,
                radius = radius,
                schoolName = schoolName?.trim(),
                address = address?.trim()
            )
        }
        
        /**
         * الحد الأدنى والأقصى للنطاق
         */
        const val MIN_RADIUS = 10
        const val MAX_RADIUS = 1000
        const val DEFAULT_RADIUS = 100
        
        /**
         * التحقق من صحة النطاق
         */
        fun isValidRadius(radius: Int): Boolean {
            return radius in MIN_RADIUS..MAX_RADIUS
        }
        
        /**
         * التحقق من صحة الإحداثيات
         */
        fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
            return latitude in -90.0..90.0 && longitude in -180.0..180.0
        }
    }
}

/**
 * حالة الموقع
 */
enum class LocationStatus {
    INSIDE,    // داخل المدرسة
    NEARBY,    // قريب من المدرسة
    OUTSIDE,   // خارج المدرسة
    DISABLED,  // معطل
    UNKNOWN    // غير معروف
}

/**
 * معلومات الموقع الحالي
 */
data class CurrentLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * التحقق من دقة الموقع
     */
    fun isAccurate(maxAccuracy: Float = 50f): Boolean {
        return accuracy <= maxAccuracy
    }
    
    /**
     * التحقق من حداثة الموقع
     */
    fun isRecent(maxAgeMillis: Long = 60000): Boolean { // دقيقة واحدة
        return System.currentTimeMillis() - timestamp <= maxAgeMillis
    }
    
    /**
     * التحقق من صحة الموقع
     */
    fun isValid(): Boolean {
        return Location.isValidCoordinates(latitude, longitude) && 
               accuracy > 0 && 
               isRecent()
    }
}

/**
 * إعدادات الموقع
 */
data class LocationSettings(
    val isEnabled: Boolean = true,
    val radius: Int = Location.DEFAULT_RADIUS,
    val enableNotifications: Boolean = true,
    val enableWelcomeMessage: Boolean = true,
    val enableGoodbyeMessage: Boolean = true,
    val updateIntervalMillis: Long = 30000, // 30 ثانية
    val minDistanceMeters: Float = 10f
)

/**
 * مساعد تنسيق الأرقام
 */
private fun Double.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

