package com.routeguard.android.data.remote

import com.routeguard.android.data.remote.dto.HazardReportResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ReportsApi {

    @GET("/api/v1/reports/nearby")
    fun getNearbyReports(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 5000.0,
        @Query("status") status: String?,
        @Query("category") category: String?,
        @Query("limit") limit: Int?,
        @Query("offset") offset: Int?
    ): Call<HazardReportResponse>

    @Multipart
    @POST("/api/v1/reports")
    fun createReport(
        @Part("category") category: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("location") location: RequestBody,
        @Part mediaPart: MultipartBody.Part?
    ): Call<HazardReportResponse>

    // Keep the original method for backward compatibility or reports without media
    @POST("/api/v1/reports")
    fun createReport(
        @Body reportRequest: ReportRequest
    ): Call<HazardReportResponse>

    data class ReportRequest(
        val category: String,
        val description: String?,
        val location: String
    )
}