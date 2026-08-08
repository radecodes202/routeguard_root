package com.routeguard.android.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.routeguard.android.notifications.FcmTokenManager
import com.routeguard.android.ui.auth.AuthViewModel
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service Locator for accessing dependencies in non-Hilt managed classes
 * This is a temporary solution - ideally all classes would be Hilt-injected
 */
@Singleton
class ServiceLocator @Inject constructor(
    private val singletonComponent: SingletonComponent
) {

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context
    }

    fun getApplicationContext(): Context {
        return applicationContext ?: throw IllegalStateException("ServiceLocator not initialized")
    }

    fun <T : ViewModel> getViewModelProvider(
        owner: androidx.lifecycle.ViewModelStoreOwner
    ): ViewModelProvider {
        return ViewModelProvider(owner)
    }

    fun getFcmTokenManager(): FcmTokenManager {
        return singletonComponent.fcmTokenManager()
    }

    fun getAuthViewModel(): AuthViewModel {
        return singletonComponent.authViewModel()
    }
}