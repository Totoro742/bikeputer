# Bikeputer

An Android cycling computer built with Kotlin and Jetpack Compose. It reads
Bluetooth LE power and heart-rate sensors plus GPS, computes live ride metrics,
and renders them on swipeable dashboards with turn-by-turn route navigation.

## Features

- **Live metrics** — speed, distance, instantaneous & normalized power, heart
  rate, cadence, elevation gain, and power/HR training zones.
- **Three swipeable dashboards**, including a map/navigation view.
- **Route navigation** from imported GPX routes, with two interchangeable
  engines: a built-in offline geometry engine, or online routing via
  OpenRouteService for real street names and maneuvers.
- **Background recording** through a foreground service, so a ride keeps running
  while the app is backgrounded.
- Light/dark themes and metric/imperial units.

## Architecture

The project is deliberately layered so that all ride, metric, and navigation
logic is **pure, JVM-testable Kotlin**, with Android specifics (BLE, GPS, the
foreground service, and Compose UI) confined to the edges. Sensor and location
inputs sit behind small source interfaces, which lets the same pipeline run from
real hardware, demo mode, or unit tests.

- `metrics/` — stateful metrics aggregation (distance, normalized power, zones…).
- `nav/` — offline turn-by-turn geometry engine.
- `nav/online/` — online routing client (OpenRouteService) and rerouting.
- `ride/`, `domain/` — the ride session pipeline and immutable ride state.
- `ui/` — Jetpack Compose screens and dashboards.

## Build & test

Requires JDK 17. Uses the Gradle wrapper.

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run the JVM unit-test suite
./gradlew lint                 # Android lint
```

Targets `minSdk 26` / `targetSdk 34`.

## Online navigation

Online routing is optional. To enable it, provide an
[OpenRouteService](https://openrouteservice.org/) API key in the app's settings;
without a key (or without connectivity) the app falls back to the offline
navigation engine.

## License

No license is currently specified.
