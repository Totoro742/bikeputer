package com.bikeputer.metrics

import java.util.ArrayDeque

class RollingAverage(private val windowMs: Long) {
    private data class Entry(val t: Long, val v: Double)
    private val q = ArrayDeque<Entry>()

    fun add(timestampMs: Long, value: Double) {
        q.addLast(Entry(timestampMs, value))
        val cutoff = timestampMs - windowMs
        while (true) {
            val head = q.peekFirst() ?: break
            if (head.t < cutoff) q.removeFirst() else break
        }
    }

    fun average(): Double? {
        if (q.isEmpty()) return null
        return q.sumOf { it.v } / q.size
    }
}
