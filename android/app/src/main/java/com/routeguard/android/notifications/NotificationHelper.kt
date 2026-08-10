package com.routeguard.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.routeguard.android.R
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.util.LocationUtils
import java.util.Locale

/**
 * Helper class for creating and managing notifications
 */
object NotificationHelper {

    private const val HAZARD_ALERT_CHANNEL_ID = "hazard_alert_channel"
    private const val HAZARD_ALERT_CHANNEL_NAME = "Hazard Alerts"
    private const val HAZARD_ALERT_CHANNEL_DESCRIPTION = "Alerts for nearby hazards"

    /**
     * Create notification channel for hazard alerts (call once on app start)
     */
    fun createHazardAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HAZARD_ALERT_CHANNEL_ID,
                HAZARD_ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = HAZARD_ALERT_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show a hazard proximity notification
     */
    fun showHazardProximityNotification(
        context: Context,
        hazard: HazardMapper,
        userLatitude: Double,
        userLongitude: Double
    ) {
        val distance = LocationUtils.calculateDistance(
            userLatitude,
            userLongitude,
            hazard.latitude,
            hazard.longitude
        )

        // Create intent to open hazard detail screen (placeholder for now)
        val intent = Intent(context, com.routeguard.android.map.HazardMapScreen::class.java)
        intent.putExtra("HAZARD_ID", hazard.id)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create dismiss action
        val dismissIntent = Intent(context, HazardAlertDismissReceiver::class.java)
        dismissIntent.putExtra("HAZARD_ID", hazard.id)
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, HAZARD_ALERT_CHANNEL_ID)
            .setSmallIcon(HazardMapper.getIconForCategory(hazard.category))
            .setContentTitle("Hazard Alert!")
            .setContentText(
                "${hazard.description?.take(30)}... ${String.format(Locale.US, "%.1f", distance / 1000)}km away"
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(hazard.hashCode(), notification)
    }

    fun createHazardAlertChannel(hazardAlertService: HazardAlertService) {
        createHazardAlertChannel(hazardAlertService as Context)
    }
}
