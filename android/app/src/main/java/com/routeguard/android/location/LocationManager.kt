package com.routeguard.android.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.routeguard.android.util.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages location updates for the application
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null

    init {
        if (AppConfig.DEMO_MODE) {
            // Set a default demo location (e.g., Tacloban City coordinates)
            currentLocation = Location("demo").apply {
                latitude = 11.2400
                longitude = 125.0000
                accuracy = 10.0f
                time = System.currentTimeMillis()
            }
        }
    }

    fun startLocationUpdates() {
        stopLocationUpdates()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { onLocationChanged(it) }
            }
        }
        locationCallback = callback

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).build()

        if (hasLocationPermission()) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("LocationManager", "Permission error", e)
            }
        }
    }

    fun stopLocationUpdates() {
        val callback = locationCallback
        if (callback != null) {
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    suspend fun getLastKnownLocation(): Location? {
        return currentLocation
    }

    fun getCurrentLocation(): Location? {
        return currentLocation
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun onLocationChanged(location: Location) {
        currentLocation = location
    }
}
