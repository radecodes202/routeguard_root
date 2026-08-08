package com.routeguard.android.di

import com.routeguard.android.data.remote.AuthApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.routeguard.example.com/") // Base URL - would be configured via build flavors or env vars
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(
        retrofit: Retrofit
    ): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideReportsApi(
        retrofit: Retrofit
    ): com.routeguard.android.data.remote.ReportsApi {
        return retrofit.create(com.routeguard.android.data.remote.ReportsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOsrmApi(
        okHttpClient: OkHttpClient
    ): com.routeguard.android.map.OsrmApi {
        // For OSRM, we need a different base URL
        val osrmRetrofit = Retrofit.Builder()
            .baseUrl("http://router.project-osrm.org/") // Public OSRM demo server - would be configured via env vars
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return osrmRetrofit.create(com.routeguard.android.map.OsrmApi::class.java)
    }
}