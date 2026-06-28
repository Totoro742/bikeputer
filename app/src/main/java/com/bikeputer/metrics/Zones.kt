package com.bikeputer.metrics

data class ZoneModel(val upperBounds: List<Int>) {
    fun zoneOf(value: Int): Int {
        upperBounds.forEachIndexed { i, bound -> if (value <= bound) return i + 1 }
        return upperBounds.size + 1
    }
}

object Zones {
    fun powerZonesFromFtp(ftp: Int): ZoneModel = ZoneModel(
        listOf(0.55, 0.75, 0.90, 1.05, 1.20, 1.50).map { (it * ftp).toInt() }
    )

    fun hrZonesFromMax(maxHr: Int): ZoneModel = ZoneModel(
        listOf(0.60, 0.70, 0.80, 0.90).map { (it * maxHr).toInt() }
    )
}
