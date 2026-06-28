package com.bikeputer.data

/** Which dashboard layout the ride screen renders. */
enum class DashboardLayout { A, B, C }

/** How navigation chooses between online (engine instructions) and offline (geometry) turns. */
enum class NavMode { Auto, Online, Offline }

data class RiderSettings(
    val useImperial: Boolean = false,
    val ftp: Int = 200,
    val maxHr: Int = 190,
    val powerSensorMac: String? = null,
    val hrSensorMac: String? = null,
    val layout: DashboardLayout = DashboardLayout.C,
    val darkTheme: Boolean = true,
    val activeRouteId: String? = null,
    val offRouteThresholdM: Int = 35,
    val fitAheadCamera: Boolean = true,
    val speedAdaptiveLookAhead: Boolean = false,
    val headingUpMap: Boolean = true,
    val navMode: NavMode = NavMode.Auto,
    val orsApiKey: String = "",
)
