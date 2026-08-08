package com.routeguard.android.di

import com.routeguard.android.data.AuthRepository
import com.routeguard.android.data.local.TokenStore
import com.routeguard.android.data.remote.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTokenStore(
        @ApplicationContext context: android.content.Context
    ): TokenStore {
        return TokenStore(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenStore: TokenStore
    ): AuthRepository {
        return AuthRepository(authApi, tokenStore)
    }

    @Provides
    @Singleton
    fun provideReportsRepository(
        reportsApi: com.routeguard.android.data.remote.ReportsApi
    ): com.routeguard.android.data.ReportsRepository {
        return com.routeguard.android.data.ReportsRepository(reportsApi)
    }

    @Provides
    @Singleton
    fun provideServiceLocator(
        singletonComponent: SingletonComponent
    ): ServiceLocator {
        return ServiceLocator(singletonComponent)
    }

    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: android.content.Context
    ): LocationManager {
        return LocationManager(context)
    }

    @Provides
    @Singleton
    fun provideOsrmRepository(
        osrmApi: com.routeguard.android.map.OsrmApi
    ): com.routeguard.android.map.OsrmRepository {
        return com.routeguard.android.map.OsrmRepository(osrmApi)
    }

    @Provides
    @Singleton
    fun provideRouteViewModel(
        osrmRepository: com.routeguard.android.map.OsrmRepository
    ): com.routeguard.android.map.RouteViewModel {
        return com.routeguard.android.map.RouteViewModel(osrmRepository)
    }
}