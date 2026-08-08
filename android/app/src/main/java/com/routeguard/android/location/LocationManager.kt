package com.routeguard.android.location

import android.Manifest
import android.app.Service
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import javax.inject.Inject

/**
 * Manages location updates for the application
 */
class LocationManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "LocationManager"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val FASTEST_UPDATE_INTERVAL = 5000L // 5 seconds
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null
    private val locationUpdateHandler = Handler(Looper.getMainLooper())
    private var locationUpdateRunnable: Runnable? = null

    /**
     * Start location updates
     */
    fun startLocationUpdates() {
        stopLocationUpdates() // Clean up any existing updates

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult.lastLocation)
            }
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .build()

        // Check permissions before requesting updates
        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        // Also get initial location
        getLastKnownLocation()
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeUpdates(it) }
        locationCallback = null
        locationUpdateRunnable?.let { locationUpdateHandler.removeCallbacks(it) }
        locationUpdateRunnable = null
    }

    /**
     * Get the last known location
     */
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        return try {
            fusedLocationClient.lastLocation
        } catch (e: SecurityException) {
            Log.w(TAG, "Lost location permission.", e)
            null
        }
    }

    /**
     * Get current location (updated via callbacks)
     */
    fun getCurrentLocation(): Location? {
        return currentLocation
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return android.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Handle location updates
     */
    private fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
        currentLocation = location
    }
}