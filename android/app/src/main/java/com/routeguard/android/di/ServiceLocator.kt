package com.routeguard.android.di

import android.content.Context
import com.routeguard.android.notifications.FcmTokenManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service Locator for accessing dependencies in non-Hilt managed classes
 */
@Singleton
class ServiceLocator @Inject constructor() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceLocatorEntryPoint {
        fun fcmTokenManager(): FcmTokenManager
    }

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context
    }

    private fun getEntryPoint(): ServiceLocatorEntryPoint {
        val context = applicationContext ?: throw IllegalStateException("ServiceLocator not initialized")
        return EntryPoints.get(context, ServiceLocatorEntryPoint::class.java)
    }

    fun getFcmTokenManager(): FcmTokenManager {
        return getEntryPoint().fcmTokenManager()
    }
}
