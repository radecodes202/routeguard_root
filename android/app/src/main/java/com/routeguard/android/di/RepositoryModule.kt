package com.routeguard.android.di

import com.routeguard.android.data.AuthRepository
import com.routeguard.android.data.local.TokenStore
import com.routeguard.android.data.remote.AuthApi
import com.routeguard.android.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

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