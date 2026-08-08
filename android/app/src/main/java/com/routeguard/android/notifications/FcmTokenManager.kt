package com.routeguard.android.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.routeguard.android.ui.auth.AuthViewModel
import com.routeguard.android.di.ServiceLocator
import dagger.hilt.android.AndroidEntryPoint

/**
 * Manages FCM token retrieval and registration with backend
 */
@AndroidEntryPoint
class FcmTokenManager @Inject constructor(
    private val context: Context,
    private val authViewModel: AuthViewModel
) {

    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Initialize FCM and register token with backend
     */
    fun initializeFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            val msg = "FCM Token: $token"
            Log.d(TAG, msg)
            // Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

            // Send token to backend if user is authenticated
            // In a real app, you would check auth state first
            authViewModel.registerFcmToken(token)
        }
    }

    /**
     * Get current FCM token
     */
    fun getFcmToken(): String {
        // This is a synchronous call that might block - in production you'd use the async version above
        // For simplicity in this example, we'll return a placeholder
        // In reality, you should use FirebaseMessaging.getInstance().token.addOnSuccessListener {...}
        return "PLACEHOLDER_FCM_TOKEN"
    }
}