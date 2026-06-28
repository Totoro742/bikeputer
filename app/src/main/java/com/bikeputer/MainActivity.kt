package com.bikeputer

import android.Manifest
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bikeputer.ble.BleScanner
import com.bikeputer.nav.Maneuver
import com.bikeputer.nav.online.NavStatus
import com.bikeputer.nav.online.OpenRouteServiceClient
import com.bikeputer.nav.online.RoutingClient
import com.bikeputer.nav.online.onlineManeuvers
import com.bikeputer.nav.online.shouldAttemptOnline
import com.bikeputer.nav.online.simplifyRoute
import com.bikeputer.data.RiderSettings
import com.bikeputer.data.RouteStore
import com.bikeputer.data.SettingsRepository
import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.RidePhase
import com.bikeputer.service.RideService
import com.bikeputer.ui.RideViewModel
import com.bikeputer.ui.RoutesScreen
import com.bikeputer.ui.RoutesViewModel
import com.bikeputer.ui.SettingsScreen
import com.bikeputer.ui.SettingsViewModel
import com.bikeputer.ui.SetupScreen
import com.bikeputer.ui.SetupViewModel
import com.bikeputer.ui.dashboard.RideDashboard
import com.bikeputer.ui.dashboard.RideSummaryScreen
import com.bikeputer.ui.theme.BikeputerTheme
import com.bikeputer.ui.theme.LocalBikeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private enum class Screen { Setup, Settings, Ride, Summary, Routes }

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: SettingsRepository

    private val powerScanner by lazy { BleScanner(applicationContext, BleScanner.CYCLING_POWER_SERVICE) }
    private val hrScanner by lazy { BleScanner(applicationContext, BleScanner.HEART_RATE_SERVICE) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepo = SettingsRepository(applicationContext)
        requestPermissions()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = RiderSettings())
            BikeputerTheme(dark = settings.darkTheme) {
                Box(Modifier.fillMaxSize().background(LocalBikeColors.current.bg)) {
                    var screen by remember { mutableStateOf(Screen.Setup) }
                    var plannedRoute by remember { mutableStateOf<List<GeoPos>>(emptyList()) }
                    LaunchedEffect(settings.activeRouteId) {
                        plannedRoute = loadPlanned(settings.activeRouteId)
                    }
                    var onlineManeuvers by remember { mutableStateOf<List<Maneuver>?>(null) }
                    var navStatus by remember { mutableStateOf(NavStatus.Offline) }
                    var rerouteClient by remember { mutableStateOf<RoutingClient?>(null) }
                    val networkAvailable = remember {
                        val cm = getSystemService(ConnectivityManager::class.java)
                        cm?.activeNetwork != null
                    }
                    LaunchedEffect(plannedRoute, settings.navMode, settings.orsApiKey, networkAvailable) {
                        val attempt = shouldAttemptOnline(
                            settings.navMode, networkAvailable, settings.orsApiKey.isNotBlank(),
                        )
                        rerouteClient =
                            if (attempt && plannedRoute.size >= 2) OpenRouteServiceClient(settings.orsApiKey) else null
                        if (!attempt || plannedRoute.size < 2) {
                            onlineManeuvers = null
                            navStatus = NavStatus.Offline
                        } else {
                            navStatus = try {
                                val client = OpenRouteServiceClient(settings.orsApiKey)
                                val dirs = client.directions(simplifyRoute(plannedRoute))
                                val m = onlineManeuvers(dirs, plannedRoute)
                                onlineManeuvers = m
                                NavStatus.Online
                            } catch (e: Exception) {
                                android.util.Log.w("Bikeputer/Nav", "ORS directions failed", e)
                                onlineManeuvers = null
                                NavStatus.NoSignal
                            }
                        }
                    }
                    when (screen) {
                        Screen.Setup -> {
                            val setupVm: SetupViewModel = viewModel(factory = setupFactory())
                            SetupScreen(
                                vm = setupVm,
                                onStartRide = {
                                    startForegroundService(
                                        Intent(this@MainActivity, RideService::class.java)
                                            .apply { action = RideService.ACTION_START }
                                    )
                                    screen = Screen.Ride
                                },
                                onOpenSettings = { screen = Screen.Settings },
                                onOpenRoutes = { screen = Screen.Routes },
                            )
                        }
                        Screen.Settings -> {
                            val settingsVm: SettingsViewModel = viewModel(factory = settingsFactory())
                            SettingsScreen(vm = settingsVm, onBack = { screen = Screen.Setup })
                        }
                        Screen.Ride, Screen.Summary -> {
                            val factory = remember { realRideFactory() }
                            val vm: RideViewModel = viewModel(key = "ride", factory = factory)
                            if (screen == Screen.Ride) {
                                RideHost(
                                    vm, settings, plannedRoute,
                                    onlineManeuvers = onlineManeuvers,
                                    navStatus = navStatus,
                                    rerouteClient = rerouteClient,
                                    onToggleFitAhead = {
                                        lifecycleScope.launch {
                                            settingsRepo.update { it.copy(fitAheadCamera = !it.fitAheadCamera) }
                                        }
                                    },
                                    onToggleHeadingUp = {
                                        lifecycleScope.launch {
                                            settingsRepo.update { it.copy(headingUpMap = !it.headingUpMap) }
                                        }
                                    },
                                    onFinish = { screen = Screen.Summary },
                                )
                            } else {
                                val state by vm.state.collectAsState()
                                val route by vm.route.collectAsState()
                                RideSummaryScreen(
                                    state = state,
                                    route = route,
                                    imperial = settings.useImperial,
                                    onDone = {
                                        startService(
                                            Intent(this@MainActivity, RideService::class.java)
                                                .apply { action = RideService.ACTION_STOP }
                                        )
                                        screen = Screen.Setup
                                    },
                                    planned = plannedRoute,
                                    offRouteThresholdM = settings.offRouteThresholdM,
                                )
                            }
                        }
                        Screen.Routes -> {
                            val routesVm: RoutesViewModel = viewModel(factory = routesFactory())
                            RoutesScreen(vm = routesVm, onBack = { screen = Screen.Setup })
                        }
                    }
                }
            }
        }
    }

    private fun setupFactory() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SetupViewModel(settingsRepo, powerScanner, hrScanner) as T
    }

    private fun settingsFactory() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsRepo) as T
    }

    private fun realRideFactory() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RideViewModel(RideService.activeSession) as T
    }

    private fun routesFactory() = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RoutesViewModel(RouteStore(filesDir), settingsRepo) as T
    }

    private suspend fun loadPlanned(id: String?): List<com.bikeputer.domain.GeoPos> =
        if (id == null) emptyList()
        else withContext(Dispatchers.IO) { RouteStore(filesDir).loadPoints(id) }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
}

