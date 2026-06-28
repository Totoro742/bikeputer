package com.bikeputer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rider_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val imperial = booleanPreferencesKey("imperial")
        val ftp = intPreferencesKey("ftp")
        val maxHr = intPreferencesKey("max_hr")
        val powerMac = stringPreferencesKey("power_mac")
        val hrMac = stringPreferencesKey("hr_mac")
        val layout = stringPreferencesKey("layout")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val activeRouteId = stringPreferencesKey("active_route_id")
        val offRouteThresholdM = intPreferencesKey("off_route_threshold_m")
        val fitAheadCamera = booleanPreferencesKey("fit_ahead_camera")
        val speedAdaptiveLookAhead = booleanPreferencesKey("speed_adaptive_look_ahead")
        val headingUpMap = booleanPreferencesKey("heading_up_map")
        val defaultMapZoom = intPreferencesKey("default_map_zoom")
        val navMode = stringPreferencesKey("nav_mode")
        val orsApiKey = stringPreferencesKey("ors_api_key")
    }

    val settings: Flow<RiderSettings> = context.dataStore.data.map { p ->
        val d = RiderSettings()
        RiderSettings(
            useImperial = p[Keys.imperial] ?: d.useImperial,
            ftp = p[Keys.ftp] ?: d.ftp,
            maxHr = p[Keys.maxHr] ?: d.maxHr,
            powerSensorMac = p[Keys.powerMac],
            hrSensorMac = p[Keys.hrMac],
            layout = p[Keys.layout]?.let { runCatching { DashboardLayout.valueOf(it) }.getOrNull() }
                ?: d.layout,
            darkTheme = p[Keys.darkTheme] ?: d.darkTheme,
            activeRouteId = p[Keys.activeRouteId],
            offRouteThresholdM = p[Keys.offRouteThresholdM] ?: d.offRouteThresholdM,
            fitAheadCamera = p[Keys.fitAheadCamera] ?: d.fitAheadCamera,
            speedAdaptiveLookAhead = p[Keys.speedAdaptiveLookAhead] ?: d.speedAdaptiveLookAhead,
            headingUpMap = p[Keys.headingUpMap] ?: d.headingUpMap,
            defaultMapZoom = p[Keys.defaultMapZoom] ?: d.defaultMapZoom,
            navMode = p[Keys.navMode]?.let { runCatching { NavMode.valueOf(it) }.getOrNull() } ?: d.navMode,
            orsApiKey = p[Keys.orsApiKey] ?: d.orsApiKey,
        )
    }

    suspend fun update(transform: (RiderSettings) -> RiderSettings) {
        val current = settings.first()
        val next = transform(current)
        context.dataStore.edit { p ->
            p[Keys.imperial] = next.useImperial
            p[Keys.ftp] = next.ftp
            p[Keys.maxHr] = next.maxHr
            val pm = next.powerSensorMac
            if (pm != null) p[Keys.powerMac] = pm else p.remove(Keys.powerMac)
            val hm = next.hrSensorMac
            if (hm != null) p[Keys.hrMac] = hm else p.remove(Keys.hrMac)
            p[Keys.layout] = next.layout.name
            p[Keys.darkTheme] = next.darkTheme
            val ar = next.activeRouteId
            if (ar != null) p[Keys.activeRouteId] = ar else p.remove(Keys.activeRouteId)
            p[Keys.offRouteThresholdM] = next.offRouteThresholdM
            p[Keys.fitAheadCamera] = next.fitAheadCamera
            p[Keys.speedAdaptiveLookAhead] = next.speedAdaptiveLookAhead
            p[Keys.headingUpMap] = next.headingUpMap
            p[Keys.defaultMapZoom] = next.defaultMapZoom
            p[Keys.navMode] = next.navMode.name
            p[Keys.orsApiKey] = next.orsApiKey
        }
    }
}
