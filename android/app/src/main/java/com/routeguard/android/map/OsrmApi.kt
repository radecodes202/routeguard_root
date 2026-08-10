package com.routeguard.android.map

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for OSRM (Open Source Routing Machine) service
 */
interface OsrmApi {

    @GET("/route/v1/driving/{coordinates}")
    fun getRoute(
        @Query("alternatives") alternatives: String = "false",
        @Query("steps") steps: String = "true",
        @Query("geometries") geometries: String = "polyline",
        @Query("overview") overview: String = "simplified",
        @Path("coordinates") coordinates: String
    ): Call<OsrmRouteResponse>

    @GET("/table/v1/driving/{coordinates}")
    fun getTable(
        @Query("annotations") annotations: String = "duration,distance",
        @Path("coordinates") coordinates: String
    ): Call<OsrmTableResponse>
}

data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute>,
    val waypoints: List<OsrmWaypoint>
) {
    data class OsrmRoute(
        val geometry: String,
        val legs: List<OsrmLeg>,
        val distance: Double,
        val duration: Double,
        val weight: Double
    ) {
        data class OsrmLeg(
            val steps: List<OsrmStep>,
            val distance: Double,
            val duration: Double,
            val summary: String
        ) {
            data class OsrmStep(
                val distance: Double,
                val duration: Double,
                val geometry: String,
                val name: String?,
                val maneuver: OsrmManeuver?
            ) {
                data class OsrmManeuver(
                    val bearing_after: Double,
                    val bearing_before: Double,
                    val location: List<Double>,
                    val modifier: String?,
                    val type: String,
                    val instruction: String?
                )
            }
        }
    }

    data class OsrmWaypoint(
        val hint: String,
        val distance: Double,
        val name: String,
        val location: List<Double>
    )
}

data class OsrmTableResponse(
    val code: String,
    val distances: List<List<Double>>,
    val durations: List<List<Double>>,
    val sources: List<OsrmRouteResponse.OsrmWaypoint>,
    val destinations: List<OsrmRouteResponse.OsrmWaypoint>
)
