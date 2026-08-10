package com.routeguard.android

import android.app.Application
import com.routeguard.android.di.ServiceLocator
import com.routeguard.android.notifications.FcmTokenManager
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RouteGuardApplication : Application() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager
    
    @Inject
    lateinit var serviceLocator: ServiceLocator

    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Initialize ServiceLocator with application context
        serviceLocator.initialize(this@RouteGuardApplication)
        // Initialize FCM token manager
        fcmTokenManager.initializeFcm()
    }
}
