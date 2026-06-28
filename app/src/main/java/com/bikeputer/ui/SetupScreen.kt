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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.bikeputer.ble.ScannedDevice
import com.bikeputer.ui.theme.Barlow
import com.bikeputer.ui.theme.HeartRateColor
import com.bikeputer.ui.theme.JetBrainsMono
import com.bikeputer.ui.theme.LocalBikeColors

@Composable
fun SetupScreen(
    vm: SetupViewModel,
    onStartRide: () -> Unit,
    onDemo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoutes: () -> Unit,
) {
    val c = LocalBikeColors.current
    val settings by vm.riderSettings.collectAsState()
    val powerDevices by vm.powerScanner.devices.collectAsState()
    val powerScanning by vm.powerScanner.scanning.collectAsState()
    val hrDevices by vm.hrScanner.devices.collectAsState()
    val hrScanning by vm.hrScanner.scanning.collectAsState()

    val ready = listOf(settings.powerSensorMac != null, settings.hrSensorMac != null).count { it }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // wordmark + settings
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).rotate(45f).clip(RoundedCornerShape(2.dp)).background(c.accent))
                Text(
                    "BIKEPUTER",
                    modifier = Modifier.padding(start = 10.dp),
                    color = c.text, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, letterSpacing = 4.sp,
                )
            }
            Box(
                Modifier
                    .size(40.dp).clip(CircleShape).background(c.panel)
                    .border(1.dp, c.border, CircleShape).clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) { Text("⚙", color = c.dim, fontSize = 18.sp) }
        }

        Text(
            "Ready to ride",
            modifier = Modifier.padding(top = 14.dp),
            color = c.text, fontFamily = Barlow, fontWeight = FontWeight.ExtraBold,
            fontSize = 38.sp, letterSpacing = (-1).sp,
        )

        // recorder banner
        Row(
            Modifier
                .fillMaxWidth().padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp)).background(c.panel)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(c.bg)
                    .border(1.dp, c.border2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("⌚", fontSize = 18.sp) }
            Column(Modifier.padding(start = 14.dp)) {
                Text("● RECORDS THE RIDE", color = c.accent, fontFamily = JetBrainsMono, fontSize = 10.sp, letterSpacing = 2.sp)
                Text("GPS watch / head unit", color = c.text, fontFamily = Barlow, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text("FIT activity — phone mirrors live, doesn't record", color = c.dim, fontFamily = JetBrainsMono, fontSize = 10.sp)
            }
        }

        // sensors header
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SENSORS", color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 2.sp)
            Text("$ready OF 2 PAIRED", color = if (ready > 0) c.accent else c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 2.sp)
        }

        SensorCard(
            tag = "PW", tagColor = c.accent,
            title = "Power meter", subtitle = "Cycling power meter",
            selectedMac = settings.powerSensorMac,
            scanning = powerScanning, devices = powerDevices,
            onToggleScan = { if (powerScanning) vm.stopPowerScan() else vm.startPowerScan() },
            onSelect = { vm.selectPower(it); vm.stopPowerScan() },
            onForget = { vm.selectPower(null) },
        )
        Spacer(Modifier.height(9.dp))
        SensorCard(
            tag = "HR", tagColor = HeartRateColor,
            title = "Heart rate", subtitle = "Chest strap / HR monitor",
            selectedMac = settings.hrSensorMac,
            scanning = hrScanning, devices = hrDevices,
            onToggleScan = { if (hrScanning) vm.stopHrScan() else vm.startHrScan() },
            onSelect = { vm.selectHr(it); vm.stopHrScan() },
            onForget = { vm.selectHr(null) },
        )
        Spacer(Modifier.height(9.dp))

        // GPS (informational)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.panel)
                .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.bg)
                    .border(1.dp, c.border2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("GP", color = Color(0xFF22C55E), fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("GPS", color = c.text, fontFamily = JetBrainsMono, fontSize = 15.sp)
                Text("Phone GNSS · speed & route", color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp)
            }
            Text("ON START", color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 1.sp)
        }

        // start
        Box(
            Modifier
                .fillMaxWidth().padding(top = 20.dp).height(60.dp)
                .clip(RoundedCornerShape(16.dp)).background(c.accent)
                .clickable(onClick = onStartRide),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "START RIDE  ▸",
                color = Color(0xFF04201D), fontFamily = Barlow, fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp, letterSpacing = 1.sp,
            )
        }

        Box(
            Modifier
                .fillMaxWidth().padding(top = 10.dp).height(48.dp)
                .clip(RoundedCornerShape(14.dp)).border(1.dp, c.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onDemo),
            contentAlignment = Alignment.Center,
        ) {
            Text("Demo data", color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
        Box(
            Modifier
                .fillMaxWidth().padding(top = 10.dp, bottom = 24.dp).height(48.dp)
                .clip(RoundedCornerShape(14.dp)).border(1.dp, c.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenRoutes),
            contentAlignment = Alignment.Center,
        ) {
            Text("Routes", color = c.text2, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SensorCard(
    tag: String, tagColor: Color,
    title: String, subtitle: String,
    selectedMac: String?,
    scanning: Boolean,
    devices: List<ScannedDevice>,
    onToggleScan: () -> Unit,
    onSelect: (String) -> Unit,
    onForget: () -> Unit,
) {
    val c = LocalBikeColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.panel)
            .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(c.bg)
                    .border(1.dp, c.border2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(tag, color = tagColor, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = c.text, fontFamily = JetBrainsMono, fontSize = 15.sp)
                Text(
                    selectedMac ?: subtitle,
                    color = if (selectedMac != null) c.accent else c.dim,
                    fontFamily = JetBrainsMono, fontSize = 11.sp,
                )
            }
            if (selectedMac != null) {
                SignalBars(tagColor)
                Spacer(Modifier.width(10.dp))
                PillButton("FORGET", onForget)
            } else {
                PillButton(if (scanning) "STOP" else "SCAN", onToggleScan)
            }
        }

        if (scanning) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Text(
                if (devices.isEmpty()) "Scanning…" else "Tap a device to pair",
                modifier = Modifier.padding(top = 10.dp),
                color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp,
            )
            // Plain column (no inner scroll) — this card lives in a scrollable page.
            devices.take(8).forEach { d ->
                Column(
                    Modifier.fillMaxWidth().clickable { onSelect(d.address) }.padding(vertical = 7.dp),
                ) {
                    Text(d.name ?: "(unnamed)", color = c.text, fontFamily = JetBrainsMono, fontSize = 14.sp)
                    Text(d.address, color = c.dim, fontFamily = JetBrainsMono, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val c = LocalBikeColors.current
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(c.accentSoft)
            .border(1.dp, c.border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = c.accent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun SignalBars(color: Color) {
    val dim = LocalBikeColors.current.border2
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(5, 8, 12, 16).forEachIndexed { i, h ->
            Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(if (i < 3) color else dim))
        }
    }
}
