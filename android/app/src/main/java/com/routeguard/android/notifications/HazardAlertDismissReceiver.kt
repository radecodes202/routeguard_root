package com.routeguard.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver to handle dismissal of hazard alerts
 */
class HazardAlertDismissReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HazardAlertDismissReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val hazardId = intent.getStringExtra("HAZARD_ID")
        Log.d(TAG, "Hazard alert dismissed for hazard ID: $hazardId")
        // In a real implementation, you might want to:
        // 1. Track that the user dismissed this alert
        // 2. Prevent showing the same alert again for a period of time
        // 3. Update some analytics or user preference
    }
}