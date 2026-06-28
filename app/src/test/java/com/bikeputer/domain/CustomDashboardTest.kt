package com.bikeputer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDashboardTest {

    @Test fun footprint_metric_is_1x1_and_map_is_3x3() {
        assertEquals(1 to 1, footprint(MetricTile(MetricId.POWER)))
        assertEquals(3 to 3, footprint(MapTile))
    }

    @Test fun map_covers_a_full_3x3_block_from_its_anchor() {
        val map = PlacedField(MapTile, row = 1, col = 0)
        val cells = map.cells().toSet()
        assertEquals(9, cells.size)
        assertTrue(cells.contains(1 to 0))
        assertTrue(cells.contains(3 to 2))
        assertFalse(cells.contains(0 to 0))
    }

    @Test fun cellAt_finds_the_field_covering_a_cell() {
        val grid = CustomGrid("g", "n", 6, listOf(PlacedField(MapTile, 0, 0)))
        assertEquals(MapTile, grid.cellAt(2, 1)?.content)
        assertNull(grid.cellAt(3, 0)) // row 3 is outside a 0..2 map block
    }

    @Test fun isValid_rejects_bad_rows_overlap_oob_and_offset_map() {
        assertTrue(CustomGrid.DEFAULT.isValid())
        assertFalse(CustomGrid("g", "n", 4, emptyList()).isValid()) // rows not 5/6
        // overlap
        assertFalse(
            CustomGrid("g", "n", 5, listOf(
                PlacedField(MetricTile(MetricId.POWER), 0, 0),
                PlacedField(MetricTile(MetricId.SPEED), 0, 0),
            )).isValid()
        )
        // out of bounds (col 3 doesn't exist)
        assertFalse(CustomGrid("g", "n", 5, listOf(PlacedField(MetricTile(MetricId.POWER), 0, 3))).isValid())
        // map must be at col 0
        assertFalse(CustomGrid("g", "n", 6, listOf(PlacedField(MapTile, 0, 1))).isValid())
        // map runs off the bottom of a 5-row grid when anchored at row 3
        assertFalse(CustomGrid("g", "n", 5, listOf(PlacedField(MapTile, 3, 0))).isValid())
    }

    @Test fun place_adds_when_it_fits_and_is_noop_when_it_overlaps() {
        val empty = CustomGrid("g", "n", 6, emptyList())
        val one = empty.place(MetricTile(MetricId.POWER), 0, 0)
        assertEquals(1, one.fields.size)
        val blocked = one.place(MetricTile(MetricId.SPEED), 0, 0) // occupied
        assertEquals(one, blocked)
        val mapBlocked = one.place(MapTile, 0, 0) // map would cover (0,0)
        assertEquals(one, mapBlocked)
    }

    @Test fun clear_removes_the_field_covering_a_cell() {
        val grid = CustomGrid("g", "n", 6, listOf(PlacedField(MapTile, 0, 0)))
        assertTrue(grid.clear(1, 1).fields.isEmpty()) // clearing any covered cell drops the map
    }

    @Test fun setRows_coerces_and_drops_fields_that_no_longer_fit() {
        val grid = CustomGrid("g", "n", 6, listOf(
            PlacedField(MetricTile(MetricId.POWER), 0, 0),
            PlacedField(MetricTile(MetricId.SPEED), 5, 0), // only valid in a 6-row grid
        ))
        val shrunk = grid.setRows(5)
        assertEquals(5, shrunk.rows)
        assertEquals(listOf(MetricTile(MetricId.POWER)), shrunk.fields.map { it.content })
        assertEquals(6, grid.setRows(9).rows) // coerced to 6
    }
}
