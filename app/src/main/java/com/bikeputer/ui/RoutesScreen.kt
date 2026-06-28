package com.bikeputer.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.SavedRoute
import com.bikeputer.ui.dashboard.RouteMap
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import org.osmdroid.util.GeoPoint

@Composable
fun RoutesScreen(vm: RoutesViewModel, onBack: () -> Unit) {
    val c = LocalBikeColors.current
    val ctx = LocalContext.current
    val routes by vm.routes.collectAsState()
    val activeId by vm.activeRouteId.collectAsState()
    val error by vm.error.collectAsState()
    var preview by remember { mutableStateOf<SavedRoute?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(ctx, uri) ?: "route.gpx"
            ctx.contentResolver.openInputStream(uri)?.let { vm.import(it, name) }
        }
    }

    val selected = preview
    if (selected != null) {
        RoutePreview(
            route = selected,
            loadPoints = { vm.loadPoints(selected.id) },
            onUse = { vm.arm(selected.id); onBack() },
            onBack = { preview = null },
        )
        return
    }

    Column(Modifier.fillMaxSize().background(c.bg).padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("‹ BACK", modifier = Modifier.clickable(onClick = onBack),
                color = c.dim, fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 2.sp)
            Text("ROUTES", color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, letterSpacing = 3.sp)
            if (activeId != null) {
                Text("CLEAR", modifier = Modifier.clickable { vm.clear() },
                    color = c.accent, fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 2.sp)
            } else {
                Text("", fontSize = 12.sp)
            }
        }

        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accent)
                .clickable { picker.launch(arrayOf("*/*")) }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("IMPORT .GPX", color = c.bg, fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
        }

        error?.let { msg ->
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(10.dp))
                    .background(c.panel).clickable { vm.clearError() }.padding(12.dp),
            ) {
                Text(msg, color = c.dim, fontFamily = JetBrainsMono, fontSize = 12.sp)
            }
        }

        if (routes.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No saved routes yet", color = c.dim, fontFamily = JetBrainsMono, fontSize = 13.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 14.dp)) {
                items(routes, key = { it.id }) { route ->
                    RouteRow(
                        route = route,
                        armed = route.id == activeId,
                        onClick = { preview = route },
                        onDelete = { vm.delete(route.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteRow(route: SavedRoute, armed: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val c = LocalBikeColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(12.dp))
            .background(c.panel).border(1.dp, if (armed) c.accent else c.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(route.name, color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                if (armed) "ARMED · ${route.pointCount} pts" else "${route.pointCount} pts",
                color = if (armed) c.accent else c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp,
            )
        }
        Text("✕", modifier = Modifier.clickable(onClick = onDelete).padding(start = 12.dp),
            color = c.dim, fontSize = 16.sp)
    }
}

@Composable
private fun RoutePreview(
    route: SavedRoute,
    loadPoints: suspend () -> List<GeoPos>,
    onUse: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalBikeColors.current
    var points by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    LaunchedEffect(route.id) { points = loadPoints().map { GeoPoint(it.lat, it.lng) } }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("‹ BACK", modifier = Modifier.clickable(onClick = onBack),
                color = c.dim, fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 2.sp)
            Text(route.name, color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("", fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            RouteMap(route = points, current = null, modifier = Modifier.fillMaxSize(), follow = false)
        }
        Box(
            Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(14.dp)).background(c.accent)
                .clickable(onClick = onUse).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("USE THIS ROUTE", color = c.bg, fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp)
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    }
