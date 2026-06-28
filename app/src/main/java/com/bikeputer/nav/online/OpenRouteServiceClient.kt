package com.bikeputer.nav.online

import com.bikeputer.domain.GeoPos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * OpenRouteService cycling directions over plain HttpURLConnection (no extra deps).
 * GeoJSON coordinates are [lng, lat]; a step's turn point is its first way-point index
 * into the route geometry. Any HTTP/parse failure surfaces as an exception, which the
 * caller treats as "online unavailable" and falls back to offline.
 */
class OpenRouteServiceClient(private val apiKey: String) : RoutingClient {

    override suspend fun directions(waypoints: List<GeoPos>): RouteDirections = withContext(Dispatchers.IO) {
        val coords = waypoints.joinToString(",") { "[${it.lng},${it.lat}]" }
        val body = """{"coordinates":[$coords]}"""
        val url = URL("https://api.openrouteservice.org/v2/directions/cycling-regular/geojson")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Authorization", apiKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/geo+json, application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) {
                error("ORS HTTP ${conn.responseCode}")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            parseOrsDirections(text)
        } finally {
            conn.disconnect()
        }
    }
}

/** Parse an ORS directions GeoJSON body into [RouteDirections]. Pure; unit-tested. */
fun parseOrsDirections(json: String): RouteDirections {
    val root = JSONObject(json)
    val feature = root.getJSONArray("features").getJSONObject(0)
    val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
    val geometry = ArrayList<GeoPos>(coords.length())
    for (i in 0 until coords.length()) {
        val c = coords.getJSONArray(i)
        geometry.add(GeoPos(lat = c.getDouble(1), lng = c.getDouble(0)))
    }
    val steps = ArrayList<RouteStep>()
    val segments = feature.getJSONObject("properties").getJSONArray("segments")
    for (s in 0 until segments.length()) {
        val stepArr = segments.getJSONObject(s).getJSONArray("steps")
        for (i in 0 until stepArr.length()) {
            val step = stepArr.getJSONObject(i)
            val wpStart = step.getJSONArray("way_points").getInt(0)
            val point = geometry.getOrElse(wpStart) { geometry.last() }
            steps.add(RouteStep(step.getInt("type"), step.optString("name", "-"), point))
        }
    }
    return RouteDirections(steps, geometry)
}
