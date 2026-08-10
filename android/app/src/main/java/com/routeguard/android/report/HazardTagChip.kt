package com.routeguard.android.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.routeguard.android.map.HazardMapper

/**
 * Reusable chip component for selecting hazard tags
 * Part of Feature 6: Tag-based reporting interface (<10s)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HazardTagChip(
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = tag.replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(HazardMapper.getIconForCategory(tag)),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier.padding(horizontal = 4.dp)
    )
}
