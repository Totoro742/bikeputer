package com.bikeputer.data

import com.bikeputer.domain.GeoPos
import com.bikeputer.domain.SavedRoute
import com.bikeputer.gpx.GpxParser
import java.io.File
import java.io.InputStream

/**
 * Persists imported GPX routes under [baseDir]/routes as a raw `<id>.gpx` plus a
 * plain-text `<id>.meta` sidecar (line 1 = display name, line 2 = point count).
 * No JSON or Android types, so the whole store is unit-testable against a temp dir.
 */
class RouteStore(
    baseDir: File,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val dir = File(baseDir, "routes")

    /**
     * Reads [input] once (the SAF stream is one-shot), parses it to validate and
     * count points (rejecting an empty route), then writes the same bytes and the
     * metadata. Display name is the GPX name, else [fallbackName].
     */
    fun import(fallbackName: String, input: InputStream): SavedRoute {
        val bytes = input.use { it.readBytes() }
        val parsed = GpxParser.parse(bytes.inputStream())
        require(parsed.points.isNotEmpty()) { "GPX has no track or route points" }
        dir.mkdirs()
        val id = now().toString()
        val name = (parsed.name?.takeIf { it.isNotBlank() } ?: fallbackName)
            .replace("\n", " ").trim()
        File(dir, "$id.gpx").writeBytes(bytes)
        File(dir, "$id.meta").writeText(name + "\n" + parsed.points.size)
        return SavedRoute(id, name, parsed.points.size)
    }

    fun list(): List<SavedRoute> {
        val metas = dir.listFiles { f -> f.extension == "meta" } ?: return emptyList()
        return metas.mapNotNull { f ->
            val lines = f.readLines()
            val name = lines.getOrNull(0) ?: return@mapNotNull null
            SavedRoute(f.nameWithoutExtension, name, lines.getOrNull(1)?.toIntOrNull() ?: 0)
        }.sortedByDescending { it.id.toLongOrNull() ?: 0L }
    }

    fun loadPoints(id: String): List<GeoPos> {
        val gpx = File(dir, "$id.gpx")
        if (!gpx.exists()) return emptyList()
        return GpxParser.parse(gpx.inputStream()).points
    }

    fun delete(id: String) {
        File(dir, "$id.gpx").delete()
        File(dir, "$id.meta").delete()
    }
}
