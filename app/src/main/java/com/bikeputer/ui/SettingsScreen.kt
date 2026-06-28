package com.bikeputer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.bikeputer.data.DashboardLayout
import com.bikeputer.data.NavMode
import com.bikeputer.ui.dashboard.PreviewMap
import com.bikeputer.ui.theme.Barlow
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors
import com.bikeputer.ui.theme.ZoneRow
import com.bikeputer.ui.theme.hrZoneTable
import com.bikeputer.ui.theme.powerZoneTable

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val c = LocalBikeColors.current
    val s by vm.riderSettings.collectAsState()

    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // app bar
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.panel)
                    .border(1.dp, c.border, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("‹", color = c.text2, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Text(
                "Settings",
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                color = c.text, fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
            )
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(c.accentSoft)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text("SAVED", color = c.accent, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 1.sp) }
        }

        SectionLabel("DASHBOARD LAYOUT")
        LayoutRow("A", "Glance", "Huge numbers, minimal", s.layout == DashboardLayout.A) { vm.setLayout(DashboardLayout.A) }
        Spacer(Modifier.height(9.dp))
        LayoutRow("B", "Instrument", "Dense head-unit grid", s.layout == DashboardLayout.B) { vm.setLayout(DashboardLayout.B) }
        Spacer(Modifier.height(9.dp))
        LayoutRow("C", "Ride", "Map-forward & bold", s.layout == DashboardLayout.C) { vm.setLayout(DashboardLayout.C) }

        SectionLabel("APPEARANCE")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ChoiceCard("Dark", "OLED · night riding", s.darkTheme, Modifier.weight(1f)) { vm.setDark(true) }
            ChoiceCard("Light", "Daylight · direct sun", !s.darkTheme, Modifier.weight(1f)) { vm.setDark(false) }
        }

        SectionLabel("UNITS")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ChoiceCard("Metric", "km · km/h · m", !s.useImperial, Modifier.weight(1f)) { vm.setImperial(false) }
            ChoiceCard("Imperial", "mi · mph · ft", s.useImperial, Modifier.weight(1f)) { vm.setImperial(true) }
        }

        SectionLabel("RIDER PROFILE")
        Stepper("FTP", "Functional threshold power", "SETS POWER ZONES", Color(0xFFFACC15),
            s.ftp, "W", { vm.adjustFtp(-5) }, { vm.adjustFtp(5) })
        Spacer(Modifier.height(12.dp))
        Stepper("Maximum heart rate", "Measured or 220 − age", "SETS HR ZONES", Color(0xFFEF4444),
            s.maxHr, "bpm", { vm.adjustMaxHr(-1) }, { vm.adjustMaxHr(1) })

        SectionLabel("POWER ZONES · WATTS")
        ZoneTable(powerZoneTable(s.ftp))

        SectionLabel("HEART-RATE ZONES · BPM")
        ZoneTable(hrZoneTable(s.maxHr))

        SectionLabel("ADVANCED")
        Stepper("Off-route alert", "Distance before warning", "ROUTE NAVIGATION", c.accent,
            s.offRouteThresholdM, "m", { vm.adjustOffRouteThreshold(-5) }, { vm.adjustOffRouteThreshold(5) })
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ChoiceCard("Fixed", "Look-ahead · 400 m", !s.speedAdaptiveLookAhead, Modifier.weight(1f)) {
                vm.setSpeedAdaptiveLookAhead(false)
            }
            ChoiceCard("Adaptive", "Look-ahead · by speed", s.speedAdaptiveLookAhead, Modifier.weight(1f)) {
                vm.setSpeedAdaptiveLookAhead(true)
            }
        }

        SectionLabel("MAP ZOOM")
        ZoomPreviewStepper(
            zoom = s.defaultMapZoom,
            onMinus = { vm.adjustDefaultMapZoom(-1) },
            onPlus = { vm.adjustDefaultMapZoom(1) },
        )

        SectionLabel("NAVIGATION MODE")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ChoiceCard("Auto", "Online when reachable", s.navMode == NavMode.Auto, Modifier.weight(1f)) {
                vm.setNavMode(NavMode.Auto)
            }
            ChoiceCard("Online", "Engine + street names", s.navMode == NavMode.Online, Modifier.weight(1f)) {
                vm.setNavMode(NavMode.Online)
            }
            ChoiceCard("Offline", "Geometry turns only", s.navMode == NavMode.Offline, Modifier.weight(1f)) {
                vm.setNavMode(NavMode.Offline)
            }
        }
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(
            value = s.orsApiKey,
            onValueChange = { vm.setOrsApiKey(it) },
            label = { Text("OpenRouteService API key") },
            placeholder = { Text("needed for online navigation") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 22.dp, bottom = 12.dp),
        color = LocalBikeColors.current.dim, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 2.sp,
    )
}

@Composable
private fun LayoutRow(tag: String, title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalBikeColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accentSoft else c.panel)
            .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.bg)
                .border(1.dp, c.border2, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(tag, color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = c.text, fontFamily = JetBrainsMono, fontSize = 15.sp)
            Text(sub, color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp)
        }
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .border(2.dp, if (selected) c.accent else c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent))
        }
    }
}

@Composable
private fun ChoiceCard(title: String, sub: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalBikeColors.current
    Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accentSoft else c.panel)
            .border(1.dp, if (selected) c.accent else c.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(15.dp),
    ) {
        Text(title, color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(sub, color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun Stepper(
    title: String, sub: String, hint: String, hintColor: Color,
    value: Int, unit: String, onMinus: () -> Unit, onPlus: () -> Unit,
) {
    val c = LocalBikeColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.panel)
            .border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = c.text, fontFamily = JetBrainsMono, fontSize = 15.sp)
                Text(sub, color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
            Text(hint, color = hintColor, fontFamily = JetBrainsMono, fontSize = 10.sp, letterSpacing = 1.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBtn("−", onMinus)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value.toString(), color = c.text, fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 60.sp)
                Text(unit, color = c.dim, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(start = 5.dp, bottom = 8.dp))
            }
            StepBtn("+", onPlus)
        }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    val c = LocalBikeColors.current
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.line)
            .border(1.dp, c.border2, RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, fontSize = 30.sp) }
}

@Composable
private fun ZoomPreviewStepper(zoom: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = LocalBikeColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.panel)
            .border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(18.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))) {
            PreviewMap(zoom = zoom, modifier = Modifier.fillMaxSize())
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBtn("−", onMinus)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(zoom.toString(), color = c.text, fontFamily = Barlow, fontWeight = FontWeight.ExtraBold, fontSize = 60.sp)
                Text("zoom", color = c.dim, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(start = 5.dp, bottom = 8.dp))
            }
            StepBtn("+", onPlus)
        }
    }
}

@Composable
private fun ZoneTable(rows: List<ZoneRow>) {
    val c = LocalBikeColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.panel)
            .border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
    ) {
        rows.forEachIndexed { i, z ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(z.color))
                Text(z.tag, color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp).width(28.dp))
                Text(z.name, color = Color(0xFF9AA3AC), fontFamily = JetBrainsMono, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(z.range, color = c.text, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
            if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }
    }
}
