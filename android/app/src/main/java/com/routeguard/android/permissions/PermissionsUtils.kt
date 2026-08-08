package com.routeguard.android.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionsUtils {

    private const val LOCATION_REQUEST_CODE = 1001
    private const val NOTIFICATION_REQUEST_CODE = 1002

    /**
     * Check if location permission is granted
     */
    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if notification permission is granted (API 33+)
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        // For API < 33, notifications are granted by default
        return true
    }

    /**
     * Request location permission from activity
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }

    /**
     * Request notification permission from activity (API 33+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_REQUEST_CODE
            )
        }
    }

    /**
     * Handle permission request results
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
            NOTIFICATION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }
    }
}