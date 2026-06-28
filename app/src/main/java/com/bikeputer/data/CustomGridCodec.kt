package com.bikeputer.data

import com.bikeputer.domain.CustomGrid
import com.bikeputer.domain.MapTile
import com.bikeputer.domain.MetricId
import com.bikeputer.domain.MetricTile
import com.bikeputer.domain.PlacedField
import com.bikeputer.domain.TileContent
import com.bikeputer.domain.isValid

/**
 * Pure-Kotlin, no-JSON serialization for custom grids, in the same spirit as
 * [RouteStore]'s plain-text sidecars. One grid per line:
 *   id|name|rows|row,col,content;row,col,content;...
 * where content is `MAP` or `M:<MetricId>`. Robust to malformed/unknown tokens.
 */
object CustomGridCodec {

    fun encode(grids: List<CustomGrid>): String =
        grids.joinToString("\n") { encodeGrid(it) }

    fun decode(s: String): List<CustomGrid> =
        if (s.isBlank()) emptyList()
        else s.split("\n").mapNotNull { decodeGrid(it) }.filter { it.isValid() }

    private fun encodeGrid(g: CustomGrid): String {
        val name = g.name.replace('|', ' ').replace(';', ' ').replace(',', ' ').trim()
        val fields = g.fields.joinToString(";") { "${it.row},${it.col},${encodeContent(it.content)}" }
        return "${g.id}|$name|${g.rows}|$fields"
    }

    private fun encodeContent(c: TileContent): String = when (c) {
        MapTile -> "MAP"
        is MetricTile -> "M:${c.metric.name}"
    }

    private fun decodeGrid(line: String): CustomGrid? {
        val parts = line.split("|")
        if (parts.size < 4) return null
        val id = parts[0].ifBlank { return null }
        val name = parts[1]
        val rows = parts[2].toIntOrNull() ?: return null
        val fields = parts[3].split(";").filter { it.isNotBlank() }.mapNotNull { decodeField(it) }
        return CustomGrid(id, name, rows, fields)
    }

    private fun decodeField(token: String): PlacedField? {
        val p = token.split(",")
        if (p.size < 3) return null
        val row = p[0].toIntOrNull() ?: return null
        val col = p[1].toIntOrNull() ?: return null
        val content = decodeContent(p[2]) ?: return null
        return PlacedField(content, row, col)
    }

    private fun decodeContent(token: String): TileContent? = when {
        token == "MAP" -> MapTile
        token.startsWith("M:") ->
            runCatching { MetricId.valueOf(token.removePrefix("M:")) }.getOrNull()?.let { MetricTile(it) }
        else -> null
    }
}
