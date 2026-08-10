package com.routeguard.android.data

import android.net.Uri
import com.routeguard.android.data.remote.ReportsApi
import com.routeguard.android.data.remote.dto.HazardReportResponse
import com.routeguard.android.data.remote.dto.HazardReportResponse.HazardReportData.HazardReport
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.util.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.awaitResponse
import java.io.File

class ReportsRepository(
    private val reportsApi: ReportsApi
) {
    // For demo mode state management
    private val _hazards = MutableStateFlow<List<HazardMapper>>(emptyList())
    val hazards: StateFlow<List<HazardMapper>> = _hazards.asStateFlow()

    init {
        if (AppConfig.DEMO_MODE) {
            _hazards.value = listOf(
                HazardMapper(
                    id = "demo-h1",
                    reporterId = "system",
                    category = "flooded",
                    description = "Road flooded due to heavy rain",
                    status = HazardMapper.Status.CONFIRMED,
                    confirmCount = 10,
                    denyCount = 0,
                    confidenceScore = 0.95,
                    latitude = 11.2444,
                    longitude = 125.0044,
                    distance = 500.0,
                    createdAt = "2026-08-10T00:00:00Z"
                ),
                HazardMapper(
                    id = "demo-h2",
                    reporterId = "system",
                    category = "debris",
                    description = "Fallen tree blocking lane",
                    status = HazardMapper.Status.PENDING,
                    confirmCount = 2,
                    denyCount = 1,
                    confidenceScore = 0.6,
                    latitude = 11.2350,
                    longitude = 124.9950,
                    distance = 700.0,
                    createdAt = "2026-08-10T00:05:00Z"
                )
            )
        }
    }

    suspend fun getNearbyHazards(
        latitude: Double,
        longitude: Double,
        radius: Double = 5000.0,
        status: String? = null,
        category: String? = null,
        limit: Int? = 50,
        offset: Int? = 0
    ): List<HazardMapper> {
        if (AppConfig.DEMO_MODE) {
            return _hazards.value
        }

        try {
            val response: Response<HazardReportResponse> = reportsApi.getNearbyReports(
                lat = latitude,
                lng = longitude,
                radius = radius,
                status = status,
                category = category,
                limit = limit,
                offset = offset
            ).awaitResponse()

            if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                val hazardReports = response.body()!!.data!!.reports
                return hazardReports.map { HazardMapper.fromApiResponse(it, latitude, longitude) }
            }
        } catch (e: Exception) {
            // Log error
        }
        return emptyList()
    }

    suspend fun createReport(
        category: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        mediaUri: Uri? = null
    ): HazardReportResponse {
        if (AppConfig.DEMO_MODE) {
            val newHazard = HazardMapper(
                id = "demo-new-${System.currentTimeMillis()}",
                reporterId = "demo-123",
                category = category,
                description = description,
                status = HazardMapper.Status.PENDING,
                confirmCount = 0,
                denyCount = 0,
                confidenceScore = 0.5,
                latitude = latitude,
                longitude = longitude,
                distance = 0.0,
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
            )
            _hazards.value = _hazards.value + newHazard

            return HazardReportResponse(
                success = true,
                data = HazardReportResponse.HazardReportData(
                    reports = listOf(
                        HazardReport(
                            id = newHazard.id,
                            reporterId = newHazard.reporterId,
                            category = newHazard.category,
                            description = newHazard.description,
                            locationWkt = "POINT($longitude $latitude)",
                            status = "PENDING",
                            confirmCount = 0,
                            denyCount = 0,
                            confidenceScore = 0.5,
                            createdAt = newHazard.createdAt
                        )
                    ),
                    total = 1,
                    limit = 50,
                    offset = 0
                ),
                error = null
            )
        }

        val location = "POINT($longitude $latitude)"

        if (mediaUri != null) {
            return createReportWithMedia(category, description, latitude, longitude, mediaUri)
        }

        val reportRequest = ReportsApi.ReportRequest(
            category = category,
            description = description,
            location = location
        )
        
        return try {
            val response = reportsApi.createReport(reportRequest).awaitResponse()
            response.body() ?: HazardReportResponse(false, null, HazardReportResponse.HazardReportError("Empty response"))
        } catch (e: Exception) {
            HazardReportResponse(false, null, HazardReportResponse.HazardReportError(e.localizedMessage ?: "Network error"))
        }
    }

    private suspend fun createReportWithMedia(
        category: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        mediaUri: Uri
    ): HazardReportResponse {
        val location = "POINT($longitude $latitude)"

        val categoryPart = category.toRequestBody("text/plain".toMediaTypeOrNull())
        val descriptionPart = description?.toRequestBody("text/plain".toMediaTypeOrNull())
        val locationPart = location.toRequestBody("text/plain".toMediaTypeOrNull())

        var mediaPart: MultipartBody.Part? = null
        try {
            val file = File(mediaUri.path ?: "")
            if (file.exists()) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                mediaPart = MultipartBody.Part.createFormData("media", file.name, requestFile)
            }
        } catch (e: Exception) {
            // Fallback to regular report if media fails
            return createReport(category, description, latitude, longitude, null)
        }

        return try {
            val response = reportsApi.createReport(
                categoryPart,
                descriptionPart,
                locationPart,
                mediaPart
            ).awaitResponse()
            response.body() ?: HazardReportResponse(false, null, HazardReportResponse.HazardReportError("Empty response"))
        } catch (e: Exception) {
            HazardReportResponse(false, null, HazardReportResponse.HazardReportError(e.localizedMessage ?: "Network error"))
        }
    }
}
