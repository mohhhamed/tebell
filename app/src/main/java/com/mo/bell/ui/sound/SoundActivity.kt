package com.mo.bell.ui.sound

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mo.bell.R
import com.mo.bell.audio.BellSoundType
import com.mo.bell.databinding.ActivitySoundBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SoundActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySoundBinding
    private val viewModel: SoundViewModel by viewModels()
    private lateinit var soundAdapter: SoundAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.sound_settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        // إعداد قائمة الأصوات
        soundAdapter = SoundAdapter { soundType ->
            viewModel.selectSound(soundType)
        }
        
        binding.soundRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SoundActivity)
            adapter = soundAdapter
        }

        // إعداد المفاتيح والمنزلقات
        binding.soundEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSoundEnabled(isChecked)
        }

        binding.volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setVolumeLevel(value.toInt())
            }
        }

        binding.durationSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setSoundDuration(value.toInt())
            }
        }

        // إعداد أزرار الاختبار
        binding.testSoundButton.setOnClickListener {
            viewModel.testCurrentSound()
        }

        binding.stopSoundButton.setOnClickListener {
            viewModel.stopSound()
        }

        // إعداد أزرار الإعدادات المسبقة
        binding.presetQuietButton.setOnClickListener {
            viewModel.applyQuietPreset()
        }

        binding.presetNormalButton.setOnClickListener {
            viewModel.applyNormalPreset()
        }

        binding.presetLoudButton.setOnClickListener {
            viewModel.applyLoudPreset()
        }
    }

    private fun setupObservers() {
        // مراقبة حالة تفعيل الصوت
        lifecycleScope.launch {
            viewModel.soundEnabled.collect { enabled ->
                binding.soundEnabledSwitch.isChecked = enabled
                updateSoundControlsVisibility(enabled)
            }
        }

        // مراقبة مستوى الصوت
        lifecycleScope.launch {
            viewModel.volumeLevel.collect { volume ->
                binding.volumeSlider.value = volume.toFloat()
                binding.volumeValueText.text = "$volume%"
            }
        }

        // مراقبة مدة الصوت
        lifecycleScope.launch {
            viewModel.soundDuration.collect { duration ->
                binding.durationSlider.value = duration.toFloat()
                binding.durationValueText.text = getString(R.string.duration_seconds, duration)
            }
        }

        // مراقبة الصوت المحدد
        lifecycleScope.launch {
            viewModel.selectedSoundType.collect { soundType ->
                soundAdapter.setSelectedSound(soundType)
            }
        }

        // مراقبة قائمة الأصوات
        lifecycleScope.launch {
            viewModel.availableSounds.collect { sounds ->
                soundAdapter.submitList(sounds)
            }
        }

        // مراقبة حالة التشغيل
        lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                binding.testSoundButton.isEnabled = !isPlaying
                binding.stopSoundButton.isEnabled = isPlaying
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

    private fun updateSoundControlsVisibility(enabled: Boolean) {
        val visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        
        binding.volumeLevelLayout.visibility = visibility
        binding.soundDurationLayout.visibility = visibility
        binding.soundSelectionLayout.visibility = visibility
        binding.testButtonsLayout.visibility = visibility
        binding.presetsLayout.visibility = visibility
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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopSound()
    }
}

