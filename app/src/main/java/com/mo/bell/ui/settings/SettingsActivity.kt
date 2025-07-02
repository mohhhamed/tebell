package com.mo.bell.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mo.bell.R
import com.mo.bell.databinding.ActivitySettingsBinding
import com.mo.bell.ui.location.LocationActivity
import com.mo.bell.ui.sound.SoundActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        // الخدمة في الخلفية
        binding.backgroundServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBackgroundServiceEnabled(isChecked)
        }

        // الوضع اليدوي
        binding.manualModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setManualModeEnabled(isChecked)
        }

        // الوضع الليلي
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkModeEnabled(isChecked)
        }

        // إعدادات الصوت
        binding.soundSettingsCard.setOnClickListener {
            startActivity(Intent(this, SoundActivity::class.java))
        }

        // إعدادات الموقع
        binding.locationSettingsCard.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        // معلومات المدرس
        binding.teacherInfoCard.setOnClickListener {
            showTeacherInfoDialog()
        }

        // حول التطبيق
        binding.aboutCard.setOnClickListener {
            showAboutDialog()
        }

        // إعادة تعيين الإعدادات
        binding.resetSettingsButton.setOnClickListener {
            showResetConfirmationDialog()
        }

        // تصدير الإعدادات
        binding.exportSettingsButton.setOnClickListener {
            viewModel.exportSettings()
        }

        // استيراد الإعدادات
        binding.importSettingsButton.setOnClickListener {
            viewModel.importSettings()
        }
    }

    private fun setupObservers() {
        // مراقبة حالة الخدمة في الخلفية
        lifecycleScope.launch {
            viewModel.backgroundServiceEnabled.collect { enabled ->
                binding.backgroundServiceSwitch.isChecked = enabled
                updateServiceStatus(enabled)
            }
        }

        // مراقبة الوضع اليدوي
        lifecycleScope.launch {
            viewModel.manualModeEnabled.collect { enabled ->
                binding.manualModeSwitch.isChecked = enabled
            }
        }

        // مراقبة الوضع الليلي
        lifecycleScope.launch {
            viewModel.darkModeEnabled.collect { enabled ->
                binding.darkModeSwitch.isChecked = enabled
            }
        }

        // مراقبة معلومات المدرس
        lifecycleScope.launch {
            viewModel.teacherInfo.collect { teacher ->
                if (teacher != null) {
                    binding.teacherNameText.text = teacher.name
                    binding.teacherSubjectText.text = teacher.subject
                } else {
                    binding.teacherNameText.text = getString(R.string.teacher_name_not_set)
                    binding.teacherSubjectText.text = getString(R.string.teacher_subject_not_set)
                }
            }
        }

        // مراقبة حالة الخدمات
        lifecycleScope.launch {
            viewModel.servicesStatus.collect { status ->
                updateServicesStatusUI(status)
            }
        }

        // مراقبة الرسائل
        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    showMessage(it)
                    viewModel.clearMessage()
                }
            }
        }
    }

    private fun updateServiceStatus(enabled: Boolean) {
        if (enabled) {
            binding.serviceStatusText.text = getString(R.string.service_enabled)
            binding.serviceStatusText.setTextColor(getColor(R.color.success_color))
        } else {
            binding.serviceStatusText.text = getString(R.string.service_disabled)
            binding.serviceStatusText.setTextColor(getColor(R.color.error_color))
        }
    }

    private fun updateServicesStatusUI(status: com.mo.bell.service.ServicesStatus) {
        val statusText = buildString {
            append("الخدمة الرئيسية: ")
            append(if (status.backgroundServiceRunning) "تعمل" else "متوقفة")
            append("\n")
            append("خدمة الموقع: ")
            append(if (status.locationServiceRunning) "تعمل" else "متوقفة")
        }
        
        binding.servicesStatusText.text = statusText
    }

    private fun showTeacherInfoDialog() {
        val dialogBinding = com.mo.bell.databinding.DialogTeacherInfoBinding.inflate(layoutInflater)
        
        // تعبئة البيانات الحالية
        lifecycleScope.launch {
            val teacher = viewModel.teacherInfo.value
            if (teacher != null) {
                dialogBinding.teacherNameEdit.setText(teacher.name)
                dialogBinding.teacherSubjectEdit.setText(teacher.subject)
                dialogBinding.teacherEmailEdit.setText(teacher.email)
                dialogBinding.teacherPhoneEdit.setText(teacher.phone)
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.teacher_info))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = dialogBinding.teacherNameEdit.text.toString()
                val subject = dialogBinding.teacherSubjectEdit.text.toString()
                val email = dialogBinding.teacherEmailEdit.text.toString()
                val phone = dialogBinding.teacherPhoneEdit.text.toString()
                
                viewModel.updateTeacherInfo(name, subject, email, phone)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAboutDialog() {
        val aboutText = """
            تطبيق Bell للمدرسين
            الإصدار: 1.0.0
            
            تطبيق ذكي لإدارة الجداول المدرسية والتنبيهات الصوتية
            
            الميزات:
            • إدارة الجداول الأسبوعية
            • التنبيهات الصوتية التلقائية
            • مراقبة الموقع الجغرافي
            • العمل في الخلفية
            • واجهة عربية كاملة
            
            تم التطوير بواسطة: فريق Bell
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_app))
            .setMessage(aboutText)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.reset_settings))
            .setMessage(getString(R.string.reset_settings_confirmation))
            .setPositiveButton(getString(R.string.reset)) { _, _ ->
                viewModel.resetAllSettings()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServicesStatus()
    }
}

