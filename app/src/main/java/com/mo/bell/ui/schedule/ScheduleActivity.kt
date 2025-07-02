package com.mo.bell.ui.schedule

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mo.bell.R
import com.mo.bell.databinding.ActivityScheduleBinding
import com.mo.bell.ui.settings.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private val viewModel: ScheduleViewModel by viewModels()
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.weekly_schedule)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        // إعداد قائمة الجدول
        scheduleAdapter = ScheduleAdapter { classItem ->
            // يمكن إضافة إجراءات عند النقر على الحصة
            showClassDetails(classItem)
        }
        
        binding.scheduleRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScheduleActivity)
            adapter = scheduleAdapter
        }

        // إعداد أزرار الأيام
        setupDayButtons()

        // إعداد زر التحديث
        binding.refreshButton.setOnClickListener {
            viewModel.refreshSchedule()
        }

        // إعداد زر الاستيراد
        binding.importButton.setOnClickListener {
            showImportOptions()
        }

        // إعداد زر التصدير
        binding.exportButton.setOnClickListener {
            viewModel.exportSchedule()
        }
    }

    private fun setupDayButtons() {
        val dayButtons = listOf(
            binding.sundayButton,
            binding.mondayButton,
            binding.tuesdayButton,
            binding.wednesdayButton,
            binding.thursdayButton,
            binding.fridayButton,
            binding.saturdayButton
        )

        dayButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.selectDay(index + 1) // الأحد = 1
            }
        }
    }

    private fun setupObservers() {
        // مراقبة اليوم المحدد
        lifecycleScope.launch {
            viewModel.selectedDay.collect { day ->
                updateDayButtonsSelection(day)
            }
        }

        // مراقبة جدول اليوم
        lifecycleScope.launch {
            viewModel.daySchedule.collect { schedule ->
                scheduleAdapter.submitList(schedule)
                updateEmptyState(schedule.isEmpty())
            }
        }

        // مراقبة الحصة الحالية
        lifecycleScope.launch {
            viewModel.currentClass.collect { currentClass ->
                scheduleAdapter.setCurrentClass(currentClass)
            }
        }

        // مراقبة الحصة القادمة
        lifecycleScope.launch {
            viewModel.nextClass.collect { nextClass ->
                scheduleAdapter.setNextClass(nextClass)
            }
        }

        // مراقبة حالة التحميل
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
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

        // مراقبة إحصائيات اليوم
        lifecycleScope.launch {
            viewModel.dayStats.collect { stats ->
                updateDayStats(stats)
            }
        }
    }

    private fun updateDayButtonsSelection(selectedDay: Int) {
        val dayButtons = listOf(
            binding.sundayButton,
            binding.mondayButton,
            binding.tuesdayButton,
            binding.wednesdayButton,
            binding.thursdayButton,
            binding.fridayButton,
            binding.saturdayButton
        )

        dayButtons.forEachIndexed { index, button ->
            button.isSelected = (index + 1) == selectedDay
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
            binding.scheduleRecyclerView.visibility = android.view.View.GONE
        } else {
            binding.emptyStateLayout.visibility = android.view.View.GONE
            binding.scheduleRecyclerView.visibility = android.view.View.VISIBLE
        }
    }

    private fun updateDayStats(stats: DayStats) {
        binding.totalClassesText.text = getString(R.string.total_classes, stats.totalClasses)
        binding.totalDurationText.text = getString(R.string.total_duration, stats.totalDurationMinutes)
        binding.currentStatusText.text = stats.currentStatus
    }

    private fun showClassDetails(classItem: com.mo.bell.data.database.entities.Schedule) {
        // يمكن إضافة حوار أو نشاط لعرض تفاصيل الحصة
        val details = """
            الفصل: ${classItem.className}
            المادة: ${classItem.subjectName}
            الوقت: ${classItem.startTime} - ${classItem.endTime}
            المدرس: ${classItem.teacherName}
        """.trimIndent()
        
        Snackbar.make(binding.root, details, Snackbar.LENGTH_LONG).show()
    }

    private fun showImportOptions() {
        val options = arrayOf(
            getString(R.string.import_from_file),
            getString(R.string.import_from_qr_code)
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_schedule))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.importFromFile()
                    1 -> viewModel.importFromQRCode()
                }
            }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.schedule_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                viewModel.refreshSchedule()
                true
            }
            R.id.action_today -> {
                viewModel.selectToday()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCurrentTime()
    }
}

