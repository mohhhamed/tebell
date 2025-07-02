package com.mo.bell.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.mo.bell.receivers.GeofenceBroadcastReceiver
import com.mo.bell.utils.NotificationHelper
import com.mo.bell.utils.SettingsManager

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var settingsManager: SettingsManager

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
        settingsManager = SettingsManager(this)
        NotificationHelper.createNotificationChannels(this)
        Log.d("LocationServiceDebug", "Service created.")
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationServiceDebug", "Service started with action: ${intent?.action}")

        if (intent?.action == ACTION_START_GEOFENCE) {
            startAsForeground()
            Log.d("LocationServiceDebug", "Requesting current location from FusedLocationProvider...")
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    Log.d("LocationServiceDebug", "FusedLocationProvider - onSuccessListener triggered.")
                    if (location != null) {
                        Log.d("LocationServiceDebug", "SUCCESS: Location received: Lat=${location.latitude}, Lon=${location.longitude}")
                        settingsManager.save(SettingsManager.KEY_SCHOOL_LAT, location.latitude.toBits())
                        settingsManager.save(SettingsManager.KEY_SCHOOL_LON, location.longitude.toBits())
                        Toast.makeText(this, "تم حفظ الموقع بنجاح!", Toast.LENGTH_SHORT).show()
                        addGeofence(location.latitude, location.longitude)
                    } else {
                        Log.w("LocationServiceDebug", "FAILURE: Location object received from listener is NULL.")
                        Toast.makeText(this, "لم يتم العثور على الموقع (null). تأكد من أنك في مكان مفتوح.", Toast.LENGTH_LONG).show()
                        stopSelf()
                    }
                }.addOnFailureListener { e ->
                    Log.e("LocationServiceDebug", "FAILURE: FusedLocationProvider - onFailureListener triggered. Error: ${e.message}", e)
                    Toast.makeText(this, "فشل في الحصول على الموقع. تأكد من تفعيل GPS ومنح الأذونات.", Toast.LENGTH_LONG).show()
                    stopSelf()
                }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = NotificationHelper.createForegroundNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(SERVICE_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(lat: Double, lon: Double) {
        // ... (هذه الدالة تبقى كما هي)
    }

    override fun onDestroy() {
        super.onDestroy()
        geofencingClient.removeGeofences(geofencePendingIntent)
        Log.d("LocationServiceDebug", "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_GEOFENCE = "ACTION_START_GEOFENCE"
        private const val GEOFENCE_ID = "SCHOOL_GEOFENCE"
        private const val SERVICE_ID = 101
    }
}