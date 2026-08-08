package com.routeguard.android.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

/**
 * Firebase Messaging Service to handle incoming push notifications
 */
class FcmMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // Handle data payload if needed
            if (remoteMessage.data.containsKey("hazard_id")) {
                val hazardId = remoteMessage.data["hazard_id"]
                // Handle hazard-specific notification
                handleHazardNotification(remoteMessage)
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: ${remoteMessage.notification!!.body}")
            // Handle notification payload if needed
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send the token to your server if needed
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        // In a real implementation, you would send this token to your backend
        // so it can be used to send push notifications to this specific device
        Log.d(TAG, "Token sent to server: $token")
    }

    private fun handleHazardNotification(remoteMessage: RemoteMessage) {
        // Extract hazard data from message
        val hazardId = remoteMessage.data["hazard_id"]
        val hazardType = remoteMessage.data["type"]
        val message = remoteMessage.data["message"]

        // You could start an activity or show a notification here
        // For now, we'll just log it
        Log.d(TAG, "Handling hazard notification: $hazardId, $hazardType, $message")
    }
}