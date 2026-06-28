package com.bikeputer.ui.dashboard

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

private const val MIN_ZOOM = 14.0
private const val MAX_ZOOM = 17.0
private const val CAMERA_PADDING_PX = 80
/** Fixed zoom for plain follow mode (and the baseline restored when leaving fit-ahead). */
private const val FOLLOW_ZOOM = 16.0

private val CartoDark = object : OnlineTileSourceBase(
    "CartoDarkMatter", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
}

private val CartoLight = object : OnlineTileSourceBase(
    "CartoPositron", 0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/light_all/",
        "https://b.basemaps.cartocdn.com/light_all/",
        "https://c.basemaps.cartocdn.com/light_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getX(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
}

/** Selects the CARTO basemap matching the active theme. */
internal fun tileSource(isLight: Boolean): OnlineTileSourceBase =
    if (isLight) CartoLight else CartoDark

/**
 * A theme-aware route map. Tiles and the [route] track follow the active theme
 * (dark CARTO + bright teal in dark mode, light CARTO + deeper teal in light),
 * switching in place on a theme toggle. Follows [current] when [follow] is true,
 * marking the live position with the Compose overlay dot; shows an "acquiring
 * GPS" hint until the first fix arrives.
 */
@Composable
fun RouteMap(
    route: List<GeoPoint>,
    current: GeoPoint?,
    modifier: Modifier = Modifier,
    follow: Boolean = true,
    planned: List<GeoPoint> = emptyList(),
    aheadWindow: List<GeoPoint> = emptyList(),
    headingUp: Boolean = false,
    bearingDeg: Float? = null,
    frozen: Boolean = false,
    onUserPan: () -> Unit = {},
) {
    val c = LocalBikeColors.current
    val ctx = LocalContext.current

    val onUserPanState = rememberUpdatedState(onUserPan)

    val plannedLine = remember {
        Polyline().apply {
            outlinePaint.color = c.dim.toArgb()
            outlinePaint.strokeWidth = 8f
        }
    }
    val polyline = remember {
        Polyline().apply {
            outlinePaint.color = c.route.toArgb()
            outlinePaint.strokeWidth = 10f
        }
    }
    val mapView = remember {
        Configuration.getInstance().userAgentValue = ctx.packageName
        MapView(ctx).apply {
            setTileSource(tileSource(c.isLight))
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(FOLLOW_ZOOM)
            overlays.add(plannedLine)
            overlays.add(polyline)
            // Two-finger twist to rotate the map. The gesture's ACTION_MOVE events
            // also reach the touch listener below, which freezes following (so the
            // manual orientation persists until the rider taps recenter).
            overlays.add(RotationGestureOverlay(this).apply { setEnabled(true) })
            setOnTouchListener { _, event ->
                if (event.actionMasked == android.view.MotionEvent.ACTION_MOVE) {
                    onUserPanState.value()
                }
                false // observe, don't consume — osmdroid still pans/zooms
            }
        }
    }

    val positionDot = remember {
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(c.accent.toArgb())
            setSize(34, 34)
        }
    }
    val positionMarker = remember {
        Marker(mapView).apply {
            icon = positionDot
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setInfoWindow(null)
            setOnMarkerClickListener { _, _ -> true }
            isEnabled = false
        }.also { mapView.overlays.add(it) }
    }

    var lastIsLight by remember { mutableStateOf(c.isLight) }
    var wasFitAhead by remember { mutableStateOf(false) }
    var lastBearing by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                if (bearingDeg != null) lastBearing = bearingDeg
                val targetOrientation = if (headingUp) -lastBearing else 0f
                if (c.isLight != lastIsLight) {
                    map.setTileSource(tileSource(c.isLight))
                    lastIsLight = c.isLight
                }
                plannedLine.outlinePaint.color = c.dim.toArgb()
                if (planned.isNotEmpty()) {
                    plannedLine.setPoints(planned)
                }
                polyline.outlinePaint.color = c.route.toArgb()
                if (route.isNotEmpty()) {
                    polyline.setPoints(route)
                }
                positionDot.setColor(c.accent.toArgb())
                if (follow && !frozen) {
                    if (aheadWindow.isNotEmpty() && current != null) {
                        positionMarker.position = current
                        positionMarker.isEnabled = true
                        wasFitAhead = true
                        map.mapOrientation = targetOrientation
                        map.post {
                            runCatching {
                                map.zoomToBoundingBox(
                                    BoundingBox.fromGeoPoints(aheadWindow + current), false, CAMERA_PADDING_PX,
                                )
                                val z = map.zoomLevelDouble
                                if (z > MAX_ZOOM) map.controller.setZoom(MAX_ZOOM)
                                else if (z < MIN_ZOOM) map.controller.setZoom(MIN_ZOOM)
                            }
                        }
                    } else {
                        positionMarker.isEnabled = false
                        // Leaving fit-ahead (toggle off or off-route): restore the fixed
                        // follow zoom once, then leave zoom alone so pinch-zoom persists.
                        if (wasFitAhead) {
                            map.controller.setZoom(FOLLOW_ZOOM)
                            wasFitAhead = false
                        }
                        map.mapOrientation = targetOrientation
                        current?.let { map.controller.setCenter(it) }
                    }
                } else if (follow && frozen) {
                    // Frozen: leave the camera and orientation exactly where the rider
                    // left them. Keep the rider visible via the map-anchored marker.
                    positionMarker.isEnabled = current != null
                    current?.let { positionMarker.position = it }
                } else if (route.size == 1) {
                    map.controller.setZoom(15.0)
                    map.controller.setCenter(route.first())
                } else if (route.size > 1) {
                    // Fit the whole track once the view is laid out.
                    map.post {
                        runCatching {
                            map.zoomToBoundingBox(BoundingBox.fromGeoPoints(route), false, 64)
                        }
                    }
                }
                map.invalidate()
            },
        )

        if (follow && current != null && aheadWindow.isEmpty() && !frozen) {
            Box(
                Modifier.size(16.dp).clip(CircleShape).background(c.accent),
            )
        } else if (follow && current == null) {
            Text(
                "ACQUIRING GPS…",
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(c.panel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = c.dim,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
        } else if (!follow && route.isEmpty()) {
            Text(
                "NO GPS DATA",
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(c.panel)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = c.dim,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}
