// map/HazardMapScreen.kt
package com.routeguard.android.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeguard.android.R
import com.routeguard.android.map.viewmodel.HazardMapViewModel
import com.routeguard.android.ui.auth.AuthUiState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HazardMapScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HazardMapScreenContent()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun HazardMapScreenContent(
        viewModel: HazardMapViewModel = hiltViewModel()
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val hazards by viewModel.hazards.collectAsStateWithLifecycle()
        val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
        val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()
        val context = androidx.compose.ui.platform.LocalContext.current

        val scaleFactor = 20000f // Adjusted scale for demo markers

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { 
                            val intent = android.content.Intent(context, com.routeguard.android.notifications.NotificationScreen::class.java)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(if (selectedLocation != null) "Report at Pin" else stringResource(R.string.report_obstacle)) },
                    icon = { Icon(if (selectedLocation != null) Icons.Default.PushPin else Icons.Default.AddAlert, contentDescription = null) },
                    onClick = {
                        val intent = android.content.Intent(context, com.routeguard.android.report.ReportBottomSheet::class.java)
                        selectedLocation?.let {
                            val userLat = userLocation?.latitude ?: 11.2400
                            val userLng = userLocation?.longitude ?: 125.0000
                            intent.putExtra("latitude", userLat + it.x)
                            intent.putExtra("longitude", userLng + it.y)
                        }
                        context.startActivity(intent)
                        viewModel.clearSelection()
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (uiState) {
                    is AuthUiState.LoginLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is AuthUiState.LoginSuccess -> {
                        HazardMapContent(
                            hazards = hazards,
                            userLocation = userLocation,
                            selectedLocation = selectedLocation,
                            onMapTap = { latOff, lngOff -> 
                                viewModel.onMapClick(latOff, lngOff)
                            },
                            onHazardClicked = { id -> 
                                val intent = android.content.Intent(context, com.routeguard.android.map.HazardDetailScreen::class.java)
                                intent.putExtra("HAZARD_ID", id)
                                context.startActivity(intent)
                            },
                            onMapLongPress = { lat, lng -> /* TODO */ },
                            scaleFactor = scaleFactor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        // Handle other states
                    }
                }
            }
        }
    }

    @Composable
    private fun HazardMapContent(
        hazards: List<HazardMapper>,
        userLocation: HazardMapper.UserLocation?,
        selectedLocation: android.graphics.PointF?,
        onMapTap: (Float, Float) -> Unit,
        onHazardClicked: (String) -> Unit,
        onMapLongPress: (Double, Double) -> Unit,
        scaleFactor: Float,
        modifier: Modifier = Modifier
    ) {
        // Improved Map View Placeholder
        BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            
            // Grid and tap detection layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(scaleFactor) {
                        detectTapGestures { offset ->
                            val latOffset = (size.height / 2f - offset.y) / (scaleFactor * density)
                            val lngOffset = (offset.x - size.width / 2f) / (scaleFactor * density)
                            onMapTap(latOffset, lngOffset)
                        }
                    }
            ) {
                // Draw a simple grid to simulate a map
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridStep = 50.dp.toPx()
                    for (x in 0..size.width.toInt() step gridStep.toInt()) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f),
                            end = androidx.compose.ui.geometry.Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..size.height.toInt() step gridStep.toInt()) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()),
                            end = androidx.compose.ui.geometry.Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }
            }

            // User Location Marker (Center)
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .offset(y = (-12).dp)
            )

            // Show hazard markers (simplified)
            hazards.forEach { hazard ->
                // Map coordinates to pixels relative to user location
                val latDiff = (hazard.latitude - (userLocation?.latitude ?: 11.2400))
                val lngDiff = (hazard.longitude - (userLocation?.longitude ?: 125.0000))
                
                // Offset calculation for demo screen positioning
                val xOffset = screenWidth / 2 + (lngDiff * scaleFactor).toFloat().dp
                val yOffset = screenHeight / 2 - (latDiff * scaleFactor).toFloat().dp

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = colorResource(HazardMapper.getColorForCategory(hazard.category)),
                    modifier = Modifier
                        .offset(x = xOffset - 16.dp, y = yOffset - 32.dp)
                        .size(32.dp)
                        .clickable { onHazardClicked(hazard.id) }
                )
            }

            // Show selected location marker
            selectedLocation?.let {
                val xOffset = screenWidth / 2 + (it.y * scaleFactor).dp
                val yOffset = screenHeight / 2 - (it.x * scaleFactor).dp

                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Selected Location",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = xOffset - 16.dp, y = yOffset - 32.dp)
                        .size(32.dp)
                )
            }

            Text(
                text = "Tap map to pick location",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun colorResource(id: Int): androidx.compose.ui.graphics.Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.ui.graphics.Color(androidx.core.content.ContextCompat.getColor(context, id))
}
