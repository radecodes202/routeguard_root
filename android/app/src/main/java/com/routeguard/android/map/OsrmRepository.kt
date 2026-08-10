package com.routeguard.android.map

import com.routeguard.android.util.AppConfig
import retrofit2.Response
import retrofit2.awaitResponse

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
        if (AppConfig.DEMO_MODE) {
            return OsrmRouteResponse(
                code = "Ok",
                routes = listOf(
                    OsrmRouteResponse.OsrmRoute(
                        geometry = "mj`yE_v|u@??", // Mock geometry
                        legs = emptyList(),
                        distance = 1500.0,
                        duration = 300.0,
                        weight = 300.0
                    )
                ),
                waypoints = emptyList()
            )
        }

        val coordinates = "${startLongitude},${startLatitude};${endLongitude},${endLatitude}"
        return try {
            val response = osrmApi.getRoute(
                alternatives = if (alternatives) "true" else "false",
                coordinates = coordinates
            ).awaitResponse()
            response.body() ?: OsrmRouteResponse("Error", emptyList(), emptyList())
        } catch (e: Exception) {
            OsrmRouteResponse("Error", emptyList(), emptyList())
        }
    }

    suspend fun getRouteAvoidingHazards(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        hazards: List<HazardMapper> = emptyList()
    ): OsrmRouteResponse {
        // In demo mode, just return the regular mock route
        return getRoute(startLatitude, startLongitude, endLatitude, endLongitude)
    }

    suspend fun getDistanceMatrix(
        points: List<Pair<Double, Double>> // List of latitude, longitude pairs
    ): OsrmTableResponse {
        if (AppConfig.DEMO_MODE) {
            return OsrmTableResponse(
                code = "Ok",
                distances = List(points.size) { List(points.size) { 100.0 } },
                durations = List(points.size) { List(points.size) { 60.0 } },
                sources = emptyList(),
                destinations = emptyList()
            )
        }

        val coordinates = points.map { (lat, lng) -> "$lng,$lat" }.joinToString(";")
        return try {
            val response = osrmApi.getTable(
                coordinates = coordinates
            ).awaitResponse()
            response.body() ?: OsrmTableResponse("Error", emptyList(), emptyList(), emptyList(), emptyList())
        } catch (e: Exception) {
            OsrmTableResponse("Error", emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
}
