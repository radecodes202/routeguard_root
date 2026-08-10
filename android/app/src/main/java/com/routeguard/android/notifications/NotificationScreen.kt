package com.routeguard.android.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routeguard.android.R
import com.routeguard.android.map.HazardMapper
import com.routeguard.android.map.viewmodel.HazardMapViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NotificationScreenContent()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NotificationScreenContent(
        viewModel: HazardMapViewModel = hiltViewModel()
    ) {
        val hazards by viewModel.hazards.collectAsStateWithLifecycle()
        val context = androidx.compose.ui.platform.LocalContext.current

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.nearby_hazards)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            if (hazards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No nearby hazards reported.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hazards) { hazard ->
                        HazardNotificationItem(
                            hazard = hazard,
                            onClick = { 
                                val intent = android.content.Intent(context, com.routeguard.android.map.HazardDetailScreen::class.java)
                                intent.putExtra("HAZARD_ID", hazard.id)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun HazardNotificationItem(
        hazard: HazardMapper,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(HazardMapper.getIconForCategory(hazard.category)),
                    contentDescription = hazard.category,
                    tint = colorResource(HazardMapper.getColorForCategory(hazard.category)),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hazard.description ?: "No description",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f km away", hazard.distance / 1000),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusIcon(hazard.status)
            }
        }
    }

    @Composable
    private fun StatusIcon(status: HazardMapper.Status) {
        val (icon, color) = when (status) {
            HazardMapper.Status.PENDING -> Icons.Default.Warning to MaterialTheme.colorScheme.errorContainer
            HazardMapper.Status.CONFIRMED -> Icons.Default.Error to MaterialTheme.colorScheme.error
            HazardMapper.Status.FLAGGED -> Icons.Default.Flag to MaterialTheme.colorScheme.secondary
            else -> Icons.Default.Warning to MaterialTheme.colorScheme.outline
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }

    @Composable
    fun colorResource(id: Int): androidx.compose.ui.graphics.Color {
        val context = androidx.compose.ui.platform.LocalContext.current
        return androidx.compose.ui.graphics.Color(androidx.core.content.ContextCompat.getColor(context, id))
    }
}
