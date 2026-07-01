package com.bikeputer.domain

/** Fixed column count for every custom grid. */
const val GRID_COLUMNS = 3

/** Stable identity of a placeable ride metric (1×1 tile). Persisted by name. */
enum class MetricId {
    POWER, AVG_POWER_3S, AVG_POWER_10S, NORMALIZED_POWER, SESSION_AVG_POWER,
    FTP_PERCENT, POWER_ZONE, CADENCE, HEART_RATE, AVG_HEART_RATE, HR_ZONE,
    SPEED, AVG_SPEED, MAX_SPEED, DISTANCE, ELEVATION_GAIN, ELAPSED, ENERGY_KJ, GRADE
}

/** What a grid cell shows. */
sealed interface TileContent
data class MetricTile(val metric: MetricId) : TileContent
data object MapTile : TileContent

/** A field anchored at a top-left cell; footprint is implied by [content]. */
data class PlacedField(val content: TileContent, val row: Int, val col: Int)

data class CustomGrid(
    val id: String,
    val name: String,
    val rows: Int,
    val fields: List<PlacedField>,
) {
    companion object {
        /** Seed grid: a metric-only 6×3, no map. */
        val DEFAULT = CustomGrid(
            id = "default",
            name = "My grid",
            rows = 6,
            fields = listOf(
                PlacedField(MetricTile(MetricId.POWER), 0, 0),
                PlacedField(MetricTile(MetricId.HEART_RATE), 0, 1),
                PlacedField(MetricTile(MetricId.CADENCE), 0, 2),
                PlacedField(MetricTile(MetricId.SPEED), 1, 0),
                PlacedField(MetricTile(MetricId.DISTANCE), 1, 1),
                PlacedField(MetricTile(MetricId.ELAPSED), 1, 2),
                PlacedField(MetricTile(MetricId.ELEVATION_GAIN), 2, 0),
                PlacedField(MetricTile(MetricId.NORMALIZED_POWER), 2, 1),
                PlacedField(MetricTile(MetricId.ENERGY_KJ), 2, 2),
            ),
        )
    }
}

/** (rows, cols) a content occupies. */
fun footprint(content: TileContent): Pair<Int, Int> = when (content) {
    MapTile -> 3 to 3
    is MetricTile -> 1 to 1
}

/** Every (row, col) cell this field covers. */
fun PlacedField.cells(): List<Pair<Int, Int>> {
    val (h, w) = footprint(content)
    val out = ArrayList<Pair<Int, Int>>(h * w)
    for (r in row until row + h) for (c in col until col + w) out += r to c
    return out
}

/** The field covering (row, col), or null if that cell is empty. */
fun CustomGrid.cellAt(row: Int, col: Int): PlacedField? =
    fields.firstOrNull { f -> f.cells().any { it.first == row && it.second == col } }

fun CustomGrid.isValid(): Boolean {
    if (rows != 5 && rows != 6) return false
    val seen = HashSet<Pair<Int, Int>>()
    for (f in fields) {
        if (f.content is MapTile && f.col != 0) return false
        for (cell in f.cells()) {
            val (r, c) = cell
            if (r !in 0 until rows || c !in 0 until GRID_COLUMNS) return false
            if (!seen.add(cell)) return false
        }
    }
    return true
}

/** Whether [content] at (row, col) fits in bounds and overlaps no existing field. */
fun CustomGrid.canPlace(content: TileContent, row: Int, col: Int): Boolean {
    if (content is MapTile && col != 0) return false
    val occupied = fields.flatMap { it.cells() }.toHashSet()
    for (cell in PlacedField(content, row, col).cells()) {
        val (r, c) = cell
        if (r !in 0 until rows || c !in 0 until GRID_COLUMNS) return false
        if (cell in occupied) return false
    }
    return true
}

fun CustomGrid.place(content: TileContent, row: Int, col: Int): CustomGrid =
    if (canPlace(content, row, col)) copy(fields = fields + PlacedField(content, row, col)) else this

fun CustomGrid.clear(row: Int, col: Int): CustomGrid {
    val target = cellAt(row, col) ?: return this
    return copy(fields = fields - target)
}

fun CustomGrid.setRows(n: Int): CustomGrid {
    val rowsN = n.coerceIn(5, 6)
    val kept = fields.filter { f -> f.cells().all { it.first in 0 until rowsN } }
    return copy(rows = rowsN, fields = kept)
}
