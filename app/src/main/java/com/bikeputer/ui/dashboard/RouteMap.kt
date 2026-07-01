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

private const val CAMERA_PADDING_PX = 80

/**
 * In heading-up fixed-follow, the rider is biased this fraction of the map height
 * below centre so more of the road ahead is visible. Positive shifts the puck down;
 * flip the sign here if a device renders the offset the other way.
 */
private const val HEADING_UP_PUCK_OFFSET_FRACTION = 0.22f

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
    defaultZoom: Int = 16,
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
            controller.setZoom(defaultZoom.toDouble())
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
                        map.setMapCenterOffset(0, 0)
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
                                val maxZ = MapZoom.adaptiveMax(defaultZoom)
                                val minZ = MapZoom.adaptiveMin(defaultZoom)
                                if (z > maxZ) map.controller.setZoom(maxZ)
                                else if (z < minZ) map.controller.setZoom(minZ)
                            }
                        }
                    } else {
                        // Leaving fit-ahead (toggle off or off-route): restore the fixed
                        // follow zoom once, then leave zoom alone so pinch-zoom persists.
                        if (wasFitAhead) {
                            map.controller.setZoom(defaultZoom.toDouble())
                            wasFitAhead = false
                        }
                        map.mapOrientation = targetOrientation
                        if (headingUp) {
                            // Heading-up follow: bias the rider toward the bottom so more of
                            // the road ahead is visible. Use the map-anchored marker (it tracks
                            // the offset projection); the screen-centred Compose dot is only
                            // used in north-up follow.
                            map.setMapCenterOffset(0, (map.height * HEADING_UP_PUCK_OFFSET_FRACTION).toInt())
                            positionMarker.isEnabled = current != null
                            current?.let { positionMarker.position = it }
                        } else {
                            map.setMapCenterOffset(0, 0)
                            positionMarker.isEnabled = false
                        }
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

        if (follow && current != null && aheadWindow.isEmpty() && !frozen && !headingUp) {
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

/** Centre of the fixed sample route shown in the Settings zoom preview. */
private val SAMPLE_ROUTE_CENTER = GeoPoint(52.2297, 21.0122)
private val SAMPLE_ROUTE = listOf(
    GeoPoint(52.2289, 21.0096),
    GeoPoint(52.2294, 21.0110),
    GeoPoint(52.2297, 21.0122),
    GeoPoint(52.2301, 21.0135),
    GeoPoint(52.2307, 21.0146),
)

/**
 * A small, non-interactive map rendering a fixed [SAMPLE_ROUTE] at [zoom]. Used in
 * Settings to preview the chosen default map zoom; theme-aware via [tileSource].
 * Gestures and zoom buttons are disabled — it is a preview, not a control.
 */
@Composable
fun PreviewMap(zoom: Int, modifier: Modifier = Modifier) {
    val c = LocalBikeColors.current
    val ctx = LocalContext.current

    val sampleLine = remember {
        Polyline().apply {
            outlinePaint.color = c.route.toArgb()
            outlinePaint.strokeWidth = 10f
            setPoints(SAMPLE_ROUTE)
        }
    }
    val dot = remember {
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(c.accent.toArgb())
            setSize(34, 34)
        }
    }
    val mapView = remember {
        Configuration.getInstance().userAgentValue = ctx.packageName
        MapView(ctx).apply {
            setTileSource(tileSource(c.isLight))
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setOnTouchListener { _, _ -> true } // preview only — swallow gestures
            overlays.add(sampleLine)
            Marker(this).apply {
                icon = dot
                position = SAMPLE_ROUTE_CENTER
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
                setOnMarkerClickListener { _, _ -> true }
            }.also { overlays.add(it) }
            controller.setZoom(zoom.toDouble())
            controller.setCenter(SAMPLE_ROUTE_CENTER)
        }
    }

    var lastIsLight by remember { mutableStateOf(c.isLight) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            if (c.isLight != lastIsLight) {
                map.setTileSource(tileSource(c.isLight))
                lastIsLight = c.isLight
            }
            sampleLine.outlinePaint.color = c.route.toArgb()
            dot.setColor(c.accent.toArgb())
            map.controller.setZoom(zoom.toDouble())
            map.controller.setCenter(SAMPLE_ROUTE_CENTER)
            map.invalidate()
        },
    )
}
