package com.routeguard.android.report

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.routeguard.android.data.AuthRepository
import com.routeguard.android.data.ReportsRepository
import com.routeguard.android.location.LocationManager
import com.routeguard.android.ui.auth.AuthUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val authRepository: AuthRepository,
    private val locationManager: LocationManager,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Ready)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _mediaUri = MutableStateFlow<Uri?>(null)
    val mediaUri: StateFlow<Uri?> = _mediaUri.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _customLocation = MutableStateFlow<android.graphics.PointF?>(null)

    fun setCustomLocation(lat: Double, lng: Double) {
        _customLocation.value = android.graphics.PointF(lat.toFloat(), lng.toFloat())
    }

    private val _reportResult = MutableStateFlow<ReportResult?>(null)
    val reportResult: StateFlow<ReportResult?> = _reportResult.asStateFlow()

    private val _lastReportTime = MutableStateFlow<Long>(0)
    private val DEBOUNCE_DELAY_MS = 10000L

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { authUiState ->
                when (authUiState) {
                    is AuthUiState.CurrentUserSuccess -> _uiState.value = ReportUiState.Ready
                    is AuthUiState.Unauthorized -> _uiState.value = ReportUiState.RequiresLogin
                    else -> _uiState.value = ReportUiState.Ready
                }
            }
        }
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
    }

    fun selectMedia(uri: Uri?) {
        _mediaUri.value = uri
    }

    fun updateDescription(text: String) {
        _description.value = text
    }

    fun submitReport(description: String? = null) {
        val tag = _selectedTag.value ?: run {
            _uiState.value = ReportUiState.Error("Please select a hazard type")
            return
        }
        val mediaUri = _mediaUri.value ?: run {
            _uiState.value = ReportUiState.Error("Please attach a photo as evidence")
            return
        }
        
        val finalDescription = description ?: _description.value
        
        if (!locationManager.hasLocationPermission()) {
            _uiState.value = ReportUiState.PermissionRequired
            return
        }

        if (System.currentTimeMillis() - _lastReportTime.value < DEBOUNCE_DELAY_MS) {
            _uiState.value = ReportUiState.Error("Please wait")
            return
        }

        _uiState.value = ReportUiState.Submitting

        viewModelScope.launch {
            val location = _customLocation.value?.let {
                val loc = android.location.Location("custom")
                loc.latitude = it.x.toDouble()
                loc.longitude = it.y.toDouble()
                loc
            } ?: locationManager.getLastKnownLocation()

            val response = reportsRepository.createReport(
                category = tag,
                description = finalDescription,
                latitude = location?.latitude ?: 0.0,
                longitude = location?.longitude ?: 0.0,
                mediaUri = mediaUri
            )

            if (response.success) {
                _lastReportTime.value = System.currentTimeMillis()
                _uiState.value = ReportUiState.Success
            } else {
                _uiState.value = ReportUiState.Error(response.error?.message ?: "Failed")
            }
        }
    }

    fun handlePermissionResult(granted: Boolean, description: String?, mediaUri: Uri?) {
        if (granted) {
            _mediaUri.value = mediaUri
            submitReport(description)
        } else {
            _uiState.value = ReportUiState.Error("Permission required")
        }
    }

    fun setError(message: String) {
        _uiState.value = ReportUiState.Error(message)
    }

    fun resetState() {
        _uiState.value = ReportUiState.Ready
    }

    sealed class ReportUiState {
        object Ready : ReportUiState()
        object RequiresLogin : ReportUiState()
        object Submitting : ReportUiState()
        object PermissionRequired : ReportUiState()
        data class Error(val message: String) : ReportUiState()
        object Success : ReportUiState()
    }

    sealed class ReportResult {
        data class Success(val report: com.routeguard.android.data.remote.dto.HazardReportResponse.HazardReportData.HazardReport) : ReportResult()
        data class Error(val message: String) : ReportResult()
    }
}
