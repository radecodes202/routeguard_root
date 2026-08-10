package com.routeguard.android.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.routeguard.android.map.HazardMapper

@Composable
fun HazardMap(
    hazards: List<HazardMapper>,
    userLocation: HazardMapper.UserLocation?,
    onHazardClicked: (String) -> Unit,
    onMapLongPress: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Map Placeholder (Demo Mode)\nFound ${hazards.size} hazards nearby",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
