// map/ui/HazardMap.kt
package com.routeguard.android.map.ui

import android.content.Context
import androidx.compose.platform.android.ComposeView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.RememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mapbox.maps.Bitmap
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.geojson.GeoJsonManager
import com.mapbox.maps.extension.style.layers.geojson.GeoJsonSource
import com.mapbox.maps.extension.style.layers.geojson.geojsonLayer
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.AnnotationPluginImplKt
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationStateKt
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.GesturesPluginKt
import com.routeguard.android.map.HazardMapper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharedFlow

/**
 * Map component using MapLibre GL Native (via Mapbox Maps SDK for Android)
 * Displays hazard markers and user location on an interactive map
 */
@Composable
fun HazardMap(
    hazards: List<HazardMapper>,
    userLocation: HazardMapper.UserLocation?,
    onHazardClicked: (String) -> Unit,
    onMapLongPress: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val annotationsPlugin = remember { AnnotationPluginImplKt.getAnnotationsPlugin(mapView) }
    val gesturesPlugin = remember { GesturesPluginKt.getGesturesPlugin(mapView) }
    val pointAnnotationManager: PointAnnotationManager? = remember {
        val plugin = AnnotationPluginImplKt.getAnnotationsPlugin(mapView)
        PointAnnotationManagerKt.createPointAnnotationManager(plugin, mapView.mapboxMap)
    }

    // Update user location on map
    DisposableEffect(userLocation) {
        if (userLocation != null) {
            val cameraOptions = CameraOptions.Builder()
                .center(
                    com.mapbox.maps.Point.fromLngLat(
                        userLocation!!.longitude,
                        userLocation!!.latitude
                    )
                )
                .zoom(14.0)
                .build()
            mapView.mapboxMap?.camera?.setCameraOptions(cameraOptions)
        }
        // Dispose effect when userLocation changes
        onDispose { }
    }

    // Update hazard markers on map
    DisposableEffect(hazards) {
        pointAnnotationManager?.deleteAll()
        hazards.forEach { hazard ->
            val options = PointAnnotationOptions()
                .withPosition(
                    com.mapbox.maps.Point.fromLngLat(
                        hazard.longitude,
                        hazard.latitude
                    )
                )
                .withIconImage(getHazardIcon(hazard))
                .withIconSize(1.2)
                .withDraggable(false)
                .withSelected(false)
            pointAnnotationManager?.create(options)
        }
        onDispose { }
    }

    // Handle map clicks
    DisposableEffect(Unit) {
        val mapboxMap = mapView.mapboxMap
        mapboxMap?.gestures?.addOnMapClickListener { point ->
            // Convert screen point to lat/lng
            val latLng = mapboxMap?.projection?.fromScreenPoint(
                com.mapbox.maps.Point.fromLngLat(point.x, point.y)
            )
            latLng?.let { lngLat ->
                // Find closest hazard (simplified - in production you'd want proper hit testing)
                hazards.firstOrDefault { hazard ->
                    Math.abs(hazard.latitude - lngLat.latitude) < 0.01 &&
                            Math.abs(hazard.longitude - lngLat.longitude) < 0.01
                }?.let { hazard ->
                    onHazardClicked(hazard.id)
                }
            }
            true // Consume the event
        }
        onDispose { }
    }

    // Handle long press for reporting
    DisposableEffect(Unit) {
        val mapboxMap = mapView.mapboxMap
        mapboxMap?.gestures?.addOnLongPressListener { point ->
            val latLng = mapboxMap?.projection?.fromScreenPoint(
                com.mapbox.maps.Point.fromLngLat(point.x, point.y)
            )
            latLng?.let { lngLat ->
                onMapLongPress(lngLat.latitude, lngLat.longitude)
            }
            true // Consume the event
        }
        onDispose { }
    }

    // Android View holding the MapView
    android.view.views.View(
        modifier = modifier
    ) {
        val view = composeView.context
        val mapView = MapView(view)
        val frameLayout = android.widget.FrameLayout(view)
        frameLayout.addView(mapView)
        addView(frameLayout)

        // Initialize map with custom style URL for self-hosted tiles or open provider
        DisposableEffect(Unit) {
            mapView.getMapAsync { mapboxMap ->
                // Use string resources for API keys and style URLs
                val apiKey = context.getString(R.string.maptiler_api_key)
                val styleUrlFormat = context.getString(R.string.maptiler_streets_style_url)
                val styleUrl = String.format(styleUrlFormat, apiKey)

                mapboxMap?.loadStyleUri(styleUrl) { style ->
                    // Style loaded successfully
                    // Add any custom style configurations here
                }
            }
            onDispose { mapView.onDestroy() }
        }

        // Map lifecycle
        DisposableEffect(Unit) {
            mapView.onCreate(null)
            mapView.onStart()
            mapView.onResume()
            onDispose {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }
}

/**
 * Get appropriate icon for hazard category
 * In a real implementation, this would map to actual drawable resources
 */
private fun getHazardIcon(hazard: HazardMapper): String {
    return when (hazard.category) {
        "flooded" -> "hazard-flooded"
        "fully_blocked" -> "hazard-blocked"
        "debris" -> "hazard-debris"
        "accident" -> "hazard-accident"
        "partially_passable" -> "hazard-partially"
        else -> "hazard-unknown"
    }
}