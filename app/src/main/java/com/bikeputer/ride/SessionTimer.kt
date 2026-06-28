package com.bikeputer.ride

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SessionTimer(private val tickMs: Long = 1000) {
    private var banked = 0L
    private var startedAt: Long? = null

    fun start(now: Long = System.currentTimeMillis()) {
        if (startedAt == null) startedAt = now
    }

    fun pause(now: Long = System.currentTimeMillis()) {
        val s = startedAt ?: return
        banked += now - s
        startedAt = null
    }

    fun reset() { banked = 0L; startedAt = null }

    fun currentElapsed(now: Long): Long {
        val s = startedAt ?: return banked
        return banked + (now - s)
    }

    fun elapsedMs(now: () -> Long): Flow<Long> = flow {
        while (true) {
            emit(currentElapsed(now()))
            delay(tickMs)
        }
    }
}
