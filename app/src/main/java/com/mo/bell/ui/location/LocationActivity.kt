package com.mo.bell.ui.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.mo.bell.R
import com.mo.bell.databinding.ActivityLocationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationBinding
    private val viewModel: LocationViewModel by viewModels()
    private var googleMap: GoogleMap? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.onLocationPermissionGranted()
            enableLocationFeatures()
        } else {
            showLocationPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupObservers()
        setupMap()
        checkLocationPermission()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.location_settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        // تفعيل/إلغاء تفعيل الموقع
        binding.locationEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLocationEnabled(isChecked)
        }

        // منزلق نطاق التفعيل
        binding.radiusSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.setActivationRadius(value.toDouble())
            }
        }

        // أزرار الإجراءات
        binding.getCurrentLocationButton.setOnClickListener {
            viewModel.getCurrentLocation()
        }

        binding.setSchoolLocationButton.setOnClickListener {
            viewModel.setCurrentLocationAsSchool()
        }

        binding.testLocationButton.setOnClickListener {
            viewModel.testLocationDetection()
        }

        binding.locationPermissionButton.setOnClickListener {
            requestLocationPermissions()
        }

        binding.openLocationSettingsButton.setOnClickListener {
            openLocationSettings()
        }
    }

    private fun setupObservers() {
        // مراقبة تفعيل الموقع
        lifecycleScope.launch {
            viewModel.locationEnabled.collect { enabled ->
                binding.locationEnabledSwitch.isChecked = enabled
                updateLocationControlsVisibility(enabled)
            }
        }

        // مراقبة نطاق التفعيل
        lifecycleScope.launch {
            viewModel.activationRadius.collect { radius ->
                binding.radiusSlider.value = radius.toFloat()
                binding.radiusValueText.text = getString(R.string.radius_meters, radius.toInt())
                updateMapRadius(radius)
            }
        }

        // مراقبة الموقع الحالي
        lifecycleScope.launch {
            viewModel.currentLocation.collect { location ->
                location?.let {
                    updateCurrentLocationOnMap(it.latitude, it.longitude)
                }
            }
        }

        // مراقبة موقع المدرسة
        lifecycleScope.launch {
            viewModel.schoolLocation.collect { location ->
                location?.let {
                    updateSchoolLocationOnMap(it.latitude, it.longitude)
                    binding.schoolLocationText.text = getString(
                        R.string.school_location_coordinates,
                        it.latitude,
                        it.longitude
                    )
                } ?: run {
                    binding.schoolLocationText.text = getString(R.string.school_location_not_set)
                }
            }
        }

        // مراقبة حالة الموقع
        lifecycleScope.launch {
            viewModel.locationStatus.collect { status ->
                updateLocationStatusUI(status)
            }
        }

        // مراقبة صلاحيات الموقع
        lifecycleScope.launch {
            viewModel.hasLocationPermission.collect { hasPermission ->
                updatePermissionUI(hasPermission)
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
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // إعداد الخريطة
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }

        // تحديث الخريطة بالبيانات الحالية
        viewModel.refreshLocationData()
    }

    private fun updateCurrentLocationOnMap(latitude: Double, longitude: Double) {
        googleMap?.let { map ->
            val location = LatLng(latitude, longitude)
            map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(getString(R.string.current_location))
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun updateSchoolLocationOnMap(latitude: Double, longitude: Double) {
        googleMap?.let { map ->
            val location = LatLng(latitude, longitude)
            map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(getString(R.string.school_location))
            )
        }
    }

    private fun updateMapRadius(radius: Double) {
        // يمكن إضافة دائرة على الخريطة لإظهار نطاق التفعيل
    }

    private fun updateLocationControlsVisibility(enabled: Boolean) {
        val visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        
        binding.radiusLayout.visibility = visibility
        binding.locationActionsLayout.visibility = visibility
        binding.mapContainer.visibility = visibility
        binding.locationStatusLayout.visibility = visibility
    }

    private fun updateLocationStatusUI(status: String) {
        binding.locationStatusText.text = status
    }

    private fun updatePermissionUI(hasPermission: Boolean) {
        binding.locationPermissionButton.visibility = if (hasPermission) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun checkLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.setLocationPermissionStatus(hasPermission)
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableLocationFeatures() {
        googleMap?.let { map ->
            if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            }
        }
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun showLocationPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            getString(R.string.location_permission_denied),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.settings)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }.show()
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
}