@Composable
private fun RideHost(
    vm: RideViewModel,
    settings: RiderSettings,
    planned: List<GeoPos>,
    onlineManeuvers: List<Maneuver>? = null,
    navStatus: NavStatus = NavStatus.Offline,
    rerouteClient: RoutingClient? = null,
    onToggleFitAhead: () -> Unit = {},
    onToggleHeadingUp: () -> Unit = {},
    onFinish: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val phase by vm.phase.collectAsState()
    val route by vm.route.collectAsState()
    var showEndDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.start() }

    RideDashboard(
        state = state,
        layout = settings.layout,
        imperial = settings.useImperial,
        ftp = settings.ftp,
        paused = phase == RidePhase.Paused,
        route = route,
        planned = planned,
        onlineManeuvers = onlineManeuvers,
        navStatus = navStatus,
        rerouteClient = rerouteClient,
        offRouteThresholdM = settings.offRouteThresholdM,
        fitAheadCamera = settings.fitAheadCamera,
        speedAdaptiveLookAhead = settings.speedAdaptiveLookAhead,
        onToggleFitAhead = onToggleFitAhead,
        headingUp = settings.headingUpMap,
        onToggleHeadingUp = onToggleHeadingUp,
        onBack = { showEndDialog = true },
        onTogglePause = {
            if (phase == RidePhase.Paused) vm.resume() else vm.pause()
        },
    )

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End this ride?") },
            text = { Text("Your ride will end and you'll see a summary.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    vm.finish()
                    onFinish()
                }) { Text("End ride") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Keep riding") }
            },
        )
    }
}
