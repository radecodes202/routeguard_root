package com.routeguard.android.notifications

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.routeguard.android.R
import com.routeguard.android.data.AuthRepository
import com.routeguard.android.data.ReportsRepository
import com.routeguard.android.location.LocationManager
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.util.LocationUtils
import com.routeguard.android.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.InjectedService
import javax.inject.Inject

/**
 * Service that monitors user location and sends push notifications for nearby hazards
 * Triggers alerts when hazards are detected within 500m radius
 */
@InjectedService
@AndroidEntryPoint
class HazardAlertService @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportsRepository: ReportsRepository,
    private val locationManager: LocationManager
) : Service() {

    companion object {
        private const val TAG = "HazardAlertService"
        private const val ALERT_RADIUS_METERS = 500.0f
        private const val CHECK_INTERVAL_MS = 30000 // 30 seconds
        private const val NOTIFICATION_CHANNEL_ID = "hazard_alert_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var isRunning = false
    private var alertTask: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HazardAlertService created")
        // Initialize notification channel
        NotificationHelper.createHazardAlertChannel(this)
        // Start location updates
        locationManager.startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HazardAlertService started")
        if (!isRunning) {
            startMonitoring()
        }
        // Return START_STICKY so service is restarted if killed by system
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HazardAlertService destroyed")
        stopMonitoring()
        // Stop location updates
        locationManager.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used for started service
        return null
    }

    private fun startMonitoring() {
        isRunning = true
        alertTask = object : Runnable {
            override fun run() {
                checkForNearbyHazards()
                // Schedule next check
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
        handler.postDelayed(alertTask!!, CHECK_INTERVAL_MS)
    }

    private fun stopMonitoring() {
        isRunning = false
        alertTask?.let { handler.removeCallbacks(it) }
        alertTask = null
    }

    private fun checkForNearbyHazards() {
        // Get the user's current location
        val currentLocation = locationManager.getCurrentLocation()
        if (currentLocation == null) {
            Log.w(TAG, "Unable to get current location")
            return
        }

        // Fetch hazards within 5km (we'll filter to 500m locally for efficiency)
        // In production, you might want to adjust the backend query to use a smaller radius
        androidx.lifecycle.viewModelScope.launch {
            try {
                val hazards = reportsRepository.getNearbyHazards(
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude,
                    radius = 5000.0 // 5km to reduce API calls, we'll filter locally
                )

                // Check for hazards within 500m
                val nearbyHazards = hazards.filter { hazard ->
                    LocationUtils.calculateDistance(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        hazard.latitude,
                        hazard.longitude
                    ) <= ALERT_RADIUS_METERS
                }

                // Show notifications for new hazards
                for (hazard in nearbyHazards) {
                    NotificationHelper.showHazardProximityNotification(
                        this@HazardAlertService,
                        hazard,
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
                    Log.i(TAG, "Showing hazard notification: ${hazard.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for nearby hazards", e)
            }
        }
    }
}