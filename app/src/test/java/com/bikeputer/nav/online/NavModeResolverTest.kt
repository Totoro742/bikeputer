package com.bikeputer.nav.online

import com.bikeputer.data.NavMode
import org.junit.Assert.assertEquals
import org.junit.Test

class NavModeResolverTest {
    @Test fun no_key_never_attempts_online() {
        for (mode in NavMode.values()) {
            assertEquals(false, shouldAttemptOnline(mode, networkAvailable = true, hasKey = false))
        }
    }

    @Test fun offline_pref_never_attempts() {
        assertEquals(false, shouldAttemptOnline(NavMode.Offline, networkAvailable = true, hasKey = true))
    }

    @Test fun online_pref_attempts_even_without_network() {
        // Force-online still attempts; the fetch itself will fail and fall back to offline (handled at the edge).
        assertEquals(true, shouldAttemptOnline(NavMode.Online, networkAvailable = false, hasKey = true))
    }

    @Test fun auto_attempts_only_with_network() {
        assertEquals(true, shouldAttemptOnline(NavMode.Auto, networkAvailable = true, hasKey = true))
        assertEquals(false, shouldAttemptOnline(NavMode.Auto, networkAvailable = false, hasKey = true))
    }
}
