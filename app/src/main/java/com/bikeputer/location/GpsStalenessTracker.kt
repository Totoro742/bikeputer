package com.bikeputer.location

/**
 * Pure clock-driven gate: GPS is "available" only if a fresh fix arrived within
 * [staleMs]. Lets the dashboard's "no GPS" degradation trigger on a mid-ride
 * dropout, not only on explicit stop().
 */
class GpsStalenessTracker(private val staleMs: Long = 3000) {
    private var lastSampleAtMs: Long? = null

    fun onSample(nowMs: Long) {
        lastSampleAtMs = nowMs
    }

    fun isAvailable(nowMs: Long): Boolean {
        val last = lastSampleAtMs ?: return false
        return nowMs - last <= staleMs
    }
}
