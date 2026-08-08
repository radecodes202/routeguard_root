package com.routeguard.android.map

import com.routeguard.android.map.HazardMapper
import retrofit2.Response
import retrofit2.run

/**
 * Repository for OSRM service operations
 */
class OsrmRepository(
    private val osrmApi: OsrmApi
) {

    suspend fun getRoute(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        alternatives: Boolean = false
    ): OsrmRouteResponse {
        val coordinates = "${startLongitude},${startLatitude};${endLongitude},${endLatitude}"
        val response: Response<OsrmRouteResponse> = osrmApi.getRoute(
            alternatives = if (alternatives) "true" else "false",
            coordinates = coordinates
        ).execute()

        if (response.isSuccessful && response.body() != null && response.body()!!.code == "Ok") {
            return response.body()!!
        } else {
            // Return an error response - in a real implementation, we might want to throw or handle differently
            return OsrmRouteResponse(
                code = "Error",
                routes = emptyList(),
                waypoints = emptyList()
            )
        }
    }

    suspend fun getRouteAvoidingHazards(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        hazards: List<HazardMapper> = emptyList()
    ): OsrmRouteResponse {
        // For Phase 1, we'll just call the regular route method
        // In a full implementation, we would use OSRM's barrier feature to avoid hazards
        // This would involve converting hazards to barrier coordinates and adding them to the request
        return getRoute(startLatitude, startLongitude, endLatitude, endLongitude)
    }

    suspend fun getDistanceMatrix(
        points: List<Pair<Double, Double>> // List of latitude, longitude pairs
    ): OsrmTableResponse {
        val coordinates = points.map { (lat, lng) -> "$lng,$lat" }.joinToString(";")
        val response: Response<OsrmTableResponse> = osrmApi.getTable(
            coordinates = coordinates
        ).execute()

        if (response.isSuccessful && response.body() != null && response.body()!!.code == "Ok") {
            return response.body()!!
        } else {
            // Return an error response
            return OsrmTableResponse(
                code = "Error",
                distances = emptyList(),
                durations = emptyList(),
                sources = emptyList(),
                destinations = emptyList()
            )
        }
    }
}