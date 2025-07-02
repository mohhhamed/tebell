package com.mo.bell.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.mo.bell.data.database.entities.Location
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الوصول لبيانات الموقع
 */
@Dao
interface LocationDao {
    
    /**
     * إدراج موقع جديد
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Location): Long
    
    /**
     * إدراج عدة مواقع
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<Location>): List<Long>
    
    /**
     * تحديث موقع
     */
    @Update
    suspend fun updateLocation(location: Location): Int
    
    /**
     * حذف موقع
     */
    @Delete
    suspend fun deleteLocation(location: Location): Int
    
    /**
     * حذف موقع بالمعرف
     */
    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: Long): Int
    
    /**
     * حذف جميع المواقع
     */
    @Query("DELETE FROM locations")
    suspend fun deleteAllLocations(): Int
    
    /**
     * الحصول على موقع بالمعرف
     */
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): Location?
    
    /**
     * الحصول على موقع بالمعرف كـ LiveData
     */
    @Query("SELECT * FROM locations WHERE id = :locationId")
    fun getLocationByIdLiveData(locationId: Long): LiveData<Location?>
    
    /**
     * الحصول على موقع بالمعرف كـ Flow
     */
    @Query("SELECT * FROM locations WHERE id = :locationId")
    fun getLocationByIdFlow(locationId: Long): Flow<Location?>
    
    /**
     * الحصول على الموقع النشط
     */
    @Query("SELECT * FROM locations WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1")
    suspend fun getActiveLocation(): Location?
    
    /**
     * الحصول على الموقع النشط كـ LiveData
     */
    @Query("SELECT * FROM locations WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1")
    fun getActiveLocationLiveData(): LiveData<Location?>
    
    /**
     * الحصول على الموقع النشط كـ Flow
     */
    @Query("SELECT * FROM locations WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1")
    fun getActiveLocationFlow(): Flow<Location?>
    
    /**
     * الحصول على جميع المواقع
     */
    @Query("SELECT * FROM locations ORDER BY created_at DESC")
    suspend fun getAllLocations(): List<Location>
    
    /**
     * الحصول على جميع المواقع كـ LiveData
     */
    @Query("SELECT * FROM locations ORDER BY created_at DESC")
    fun getAllLocationsLiveData(): LiveData<List<Location>>
    
    /**
     * الحصول على جميع المواقع كـ Flow
     */
    @Query("SELECT * FROM locations ORDER BY created_at DESC")
    fun getAllLocationsFlow(): Flow<List<Location>>
    
    /**
     * الحصول على المواقع النشطة فقط
     */
    @Query("SELECT * FROM locations WHERE is_active = 1 ORDER BY created_at DESC")
    suspend fun getActiveLocations(): List<Location>
    
    /**
     * الحصول على المواقع حسب اسم المدرسة
     */
    @Query("SELECT * FROM locations WHERE school_name = :schoolName ORDER BY created_at DESC")
    suspend fun getLocationsBySchoolName(schoolName: String): List<Location>
    
    /**
     * البحث في المواقع
     */
    @Query("SELECT * FROM locations WHERE school_name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY created_at DESC")
    suspend fun searchLocations(query: String): List<Location>
    
    /**
     * العثور على المواقع القريبة
     */
    @Query("""
        SELECT *, 
        (6371000 * acos(cos(radians(:latitude)) * cos(radians(school_latitude)) * 
        cos(radians(school_longitude) - radians(:longitude)) + 
        sin(radians(:latitude)) * sin(radians(school_latitude)))) AS distance
        FROM locations 
        WHERE is_active = 1
        HAVING distance <= :maxDistanceMeters
        ORDER BY distance ASC
    """)
    suspend fun getNearbyLocations(latitude: Double, longitude: Double, maxDistanceMeters: Double): List<LocationWithDistance>
    
    /**
     * التحقق من وجود موقع ضمن نطاق معين
     */
    @Query("""
        SELECT COUNT(*) FROM locations 
        WHERE is_active = 1
        AND (6371000 * acos(cos(radians(:latitude)) * cos(radians(school_latitude)) * 
        cos(radians(school_longitude) - radians(:longitude)) + 
        sin(radians(:latitude)) * sin(radians(school_latitude)))) <= radius
    """)
    suspend fun countLocationsWithinRange(latitude: Double, longitude: Double): Int
    
    /**
     * الحصول على أقرب موقع نشط
     */
    @Query("""
        SELECT *, 
        (6371000 * acos(cos(radians(:latitude)) * cos(radians(school_latitude)) * 
        cos(radians(school_longitude) - radians(:longitude)) + 
        sin(radians(:latitude)) * sin(radians(school_latitude)))) AS distance
        FROM locations 
        WHERE is_active = 1
        ORDER BY distance ASC
        LIMIT 1
    """)
    suspend fun getClosestActiveLocation(latitude: Double, longitude: Double): LocationWithDistance?
    
    /**
     * تفعيل موقع
     */
    @Query("UPDATE locations SET is_active = 1, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun activateLocation(locationId: Long, updatedAt: String): Int
    
    /**
     * إلغاء تفعيل موقع
     */
    @Query("UPDATE locations SET is_active = 0, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun deactivateLocation(locationId: Long, updatedAt: String): Int
    
    /**
     * إلغاء تفعيل جميع المواقع
     */
    @Query("UPDATE locations SET is_active = 0, updated_at = :updatedAt")
    suspend fun deactivateAllLocations(updatedAt: String): Int
    
    /**
     * تحديث إحداثيات الموقع
     */
    @Query("UPDATE locations SET school_latitude = :latitude, school_longitude = :longitude, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun updateLocationCoordinates(locationId: Long, latitude: Double, longitude: Double, updatedAt: String): Int
    
    /**
     * تحديث نطاق الموقع
     */
    @Query("UPDATE locations SET radius = :radius, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun updateLocationRadius(locationId: Long, radius: Int, updatedAt: String): Int
    
    /**
     * تحديث اسم المدرسة
     */
    @Query("UPDATE locations SET school_name = :schoolName, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun updateSchoolName(locationId: Long, schoolName: String?, updatedAt: String): Int
    
    /**
     * تحديث العنوان
     */
    @Query("UPDATE locations SET address = :address, updated_at = :updatedAt WHERE id = :locationId")
    suspend fun updateAddress(locationId: Long, address: String?, updatedAt: String): Int
    
    /**
     * الحصول على عدد المواقع
     */
    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationsCount(): Int
    
    /**
     * الحصول على عدد المواقع النشطة
     */
    @Query("SELECT COUNT(*) FROM locations WHERE is_active = 1")
    suspend fun getActiveLocationsCount(): Int
    
    /**
     * التحقق من وجود موقع نشط
     */
    @Query("SELECT EXISTS(SELECT 1 FROM locations WHERE is_active = 1)")
    suspend fun hasActiveLocation(): Boolean
    
    /**
     * التحقق من وجود موقع بإحداثيات معينة
     */
    @Query("SELECT EXISTS(SELECT 1 FROM locations WHERE school_latitude = :latitude AND school_longitude = :longitude)")
    suspend fun locationExistsByCoordinates(latitude: Double, longitude: Double): Boolean
    
    /**
     * الحصول على آخر موقع تم إضافته
     */
    @Query("SELECT * FROM locations ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestLocation(): Location?
    
    /**
     * الحصول على أسماء جميع المدارس
     */
    @Query("SELECT DISTINCT school_name FROM locations WHERE school_name IS NOT NULL ORDER BY school_name ASC")
    suspend fun getAllSchoolNames(): List<String>
    
    /**
     * الحصول على متوسط النطاق
     */
    @Query("SELECT AVG(radius) FROM locations WHERE is_active = 1")
    suspend fun getAverageRadius(): Double?
    
    /**
     * الحصول على أكبر وأصغر نطاق
     */
    @Query("SELECT MIN(radius) as minRadius, MAX(radius) as maxRadius FROM locations WHERE is_active = 1")
    suspend fun getRadiusRange(): RadiusRange?
    
    /**
     * إحصائيات المواقع
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN is_active = 1 THEN 1 END) as active,
            COUNT(DISTINCT school_name) as schools,
            AVG(radius) as averageRadius,
            MIN(radius) as minRadius,
            MAX(radius) as maxRadius
        FROM locations
    """)
    suspend fun getLocationStatistics(): LocationStatistics
    
    /**
     * تنظيف المواقع القديمة غير النشطة
     */
    @Query("DELETE FROM locations WHERE is_active = 0 AND datetime(created_at) < datetime('now', '-30 days')")
    suspend fun cleanupOldInactiveLocations(): Int
}

/**
 * موقع مع المسافة
 */
data class LocationWithDistance(
    val id: Long,
    val schoolLatitude: Double,
    val schoolLongitude: Double,
    val radius: Int,
    val schoolName: String?,
    val address: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val distance: Double
)

/**
 * نطاق النصف قطر
 */
data class RadiusRange(
    val minRadius: Int,
    val maxRadius: Int
)

/**
 * إحصائيات المواقع
 */
data class LocationStatistics(
    val total: Int,
    val active: Int,
    val schools: Int,
    val averageRadius: Double,
    val minRadius: Int,
    val maxRadius: Int
)

