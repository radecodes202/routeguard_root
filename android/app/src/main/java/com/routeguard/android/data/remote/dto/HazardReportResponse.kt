package com.routeguard.android.data.remote.dto

/**
 * Response wrapper for hazard reports API calls
 */
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