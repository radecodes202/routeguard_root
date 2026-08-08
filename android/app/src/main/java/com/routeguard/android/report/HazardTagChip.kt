package com.routeguard.android.report

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.routeguard.android.R
import com.routeguard.android.map.HazardMapper

/**
 * Reusable chip component for selecting hazard tags
 * Part of Feature 6: Tag-based reporting interface (<10s)
 */
@Composable
fun HazardTagChip(
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (selected) colors.primary else colors.outline
    val backgroundColor = if (selected) colors.primaryContainer else colors.surface
    val textColor = if (selected) colors.onPrimaryContainer else colors.onSurface

    Chip(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(4.dp),
        label = {
            Text(
                text = tag.replace('_', ' ').titlecase(),
                color = textColor,
                style = MaterialTypography.labelSmall
            )
        },
        icon = {
            Icon(
                imageVector = HazardMapper.getIconForCategory(tag),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        },
        leadingIconTint = contentColorFor(backgroundColor),
        enabled = true
    )
}

// Extension to convert string to title case
private fun String.titlecase(): String {
    if (this.isEmpty()) return this
    return this.substring(0, 1).uppercase() + this.substring(1).lowercase()
}