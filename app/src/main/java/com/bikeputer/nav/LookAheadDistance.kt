package com.bikeputer.nav

/** Default fixed look-ahead distance (metres). */
const val DEFAULT_LOOK_AHEAD_M = 400.0
/** Seconds of travel framed ahead in speed-adaptive mode. */
const val LOOK_AHEAD_HORIZON_S = 20.0
/** Adaptive look-ahead floor / ceiling (metres). */
const val MIN_LOOK_AHEAD_M = 150.0
const val MAX_LOOK_AHEAD_M = 800.0

/**
 * How far ahead of the rider the camera should frame the route. Fixed by default;
 * speed-adaptive (≈ the next [LOOK_AHEAD_HORIZON_S] seconds at the current speed,
 * clamped) when [adaptive] is true. Null/zero speed clamps to [MIN_LOOK_AHEAD_M].
 */
fun lookAheadMeters(speedKmh: Double?, adaptive: Boolean): Double =
    if (!adaptive) DEFAULT_LOOK_AHEAD_M
    else ((speedKmh ?: 0.0) / 3.6 * LOOK_AHEAD_HORIZON_S).coerceIn(MIN_LOOK_AHEAD_M, MAX_LOOK_AHEAD_M)
