package com.mo.bell.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mo.bell.R
import com.mo.bell.databinding.ActivityMainBinding
import com.mo.bell.ui.schedule.ScheduleActivity
import com.mo.bell.ui.settings.SettingsActivity
import com.mo.bell.ui.import.ImportActivity
import com.mo.bell.ui.sound.SoundActivity
import com.mo.bell.utils.QRCodeScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var qrCodeScanner: QRCodeScanner

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
        } else {
            showLocationPermissionDeniedMessage()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationPermissionGranted()
        } else {
            showNotificationPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        setupQRCodeScanner()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupViews() {
        // إعداد بطاقات الخيارات الرئيسية
        binding.scheduleCard.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        binding.importCard.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }

        binding.soundCard.setOnClickListener {
            startActivity(Intent(this, SoundActivity::class.java))
        }

        binding.settingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // زر الإجراء العائم لمسح QR Code
        binding.fab.setOnClickListener {
            qrCodeScanner.startScan()
        }

        // زر إعداد الموقع
        binding.locationSettingsButton.setOnClickListener {
            requestLocationPermissions()
        }
    }

    private fun setupObservers() {
        // مراقبة الحصة الحالية
        lifecycleScope.launch {
            viewModel.currentClass.collect { currentClass ->
                updateCurrentClassUI(currentClass)
            }
        }

        // مراقبة الحصة القادمة
        lifecycleScope.launch {
            viewModel.nextClass.collect { nextClass ->
                updateNextClassUI(nextClass)
            }
        }

        // مراقبة حالة الموقع
        lifecycleScope.launch {
            viewModel.locationStatus.collect { status ->
                updateLocationStatusUI(status)
            }
        }

        // مراقبة الأخطاء
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    showErrorMessage(it)
                    viewModel.clearError()
                }
            }
        }

        // مراقبة حالة التحميل
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // يمكن إضافة مؤشر تحميل هنا
            }
        }
    }

    private fun setupQRCodeScanner() {
        qrCodeScanner = QRCodeScanner(this) { result ->
            viewModel.handleQRCodeResult(result)
        }
    }

    private fun updateCurrentClassUI(currentClass: com.mo.bell.data.models.CurrentClass?) {
        if (currentClass != null) {
            binding.currentClassTitle.text = currentClass.className
            binding.currentClassTime.text = "${currentClass.startTime} - ${currentClass.endTime}"
            
            // إظهار شريط التقدم
            binding.currentClassProgress.visibility = android.view.View.VISIBLE
            binding.classProgressBar.progress = currentClass.progressPercentage
            binding.classProgressText.text = getString(
                R.string.class_progress_format,
                currentClass.remainingMinutes
            )
        } else {
            binding.currentClassTitle.text = getString(R.string.no_current_class)
            binding.currentClassTime.text = ""
            binding.currentClassProgress.visibility = android.view.View.GONE
        }
    }

    private fun updateNextClassUI(nextClass: com.mo.bell.data.models.NextClass?) {
        if (nextClass != null) {
            binding.nextClassTitle.text = nextClass.className
            binding.nextClassTime.text = "${nextClass.startTime} - ${nextClass.endTime}"
            
            // إظهار العد التنازلي
            binding.nextClassCountdown.visibility = android.view.View.VISIBLE
            binding.nextClassCountdown.text = getString(
                R.string.next_class_countdown_format,
                nextClass.minutesUntilStart
            )
        } else {
            binding.nextClassTitle.text = getString(R.string.no_next_class)
            binding.nextClassTime.text = ""
            binding.nextClassCountdown.visibility = android.view.View.GONE
        }
    }

    private fun updateLocationStatusUI(status: LocationStatus) {
        when (status) {
            LocationStatus.UNKNOWN -> {
                binding.locationStatusText.text = getString(R.string.location_unknown)
                binding.locationStatusIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.warning)
                )
                binding.locationSettingsButton.visibility = android.view.View.VISIBLE
            }
            LocationStatus.AT_SCHOOL -> {
                binding.locationStatusText.text = getString(R.string.at_school)
                binding.locationStatusIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.success)
                )
                binding.locationSettingsButton.visibility = android.view.View.GONE
            }
            LocationStatus.AWAY_FROM_SCHOOL -> {
                binding.locationStatusText.text = getString(R.string.away_from_school)
                binding.locationStatusIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.error)
                )
                binding.locationSettingsButton.visibility = android.view.View.GONE
            }
            LocationStatus.DISABLED -> {
                binding.locationStatusText.text = getString(R.string.location_disabled)
                binding.locationStatusIcon.setColorFilter(
                    ContextCompat.getColor(this, R.color.disabled)
                )
                binding.locationSettingsButton.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun checkPermissions() {
        // فحص صلاحية الموقع
        if (!hasLocationPermission()) {
            viewModel.setLocationStatus(LocationStatus.DISABLED)
        }

        // فحص صلاحية الإشعارات (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                requestNotificationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showLocationPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            getString(R.string.location_permission_denied),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.settings)) {
            // فتح إعدادات التطبيق
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }.show()
    }

    private fun showNotificationPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            getString(R.string.notification_permission_denied),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshData()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.about))
            .setMessage(getString(R.string.about_message))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.help))
            .setMessage(getString(R.string.help_message))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

enum class LocationStatus {
    UNKNOWN,
    AT_SCHOOL,
    AWAY_FROM_SCHOOL,
    DISABLED
}

