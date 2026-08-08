// map/HazardMapper.kt
package com.routeguard.android.map

import com.routeguard.android.data.remote.dto.HazardReportResponse

/**
 * Data class for mapping hazard reports between API and UI layers
 * Contains additional UI-specific fields like distance, formatted strings, etc.
 */
data class HazardMapper(
    val id: String,
    val reporterId: String,
    val category: String,
    val description: String?,
    val status: Status,
    val confirmCount: Int,
    val denyCount: Int,
    val confidenceScore: Double,
    val latitude: Double,
    val longitude: Double,
    val distance: Double = 0.0, // Distance from user in meters
    val createdAt: String
) {

    enum class Status {
        PENDING, FLAGGED, CONFIRMED, FALSE, INCONCLUSIVE
    }

    /**
     * Create HazardMapper from API response
     * @param apiResponse Response from backend
     * @param userLat User's latitude for distance calculation
     * @param userLng User's longitude for distance calculation
     */
    companion object {
        fun fromApiResponse(
            apiResponse: HazardReportResponse,
            userLat: Double = 0.0,
            userLng: Double = 0.0
        ): HazardMapper {
            // Parse location from WKT format: POINT(longitude latitude)
            val coords = parseLocation(apiResponse.location)
            val lat = coords?.latitude ?: 0.0
            val lng = coords?.longitude ?: 0.0

            // Calculate distance from user if coordinates provided
            val distance = if (userLat != 0.0 && userLng != 0.0 && lat != 0.0 && lng != 0.0) {
                calculateDistance(userLat, userLng, lat, lng)
            } else {
                0.0
            }

            return HazardMapper(
                id = apiResponse.id,
                reporterId = apiResponse.reporterId,
                category = apiResponse.category,
                description = apiResponse.description,
                status = Status.valueOf(apiResponse.status.uppercase()),
                confirmCount = apiResponse.confirmCount,
                denyCount = apiResponse.denyCount,
                confidenceScore = apiResponse.confidenceScore,
                latitude = lat,
                longitude = lng,
                distance = distance,
                createdAt = apiResponse.createdAt
            )
        }

        private fun parseLocation(locationWkt: String?): LatLng? {
            if (locationWkt == null || !locationWkt.startsWith("POINT(")) return null

            // Extract coordinates from POINT(lng lat)
            val match = """\((-?\d+\.?\d*)\s+(-?\d+\.?\d*)\)""".toRegex().match(locationWkt)
            if (match == null) return null

            val lng = match.groupValues[1].toDoubleOrNull() ?: return null
            val lat = match.groupValues[2].toDoubleOrNull() ?: return null

            return LatLng(lat, lng)
        }

        private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            // Haversine formula for distance calculation
            val R = 6371000.0 // Earth's radius in meters
            val φ1 = Math.toRadians(lat1)
            val φ2 = Math.toRadians(lat2)
            val Δφ = Math.toRadians(lat2 - lat1)
            val Δλ = Math.toRadians(lng2 - lng1)

            val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                    Math.cos(φ1) * Math.cos(φ2) *
                    Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return R * c
        }

        /** Simple data class for holding lat/lng */
        private data class LatLng(val latitude: Double, val longitude: Double)
    }

    /**
     * Get icon resource for hazard category
     */
    fun getIconForCategory(): Int {
        return when (category) {
            "flooded" -> R.drawable.ic_flood
            "fully_blocked" -> R.drawable.ic_blocked
            "debris" -> R.drawable.ic_debris
            "accident" -> R.drawable.ic_accident
            "partially_passable" -> R.drawable.ic_partially_blocked
            else -> R.drawable.ic_hazard_unknown
        }
    }

    /**
     * Get color resource for hazard category
     */
    fun getColorForCategory(): Int {
        return when (category) {
            "flooded" -> R.color.hazard_flooded
            "fully_blocked" -> R.color.hazard_blocked
            "debris" -> R.color.hazard_debris
            "accident" -> R.color.hazard_accident
            "partially_passable" -> R.color.hazard_partially
            else -> R.color.hazard_unknown
        }
    }

    /**
     * Get display name for status
     */
    fun getStatusDisplayName(): String {
        return when (status) {
            Status.PENDING -> "Pending Verification"
            Status.FLAGGED -> "Under Review"
            Status.CONFIRMED -> "Confirmed"
            Status.FALSE -> "False Report"
            Status.INCONCLUSIVE -> "Inconclusive"
        }
    }
}