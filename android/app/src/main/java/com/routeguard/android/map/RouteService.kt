package com.routeguard.android.map

import com.routeguard.android.map.HazardMapper
import com.routeguard.android.util.LocationUtils
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.List

/**
 * Service for interacting with OSRM (Open Source Routing Machine) for route calculation
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
                    val r#type: String,
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
    val sources: List<OsrmWaypoint>,
    val destinations: List<OsrmWaypoint>
)

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
        val response = osrmApi.getRoute(
            alternatives = if (alternatives) "true" else "false",
            coordinates = coordinates
        ).execute()

        if (response.isSuccessful && response.body() != null && response.body()!!.code == "Ok") {
            return response.body()!!
        } else {
            // Return a mock response for now - in real implementation, handle errors properly
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
        // For now, we'll just call the regular route method
        // In a full implementation, we would use OSRM's barrier feature to avoid hazards
        return getRoute(startLatitude, startLongitude, endLatitude, endLongitude)
    }

    suspend fun getDistanceMatrix(
        points: List<Pair<Double, Double>> // List of latitude, longitude pairs
    ): OsrmTableResponse {
        val coordinates = points.map { (lat, lng) -> "$lng,$lat" }.joinToString(";")
        val response = osrmApi.getTable(
            coordinates = coordinates
        ).execute()

        if (response.isSuccessful && response.body() != null && response.body()!!.code == "Ok") {
            return response.body()!!
        } else {
            // Return a mock response for now
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

/**
 * ViewModel for handling routing functionality
 */
class RouteViewModel @Inject constructor(
    private val osrmRepository: OsrmRepository
) : ViewModel() {

    private val _routeResult = MutableStateFlow<OsrmRouteResponse?>(null)
    val routeResult: StateFlow<OsrmRouteResponse?> = _routeResult.asStateFlow()

    private val _isRouting = MutableStateFlow(false)
    val isRouting: StateFlow<Boolean> = _isRouting.asStateFlow()

    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()

    fun calculateRoute(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        alternatives: Boolean = false
    ) {
        viewModelScope.launch {
            _isRouting.value = true
            _routeError.value = null
            try {
                val route = osrmRepository.getRoute(
                    startLatitude, startLongitude, endLatitude, endLongitude, alternatives
                )
                _routeResult.value = route
            } catch (e: Exception) {
                _routeError.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _isRouting.value = false
            }
        }
    }

    fun calculateRouteAvoidingHazards(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
        hazards: List<HazardMapper>
    ) {
        viewModelScope.launch {
            _isRouting.value = true
            _routeError.value = null
            try {
                val route = osrmRepository.getRouteAvoidingHazards(
                    startLatitude, startLongitude, endLatitude, endLongitude, hazards
                )
                _routeResult.value = route
            } catch (e: Exception) {
                _routeError.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _isRouting.value = false
            }
        }
    }

    fun calculateDistanceMatrix(points: List<Pair<Double, Double>>) {
        viewModelScope.launch {
            _isRouting.value = true
            _routeError.value = null
            try {
                val table = osrmRepository.getDistanceMatrix(points)
                // For now, we'll just store the result in routeResult for simplicity
                // In a real app, you might want a separate flow for table results
                // We'll convert it to a dummy route response for now
                _routeResult.value = OsrmRouteResponse(
                    code = "Table",
                    routes = emptyList(),
                    waypoints = emptyList()
                )
                // In a real implementation, you'd have a separate StateFlow for table data
            } catch (e: Exception) {
                _routeError.value = e.localizedMessage ?: "Unknown error"
            } finally {
                _isRouting.value = false
            }
        }
    }

    fun clearRoute() {
        _routeResult.value = null
        _routeError.value = null
    }
}