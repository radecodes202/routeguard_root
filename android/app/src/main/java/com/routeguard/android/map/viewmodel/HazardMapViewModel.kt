// map/viewmodel/HazardMapViewModel.kt
package com.routeguard.android.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routeguard.android.data.AuthRepository
import com.routeguard.android.data.ReportsRepository
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Hazard Map screen
 * Handles fetching nearby hazards, user location, and map state
 */
@HiltViewModel
class HazardMapViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<AuthUiState.LoginSuccess>(
        AuthUiState.LoginSuccess(
            user = com.routeguard.android.ui.auth.AuthUiState.User(
                id = "temp",
                email = "temp@test.com",
                fullName = "Test User",
                role = "commuter",
                reputationScore = 50.0,
                isActive = true
            )
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Hazards data
    private val _hazards = MutableStateFlow<List<HazardMapper>>(emptyList())
    val hazards: StateFlow<List<HazardMapper>> = _hazards.asStateFlow()

    // User location
    private val _userLocation = MutableStateFlow<HazardMapper.UserLocation?>(null)
    val userLocation: StateFlow<HazardMapper.UserLocation?> = _userLocation.asStateFlow()

    init {
        // Initialize with mock data for now
        initializeMockData()
    }

    private fun initializeMockData() {
        // In a real implementation, this would fetch from the backend
        // For now, we'll create some mock hazards around a default location

        val mockHazards = listOf(
            HazardMapper(
                id = "1",
                reporterId = "user1",
                category = "flooded",
                description = "Severe flooding on Main St",
                status = HazardMapper.Status.CONFIRMED,
                confirmCount = 15,
                denyCount = 2,
                confidenceScore = 95.0,
                latitude = 11.2444,
                longitude = 125.0044,
                distance = 320.0,
                createdAt = "2026-08-07T10:00:00Z"
            ),
            HazardMapper(
                id = "2",
                reporterId = "user2",
                category = "accident",
                description = "Multi-vehicle accident on Highway 1",
                status = HazardMapper.Status.FLAGGED,
                confirmCount = 8,
                denyCount = 12,
                confidenceScore = 40.0,
                latitude = 11.2500,
                longitude = 125.0100,
                distance = 1800.0,
                createdAt = "2026-08-07T09:30:00Z"
            ),
            HazardMapper(
                id = "3",
                reporterId = "user3",
                category = "debris",
                description = "Fallen tree blocking lane",
                status = HazardMapper.Status.PENDING,
                confirmCount = 3,
                denyCount = 1,
                confidenceScore = 75.0,
                latitude = 11.2380,
                longitude = 124.9980,
                distance = 850.0,
                createdAt = "2026-08-07T09:45:00Z"
            )
        )

        _hazards.value = mockHazards
        _userLocation.value = HazardMapper.UserLocation(
            latitude = 11.2400,
            longitude = 125.0000,
            accuracy = 10.0
        )
    }

    /**
     * Fetch nearby hazards from backend
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param radius Search radius in meters (default 5000 for 5km)
     */
    fun fetchNearbyHazards(latitude: Double, longitude: Double, radius: Double = 5000.0) {
        viewModelScope.launch {
            try {
                val hazardList = reportsRepository.getNearbyHazards(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius
                )
                _hazards.value = hazardList
                _userLocation.value = HazardMapper.UserLocation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = 10.0
                )
            } catch (e: Exception) {
                // In a real app, we'd handle errors properly
                e.printStackTrace()
                // Fallback to mock data on error
                initializeMockData()
            }
        }
    }

    /**
     * Refresh hazards data
     */
    fun refreshHazards() {
        val currentLocation = _userLocation.value
        if (currentLocation != null) {
            fetchNearbyHazards(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                radius = 5000.0
            )
        }
    }

    /**
     * Handle hazard click
     * @param hazardId ID of clicked hazard
     */
    fun onHazardClicked(hazardId: String) {
        // TODO: Navigate to hazard detail screen
        // This would typically involve navigating to a detail screen
        // with full hazard information and confirm/deny options
    }
}