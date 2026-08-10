package com.routeguard.android.di

import com.routeguard.android.BuildConfig
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
        // Use buildConfigField for dynamic base URL based on build variant
        val baseUrl = BuildConfig.BASE_URL
        return Retrofit.Builder()
            .baseUrl(baseUrl)
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
        // For OSRM, we need a different base URL - also using buildConfigField
        val osrmUrl = BuildConfig.OSRM_URL
        val osrmRetrofit = Retrofit.Builder()
            .baseUrl(osrmUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return osrmRetrofit.create(com.routeguard.android.map.OsrmApi::class.java)
    }
}