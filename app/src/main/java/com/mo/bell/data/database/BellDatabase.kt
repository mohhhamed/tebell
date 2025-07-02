package com.mo.bell.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.mo.bell.data.database.dao.*
import com.mo.bell.data.database.entities.*

/**
 * قاعدة البيانات الرئيسية للتطبيق
 */
@Database(
    entities = [
        Teacher::class,
        Schedule::class,
        Settings::class,
        Location::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BellDatabase : RoomDatabase() {
    
    // واجهات DAO
    abstract fun teacherDao(): TeacherDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun settingsDao(): SettingsDao
    abstract fun locationDao(): LocationDao
    
    companion object {
        const val DATABASE_NAME = "bell_database"
        
        @Volatile
        private var INSTANCE: BellDatabase? = null
        
        /**
         * الحصول على مثيل قاعدة البيانات
         */
        fun getDatabase(context: Context): BellDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BellDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * إغلاق قاعدة البيانات
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
        
        /**
         * Migration من الإصدار 1 إلى 2 (للاستخدام المستقبلي)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // إضافة أعمدة جديدة أو تعديل الجداول هنا
                // مثال:
                // database.execSQL("ALTER TABLE teachers ADD COLUMN email TEXT")
            }
        }
    }
    
    /**
     * Callback لتهيئة قاعدة البيانات
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // تهيئة البيانات الافتراضية
            // يمكن استخدام Executor أو Coroutine لتشغيل العمليات غير المتزامنة
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // تشغيل عمليات عند فتح قاعدة البيانات
        }
    }
}

/**
 * محولات الأنواع لقاعدة البيانات
 */
class Converters {
    // يمكن إضافة محولات هنا إذا احتجنا لتحويل أنواع معقدة
    // مثل Date أو List إلى String والعكس
    
    // مثال:
    // @TypeConverter
    // fun fromTimestamp(value: Long?): Date? {
    //     return value?.let { Date(it) }
    // }
    //
    // @TypeConverter
    // fun dateToTimestamp(date: Date?): Long? {
    //     return date?.time
    // }
}

/**
 * مساعد لتهيئة البيانات الافتراضية
 */
object DatabaseInitializer {
    
    /**
     * تهيئة الإعدادات الافتراضية
     */
    suspend fun initializeDefaultSettings(database: BellDatabase) {
        val settingsDao = database.settingsDao()
        
        // التحقق من وجود إعدادات
        val settingsCount = settingsDao.getSettingsCount()
        if (settingsCount == 0) {
            // إدراج الإعدادات الافتراضية
            val defaultSettings = Settings.createDefaults()
            settingsDao.insertSettings(defaultSettings)
        }
    }
    
    /**
     * تهيئة بيانات تجريبية (للتطوير فقط)
     */
    suspend fun initializeSampleData(database: BellDatabase) {
        val teacherDao = database.teacherDao()
        val scheduleDao = database.scheduleDao()
        
        // التحقق من وجود بيانات
        val teachersCount = teacherDao.getTeachersCount()
        if (teachersCount == 0) {
            // إنشاء مدرس تجريبي
            val sampleTeacher = Teacher.create(
                name = "أحمد محمد",
                schoolName = "مدرسة النور الابتدائية"
            )
            val teacherId = teacherDao.insertTeacher(sampleTeacher)
            
            // إنشاء جدول تجريبي
            val sampleSchedules = listOf(
                Schedule.create(
                    teacherId = teacherId,
                    day = "الأحد",
                    period = 1,
                    startTime = "08:00",
                    endTime = "08:40",
                    className = "الصف الأول - أ",
                    subjectName = "رياضيات"
                ),
                Schedule.create(
                    teacherId = teacherId,
                    day = "الأحد",
                    period = 2,
                    startTime = "08:45",
                    endTime = "09:25",
                    className = "الصف الثاني - ب",
                    subjectName = "علوم"
                ),
                Schedule.create(
                    teacherId = teacherId,
                    day = "الاثنين",
                    period = 1,
                    startTime = "08:00",
                    endTime = "08:40",
                    className = "الصف الثالث - أ",
                    subjectName = "لغة عربية"
                )
            )
            scheduleDao.insertSchedules(sampleSchedules)
        }
    }
    
    /**
     * تنظيف البيانات القديمة
     */
    suspend fun cleanupOldData(database: BellDatabase) {
        val locationDao = database.locationDao()
        
        // حذف المواقع القديمة غير النشطة
        locationDao.cleanupOldInactiveLocations()
    }
    
    /**
     * تحديث إصدار قاعدة البيانات
     */
    suspend fun updateDatabaseVersion(database: BellDatabase, newVersion: String) {
        val settingsDao = database.settingsDao()
        val currentTime = java.time.LocalDateTime.now().toString()
        
        settingsDao.upsertSetting(
            key = Settings.Keys.APP_VERSION,
            value = newVersion,
            type = SettingType.STRING,
            description = "إصدار التطبيق",
            updatedAt = currentTime
        )
    }
    
    /**
     * إعادة تعيين قاعدة البيانات
     */
    suspend fun resetDatabase(database: BellDatabase) {
        database.clearAllTables()
        initializeDefaultSettings(database)
    }
    
    /**
     * نسخ احتياطي من البيانات
     */
    suspend fun backupData(database: BellDatabase): DatabaseBackup {
        val teacherDao = database.teacherDao()
        val scheduleDao = database.scheduleDao()
        val settingsDao = database.settingsDao()
        val locationDao = database.locationDao()
        
        return DatabaseBackup(
            teachers = teacherDao.getAllTeachers(),
            schedules = scheduleDao.getAllSchedules(),
            settings = settingsDao.getAllSettings(),
            locations = locationDao.getAllLocations(),
            backupTime = System.currentTimeMillis()
        )
    }
    
    /**
     * استعادة البيانات من النسخة الاحتياطية
     */
    suspend fun restoreData(database: BellDatabase, backup: DatabaseBackup) {
        val teacherDao = database.teacherDao()
        val scheduleDao = database.scheduleDao()
        val settingsDao = database.settingsDao()
        val locationDao = database.locationDao()
        
        // حذف البيانات الحالية
        database.clearAllTables()
        
        // استعادة البيانات
        teacherDao.insertTeachers(backup.teachers)
        scheduleDao.insertSchedules(backup.schedules)
        settingsDao.insertSettings(backup.settings)
        locationDao.insertLocations(backup.locations)
    }
}

/**
 * نموذج النسخة الاحتياطية
 */
data class DatabaseBackup(
    val teachers: List<Teacher>,
    val schedules: List<Schedule>,
    val settings: List<Settings>,
    val locations: List<Location>,
    val backupTime: Long
)

/**
 * إضافة دالة مساعدة للحصول على جميع الجداول
 */
private suspend fun ScheduleDao.getAllSchedules(): List<Schedule> {
    // هذه دالة مساعدة، يجب إضافتها إلى ScheduleDao إذا لم تكن موجودة
    // return getAllSchedules()
    return emptyList() // placeholder
}

