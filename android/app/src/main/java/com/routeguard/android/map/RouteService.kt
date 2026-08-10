package com.routeguard.android.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeguard.android.util.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                _routeResult.value = OsrmRouteResponse(
                    code = "Table",
                    routes = emptyList(),
                    waypoints = emptyList()
                )
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
