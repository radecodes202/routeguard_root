// map/HazardMapScreen.kt
package com.routeguard.android.map

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.routeguard.android.R
import com.routeguard.android.report.ReportBottomSheet
import com.routeguard.android.ui.auth.AuthUiState
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.map.ui.HazardMap
import com.routeguard.android.map.ui.HazardMarker
import com.routeguard.android.map.ui.RoomSelector
import com.routeguard.android.map.viewmodel.HazardMapViewModel

/**
 * Main hazard map screen showing nearby hazards within 5km
 * Implements SO1: Detect obstacles within a 5-kilometer radius of a user
 */
class HazardMapScreen : androidx.compose.material3.ScaffoldScreen() {

    private val viewModel: HazardMapViewModel = hiltViewModel()

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HazardMapTheme {
                HazardMapScreenContent(viewModel)
            }
        }
    }

    @Composable
    private fun HazardMapScreenContent(viewModel: HazardMapViewModel) {
        val uiState by viewModel.uiState.collectAsLifecycleEffect()
        val hazards by viewModel.hazards.collectAsLifecycleEffect()
        val userLocation by viewModel.userLocation.collectAsLifecycleEffect()

        Scaffold(
            topBar = {
                CenterAlignedTopBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { /* TODO: Open settings */ }) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.report_obstacle)) },
                    icon = { Icon(Icons.Default.AddAlert, contentDescription = null) },
                    onClick = {
                        // Show the report bottom sheet when FAB is clicked
                        ReportBottomSheet(
                            onDismissRequest = {} // Handle dismiss if needed
                        )
                    }
                )
            }
        ) { padding ->
            when (uiState) {
                is AuthUiState.LoginLoading -> {
                    CenteredCircularProgressModifier(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding))
                }
                is AuthUiState.LoginError -> {
                    ErrorState(
                        message = uiState.message,
                        onRetry = { viewModel.retryLogin() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
                is AuthUiState.LoginSuccess -> {
                    HazardMapContent(
                        hazards = uiState.hazards ?: emptyList(),
                        userLocation = uiState.userLocation,
                        onHazardClicked = { hazardId ->
                            // TODO: Navigate to hazard detail screen
                        },
                        onMapLongPress = { lat, lng ->
                            // TODO: Show context menu or start reporting from this location
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
                else -> {
                    // Show loading state for other auth states
                    CenteredCircularProgressModifier(modifier = Modifier
                        .fillMaxSize()
                        .padding(padding))
                }
            }
        }
    }

    @Composable
    private fun HazardMapContent(
        hazards: List<HazardMapper>,
        userLocation: HazardMapper.UserLocation?,
        onHazardClicked: (String) -> Unit,
        onMapLongPress: (Double, Double) -> Unit,
        modifier: Modifier = Modifier
    ) {
        HazardMap(
            hazards = hazards,
            userLocation = userLocation,
            onHazardClicked = onHazardClicked,
            onMapLongPress = onMapLongPress,
            modifier = modifier
        )

        // Show nearby hazards list as bottom sheet or panel
        if (hazards.isNotEmpty()) {
            HazardListPanel(
                hazards = hazards.take(5), // Show top 5 closest
                onHazardClicked = onHazardClicked,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    @Composable
    private fun HazardListPanel(
        hazards: List<HazardMapper>,
        onHazardClicked: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier
                .width(Width.InPercent)
                .height(120.dp)
                .shadow(4.dp),
            shape = MaterialShapes.mediumShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.nearby_hazards),
                    style = MaterialTypography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(hazards) { hazard ->
                        HazardListItem(
                            hazard = hazard,
                            onClick = { onHazardClicked(hazard.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    @Composable
    private fun HazardListItem(
        hazard: HazardMapper,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hazard icon based on category
            Icon(
                imageVector = HazardMapper.getIconForCategory(hazard.category),
                contentDescription = hazard.category,
                tint = HazardMapper.getColorForCategory(hazard.category),
                modifier = Modifier
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = hazard.description.take(30) + if (hazard.description.length > 30) "..." else "",
                    style = MaterialTypography.bodyMedium
                )
                Text(
                    text = "${(hazard.distance / 1000).format("%.1f")} ${stringResource(R.string.km_away)}",
                    style = MaterialTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Status indicator
            when (hazard.status) {
                HazardMapper.Status.PENDING -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.warning,
                        modifier = Modifier.size(16.dp)
                    )
                }
                HazardMapper.Status.CONFIRMED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
                HazardMapper.Status.FLAGGED -> {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}