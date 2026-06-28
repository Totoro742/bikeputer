package com.bikeputer.nav.online

import com.bikeputer.data.NavMode

/**
 * Whether navigation should attempt an online routing fetch. Requires a key; in Auto
 * it also requires connectivity. Force-Online attempts regardless of reported
 * connectivity (the fetch falls back to offline on failure). Offline never attempts.
 */
fun shouldAttemptOnline(pref: NavMode, networkAvailable: Boolean, hasKey: Boolean): Boolean =
    hasKey && when (pref) {
        NavMode.Online -> true
        NavMode.Auto -> networkAvailable
        NavMode.Offline -> false
    }
