package com.routeguard.android.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.routeguard.android.R
import com.routeguard.android.map.viewmodel.HazardMapViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HazardDetailScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hazardId = intent.getStringExtra("HAZARD_ID") ?: ""
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HazardDetailContent(hazardId)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun HazardDetailContent(
        hazardId: String,
        viewModel: HazardMapViewModel = hiltViewModel()
    ) {
        val hazards by viewModel.hazards.collectAsStateWithLifecycle()
        val hazard = hazards.find { it.id == hazardId }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Hazard Details") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (hazard == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Hazard not found")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Category Icon and Title
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(HazardMapper.getIconForCategory(hazard.category)),
                            contentDescription = hazard.category,
                            tint = colorResource(HazardMapper.getColorForCategory(hazard.category)),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = hazard.category.replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            StatusBadge(hazard.status)
                        }
                    }

                    // Mock Image (or real if available)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // In demo, we might not have the real image URL from backend yet
                        // Just show a placeholder icon or mock image
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "Evidence Photo Placeholder",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text(
                        text = "Description",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hazard.description ?: "No description provided.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Details Grid
                    DetailItem(Icons.Default.LocationOn, "Location", String.format(java.util.Locale.US, "%.4f, %.4f", hazard.latitude, hazard.longitude))
                    DetailItem(Icons.Default.Straighten, "Distance", String.format(java.util.Locale.US, "%.1f km away", hazard.distance / 1000))
                    DetailItem(Icons.Default.AccessTime, "Reported at", hazard.createdAt)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Interaction Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { /* TODO: Upvote */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Default.ThumbUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm")
                        }
                        Button(
                            onClick = { /* TODO: Downvote */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Default.ThumbDown, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deny")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusBadge(status: HazardMapper.Status) {
        val color = when (status) {
            HazardMapper.Status.CONFIRMED -> MaterialTheme.colorScheme.error
            HazardMapper.Status.PENDING -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline
        }
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp),
            border = borderStroke(1.dp, color.copy(alpha = 0.5f))
        ) {
            Text(
                text = status.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun DetailItem(icon: ImageVector, label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Composable
    fun colorResource(id: Int): androidx.compose.ui.graphics.Color {
        val context = androidx.compose.ui.platform.LocalContext.current
        return androidx.compose.ui.graphics.Color(androidx.core.content.ContextCompat.getColor(context, id))
    }

    @Composable
    private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color) = 
        androidx.compose.foundation.BorderStroke(width, color)
}
