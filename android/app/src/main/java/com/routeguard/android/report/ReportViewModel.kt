package com.routeguard.android.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelInitializer
import com.routeguard.android.data.ReportsRepository
import com.routeguard.android.data.AuthRepository
import com.routeguard.android.location.LocationManager
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for handling tag-based obstacle reporting
 * Implements FR-4, FR-5: Tag-based reporting interface (<10s)
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val authRepository: AuthRepository,
    private val locationManager: LocationManager,
    application: Application
) : AndroidViewModel(application) {

    // UI State
    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Ready)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    // Selected tag for reporting
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Selected media URI for reporting
    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri: StateFlow<Uri?> = _mediaUri.asStateFlow()

    // Report result
    private val _reportResult = MutableStateFlow<ReportResult?>(null)
    val reportResult: StateFlow<ReportResult?> = _reportResult.asStateFlow()

    // Debounce tracking to prevent duplicate reports
    private val _lastReportTime = MutableStateFlow<Long>(0)
    val lastReportTime: StateFlow<Long> = _lastReportTime.asStateFlow()

    // Debounce delay in milliseconds (10 seconds to prevent duplicate reports)
    private val DEBOUNCE_DELAY_MS = 10000L

    init {
        // Check authentication status on init
        checkAuthStatus()
        // Initialize last report time to allow immediate first report
        _lastReportTime.value = 0
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { authUiState ->
                when (authUiState) {
                    is com.routeguard.android.ui.auth.AuthUiState.CurrentUserSuccess -> {
                        // User is authenticated
                        _uiState.value = ReportUiState.Ready
                    }
                    is com.routeguard.android.ui.auth.AuthUiState.Unauthorized -> {
                        // User needs to log in
                        _uiState.value = ReportUiState.RequiresLogin
                    }
                    else -> {
                        // Loading or error state
                        _uiState.value = ReportUiState.Ready
                    }
                }
            }
        }
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
    }

    fun clearSelection() {
        _selectedTag.value = null
    }

    fun selectMedia(uri: Uri?) {
        _mediaUri.value = uri
    }

    fun clearMediaSelection() {
        _mediaUri.value = null
    }

    fun submitReport(description: String? = null, mediaUri: Uri? = null) {
        val tag = _selectedTag.value
        if (tag == null || tag.isEmpty()) {
            _uiState.value = ReportUiState.Error("Please select a hazard type")
            return
        }

        // Check if we have location permission
        if (!locationManager.hasLocationPermission()) {
            _uiState.value = ReportUiState.PermissionRequired
            return
        }

        // Check debounce - prevent duplicate reports within DEBOUNCE_DELAY_MS
        val currentTime = System.currentTimeMillis()
        val lastReportTimestamp = _lastReportTime.value
        if (currentTime - lastReportTimestamp < DEBOUNCE_DELAY_MS) {
            val remainingTime = (DEBOUNCE_DELAY_MS - (currentTime - lastReportTimestamp)) / 1000
            _uiState.value = ReportUiState.Error("Please wait $remainingTime seconds before submitting another report")
            return
        }

        _uiState.value = ReportUiState.Submitting

        viewModelScope.launch {
            // Get current location
            val currentLocation = locationManager.getLastKnownLocation()
            if (currentLocation == null) {
                _uiState.value = ReportUiState.Error("Unable to get current location")
                return@viewModelScope
            }

            val response = reportsRepository.createReport(
                category = tag,
                description = description,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                mediaUri = mediaUri
            )

            if (response.success) {
                // Update last report time for debounce
                _lastReportTime.value = System.currentTimeMillis()
                _uiState.value = ReportUiState.Success
                _reportResult.value = ReportResult.Success(response.data?.reports?.firstOrNull() ?:
                    com.routeguard.android.data.remote.dto.HazardReportResponse.HazardReportData.HazardReport(
                        id = "temp_id",
                        reporterId = "",
                        category = tag,
                        description = description,
                        locationWkt = "POINT(${currentLocation.longitude} ${currentLocation.latitude})",
                        status = "pending",
                        confirmCount = 0,
                        denyCount = 0,
                        confidenceScore = 100.0,
                        createdAt = java.util.Date().toString()
                    )
                )
            } else {
                _uiState.value = ReportUiState.Error(response.error?.message ?: "Failed to submit report")
                _reportResult.value = ReportResult.Error(response.error?.message ?: "Failed to submit report")
            }
        }
    }

    /**
     * Handle the result of a location permission request.
     * If permission is granted, attempt to submit the report with the given description and media URI.
     * If denied, show an error.
     */
    fun handlePermissionResult(granted: Boolean, description: String?, mediaUri: Uri?) {
        if (granted) {
            // Permission granted, submit the report
            submitReport(description, mediaUri)
        } else {
            // Permission denied
            _uiState.value = ReportUiState.Error("Location permission is required to report hazards")
        }
    }

    /**
     * Set an error state directly.
     * @param message The error message to display
     */
    fun setError(message: String) {
        _uiState.value = ReportUiState.Error(message)
    }

    fun reset() {
        _uiState.value = ReportUiState.Ready
        _selectedTag.value = null
        _mediaUri.value = null
        _reportResult.value = null
        // Optionally reset debounce timer when manually resetting
        // Commenting out to maintain debounce across resets for better UX
        // _lastReportTime.value = 0
    }

    // UI State Sealed Class
    sealed class ReportUiState {
        object Ready : ReportUiState()
        object RequiresLogin : ReportUiState()
        object Submitting : ReportUiState()
        object PermissionRequired : ReportUiState()
        data class Error(val message: String) : ReportUiState()
        object Success : ReportUiState()
    }

    // Report Result Sealed Class
    sealed class ReportResult {
        data class Success(val report: com.routeguard.android.data.remote.dto.HazardReportResponse.HazardReportData.HazardReport) : ReportResult()
        data class Error(val message: String) : ReportResult()
    }
}

// ViewModel initializer for Hilt
val reportViewModelInitializer = viewModelInitializer {
    reportViewModelFactory(create = { ReportViewModel(
        reportsRepository = get(),
        authRepository = get(),
        locationManager = get(),
        application = get()
    ) })
}