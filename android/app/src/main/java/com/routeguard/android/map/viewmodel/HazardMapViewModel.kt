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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val _uiState = MutableStateFlow<AuthUiState>(
        AuthUiState.LoginSuccess(
            user = AuthUiState.User(
                id = "demo-123",
                email = "demo@routeguard.com",
                fullName = "Demo User",
                role = "user",
                reputationScore = 50.0,
                isActive = true
            )
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Hazards data - Observe directly from repository
    val hazards: StateFlow<List<HazardMapper>> = reportsRepository.hazards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User location
    private val _userLocation = MutableStateFlow<HazardMapper.UserLocation?>(
        HazardMapper.UserLocation(
            latitude = 11.2400,
            longitude = 125.0000,
            accuracy = 10.0
        )
    )
    val userLocation: StateFlow<HazardMapper.UserLocation?> = _userLocation.asStateFlow()

    // Selected location for reporting
    private val _selectedLocation = MutableStateFlow<android.graphics.PointF?>(null)
    val selectedLocation: StateFlow<android.graphics.PointF?> = _selectedLocation.asStateFlow()

    fun onMapClick(latOffset: Float, lngOffset: Float) {
        _selectedLocation.value = android.graphics.PointF(latOffset, lngOffset)
    }

    fun clearSelection() {
        _selectedLocation.value = null
    }

    /**
     * Fetch nearby hazards from backend
     */
    fun fetchNearbyHazards(latitude: Double, longitude: Double, radius: Double = 5000.0) {
        viewModelScope.launch {
            try {
                reportsRepository.getNearbyHazards(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius
                )
                _userLocation.value = HazardMapper.UserLocation(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = 10.0
                )
            } catch (e: Exception) {
                // Ignore in demo mode
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

    fun retryLogin() {
        // Implementation
    }
}
