package com.mo.bell.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.gson.Gson
import com.mo.bell.data.Lesson
import com.mo.bell.data.TeacherSchedule
import com.mo.bell.databinding.ActivityMainBinding
import com.mo.bell.services.LocationService
import com.mo.bell.utils.NotificationHelper
import com.mo.bell.utils.SettingsManager
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager

    private val backgroundLocationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "بدون إذن الموقع في الخلفية، قد لا تعمل التنبيهات عندما يكون التطبيق مغلقاً", Toast.LENGTH_LONG).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AlertDialog.Builder(this)
                    .setTitle("إذن إضافي مطلوب")
                    .setMessage("لضمان عمل التنبيهات حتى عندما يكون التطبيق في الخلفية، نحتاج إلى إذن 'الموقع طوال الوقت'.")
                    .setPositiveButton("فهمت، متابعة") { _, _ ->
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    .setNegativeButton("إلغاء", null)
                    .show()
            }
        } else {
            Toast.makeText(this, "إذن الموقع الدقيق ضروري لعمل التطبيق", Toast.LENGTH_LONG).show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissions[Manifest.permission.POST_NOTIFICATIONS] == false) {
                Toast.makeText(this, "إذن الإشعارات ضروري لتلقي تنبيهات الحصص", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        Log.d("JsonDebug", "File picker returned. URI: $uri")
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = InputStreamReader(inputStream)
                Log.d("JsonDebug", "InputStream is ready. Starting to parse JSON...")
                val teacherSchedule = Gson().fromJson(reader, TeacherSchedule::class.java)
                if (teacherSchedule?.teacherName == null || teacherSchedule.schedule == null) {
                    throw IllegalStateException("Parsed object or its properties are null. Check JSON structure.")
                }
                Log.d("JsonDebug", "JSON parsed successfully! Teacher: ${teacherSchedule.teacherName}")
                settingsManager.save(SettingsManager.KEY_TEACHER_NAME, teacherSchedule.teacherName)
                settingsManager.save(SettingsManager.KEY_SCHEDULE_JSON, teacherSchedule.schedule)
                updateUI()
                Toast.makeText(this, "تم تحميل جدول الأستاذ: ${teacherSchedule.teacherName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("JsonDebug", "Error processing or parsing JSON file.", e)
                Toast.makeText(this, "حدث خطأ في قراءة أو تحليل الملف!", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w("JsonDebug", "URI is null. User likely cancelled.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settingsManager = SettingsManager(this)
        NotificationHelper.createNotificationChannels(this)
        setupListeners()
        updateUI()
        checkAndRequestInitialPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLessonUI()
    }

    private fun setupListeners() {
        binding.btnLoadSchedule.setOnClickListener { openDocumentLauncher.launch("application/json") }
        binding.btnSetSchoolLocation.setOnClickListener { startLocationServiceWithCheck() }
        binding.btnViewSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
        binding.btnSetRingtone.setOnClickListener {
            showRingtoneSelectionDialog()
        }
        binding.etSchoolName.doOnTextChanged { text, _, _, _ ->
            settingsManager.save(SettingsManager.KEY_SCHOOL_NAME, text.toString())
        }
        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.save(SettingsManager.KEY_VIBRATE, isChecked)
        }
        binding.sliderRadius.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                settingsManager.save(SettingsManager.KEY_GEOFENCE_RADIUS, value)
            }
        }
    }

    private fun updateUI() {
        val teacherName = settingsManager.getTeacherName()
        binding.tvWelcome.text = if (teacherName.isNotBlank()) "مرحباً, $teacherName" else "مرحباً في تطبيق الجرس"
        binding.etSchoolName.setText(settingsManager.getSchoolName())
        binding.tvSchoolName.text = "مدرسة: ${settingsManager.getSchoolName().ifBlank { "لم تحدد" }}"
        binding.switchVibrate.isChecked = settingsManager.isVibrateEnabled()
        val savedRingtone = settingsManager.getAppRingtoneName()
        val ringtoneDisplayName = if (savedRingtone != null) {
            savedRingtone.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "صامت"
        }
        binding.tvRingtoneInfo.text = "النغمة: $ringtoneDisplayName"
        binding.sliderRadius.value = settingsManager.getGeofenceRadius()
        updateCurrentLessonUI()
    }

    private fun showRingtoneSelectionDialog() {
        val ringtoneDisplayNames = arrayOf("جرس المدرسة", "تنبيه هادئ", "رنة رقمية", "صامت")
        val ringtoneFileNames = arrayOf("school_bell", "calm_notification", "digital_ping", "silent")
        val currentRingtone = settingsManager.getAppRingtoneName()
        val checkedItem = ringtoneFileNames.indexOf(currentRingtone).let { if (it == -1) 3 else it }

        AlertDialog.Builder(this)
            .setTitle("اختر نغمة التنبيه")
            .setSingleChoiceItems(ringtoneDisplayNames, checkedItem) { dialog, which ->
                val selectedFileName = if (ringtoneFileNames[which] == "silent") null else ringtoneFileNames[which]
                settingsManager.save(SettingsManager.KEY_APP_RINGTONE_NAME, selectedFileName)
                updateUI()
                dialog.dismiss()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun getArabicDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الاثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            Calendar.FRIDAY -> "الجمعة"
            Calendar.SATURDAY -> "السبت"
            else -> ""
        }
    }

    private fun updateCurrentLessonUI() {
        val schedule = settingsManager.getSchedule()
        if (schedule.isEmpty()) return
        Log.d("LessonDebug", "--- بدء تحديث واجهة الدرس ---")
        val nowCalendar = Calendar.getInstance()
        val dayOfWeekArabic = getArabicDayName(nowCalendar.get(Calendar.DAY_OF_WEEK))
        Log.d("LessonDebug", "اليوم المحسوب من النظام هو: '$dayOfWeekArabic'")
        Log.d("LessonDebug", "الجدول الكامل المحفوظ: ${schedule.joinToString()}")
        val todayLessons = schedule
            .filter { it.day.equals(dayOfWeekArabic, ignoreCase = true) }
            .sortedBy { it.startTime }
        Log.d("LessonDebug", "عدد الحصص التي تم العثور عليها لهذا اليوم ('$dayOfWeekArabic'): ${todayLessons.size}")
        var currentLesson: Lesson? = null
        var nextLesson: Lesson? = null
        for (lesson in todayLessons) {
            try {
                val startParts = lesson.startTime.split(":")
                val startCalendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startParts[0].toInt()); set(Calendar.MINUTE, startParts[1].toInt()); set(Calendar.SECOND, 0) }
                val endParts = lesson.endTime.split(":")
                val endCalendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, endParts[0].toInt()); set(Calendar.MINUTE, endParts[1].toInt()); set(Calendar.SECOND, 59) }
                if (nowCalendar.after(startCalendar) && nowCalendar.before(endCalendar)) { currentLesson = lesson }
                if (nowCalendar.before(startCalendar) && nextLesson == null) { nextLesson = lesson }
            } catch (e: Exception) { Log.e("LessonDebug", "خطأ في تحليل الوقت للحصة: ${lesson.period}", e) }
        }
        Log.d("LessonDebug", "الدرس الحالي: ${currentLesson?.period ?: "لا يوجد"}. الدرس القادم: ${nextLesson?.period ?: "لا يوجد"}")
        binding.tvCurrentLesson.text = currentLesson?.let { "الحصة ${it.period} (${it.startTime} - ${it.endTime})" } ?: "لا توجد حصة حالياً"
        binding.tvNextLesson.text = nextLesson?.let { "الحصة ${it.period} (${it.startTime})" } ?: "لا توجد حصص قادمة اليوم"
    }

    private fun checkAndRequestInitialPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startLocationServiceWithCheck() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else { true }
        if (fineLocationGranted && backgroundLocationGranted) {
            if (settingsManager.getSchoolName().isBlank()) {
                Toast.makeText(this, "يرجى كتابة اسم المدرسة أولاً", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(this, LocationService::class.java).apply { action = LocationService.ACTION_START_GEOFENCE }
            ContextCompat.startForegroundService(this, intent)
            Toast.makeText(this, "جاري حفظ الموقع وتفعيل التنبيهات...", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "يرجى منح أذونات الموقع المطلوبة (خصوصاً 'السماح طوال الوقت')", Toast.LENGTH_LONG).show()
            checkAndRequestInitialPermissions()
        }
    }
}