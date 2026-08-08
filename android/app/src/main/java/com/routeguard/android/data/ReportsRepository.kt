package com.routeguard.android.data

import android.net.Uri
import com.routeguard.android.data.remote.dto.HazardReportResponse
import com.routeguard.android.data.remote.dto.HazardReportResponse.HazardReportData.HazardReport
import com.routeguard.android.map.HazardMapper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.run
import java.io.File

class ReportsRepository(
    private val reportsApi: com.routeguard.android.data.remote.ReportsApi
) {

    suspend fun getNearbyHazards(
        latitude: Double,
        longitude: Double,
        radius: Double = 5000.0,
        status: String? = null,
        category: String? = null,
        limit: Int? = 50,
        offset: Int? = 0
    ): List<HazardMapper> {
        val response: Response<HazardReportResponse> = reportsApi.getNearbyReports(
            lat = latitude,
            lng = longitude,
            radius = radius,
            status = status,
            category = category,
            limit = limit,
            offset = offset
        ).execute()

        if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
            val hazardReports = response.body()!!.data.reports
            return hazardReports.map { HazardMapper.fromApiResponse(it, latitude, longitude) }
        } else {
            // Return empty list on error - in a real app, we might want to throw or handle differently
            return emptyList()
        }
    }

    suspend fun createReport(
        category: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        mediaUri: Uri? = null
    ): HazardReportResponse {
        val location = "POINT($longitude $latitude)"

        // If we have media, use multipart upload
        if (mediaUri != null) {
            return createReportWithMedia(category, description, latitude, longitude, mediaUri)
        }

        // Otherwise use regular JSON upload (backward compatibility)
        val reportRequest = com.routeguard.android.data.remote.ReportsApi.ReportRequest(
            category = category,
            description = description,
            location = location
        )
        val response: Response<HazardReportResponse> = reportsApi.createReport(
            reportRequest
        ).execute()

        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            // Return error response - in a real app, we might want to throw or handle differently
            return HazardReportResponse(
                success = false,
                data = null,
                error = HazardReportResponse.HazardReportError(
                    message = "Failed to create report: ${response.code()}"
                )
            )
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

        // Create request body parts
        val categoryPart = RequestBody.create(
            MediaType.parse("text/plain"),
            category
        )
        val descriptionPart = description?.let {
            RequestBody.create(
                MediaType.parse("text/plain"),
                it
            )
        }
        val locationPart = RequestBody.create(
            MediaType.parse("text/plain"),
            location
        )

        // Prepare media part if URI is valid
        var mediaPart: MultipartBody.Part? = null
        if (mediaUri != null) {
            try {
                val file = File(mediaUri.path)
                if (file.exists()) {
                    val requestFile = RequestBody.create(
                        MediaType.parse("image/*"),
                        file
                    )
                    mediaPart = MultipartBody.Part.createFormData(
                        "media",
                        file.name,
                        requestFile
                    )
                }
            } catch (e: Exception) {
                // If we can't create the media part, fallback to regular upload
                val reportRequest = com.routeguard.android.data.remote.ReportsApi.ReportRequest(
                    category = category,
                    description = description,
                    location = location
                )
                val response: Response<HazardReportResponse> = reportsApi.createReport(
                    reportRequest
                ).execute()

                if (response.isSuccessful && response.body() != null) {
                    return response.body()!!
                } else {
                    return HazardReportResponse(
                        success = false,
                        data = null,
                        error = HazardReportResponse.HazardReportError(
                            message = "Failed to create report: ${response.code()}"
                        )
                    )
                }
            }
        }

        // Make the multipart request
        val response: Response<HazardReportResponse> = reportsApi.createReport(
            categoryPart,
            descriptionPart,
            locationPart,
            mediaPart
        ).execute()

        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            // Return error response
            return HazardReportResponse(
                success = false,
                data = null,
                error = HazardReportResponse.HazardReportError(
                    message = "Failed to create report: ${response.code()}"
                )
            )
        }
    }

    data class HazardReportResponse(
        val success: Boolean,
        val data: HazardReportData?,
        val error: HazardReportError?
    ) {
        data class HazardReportData(
            val reports: List<HazardReport>,
            val total: Int,
            val limit: Int,
            val offset: Int
        ) {
            data class HazardReport(
                val id: String,
                val reporterId: String,
                val category: String,
                val description: String?,
                val locationWkt: String,
                val status: String,
                val confirmCount: Int,
                val denyCount: Int,
                val confidenceScore: Double,
                val createdAt: String
            )
        }

        data class HazardReportError(
            val message: String,
            val details: Map<String, List<String>>? = null
        )
    }
}