package com.bikeputer.data

import com.bikeputer.domain.CustomGrid
import com.bikeputer.domain.MapTile
import com.bikeputer.domain.MetricId
import com.bikeputer.domain.MetricTile
import com.bikeputer.domain.PlacedField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomGridCodecTest {

    @Test fun round_trips_multiple_grids_including_a_map() {
        val grids = listOf(
            CustomGrid.DEFAULT,
            CustomGrid("123", "Climb view", 6, listOf(
                PlacedField(MapTile, 0, 0),
                PlacedField(MetricTile(MetricId.GRADE), 3, 0),
                PlacedField(MetricTile(MetricId.HEART_RATE), 3, 1),
            )),
        )
        val decoded = CustomGridCodec.decode(CustomGridCodec.encode(grids))
        assertEquals(grids, decoded)
    }

    @Test fun blank_input_decodes_to_empty_list() {
        assertEquals(emptyList<CustomGrid>(), CustomGridCodec.decode(""))
        assertEquals(emptyList<CustomGrid>(), CustomGridCodec.decode("   "))
    }

    @Test fun delimiters_in_name_are_sanitized() {
        val g = CustomGrid("9", "a|b;c,d", 5, listOf(PlacedField(MetricTile(MetricId.POWER), 0, 0)))
        val decoded = CustomGridCodec.decode(CustomGridCodec.encode(listOf(g)))
        assertEquals(1, decoded.size)
        assertTrue(decoded[0].name.none { it == '|' || it == ';' || it == ',' })
    }

    @Test fun unknown_metric_token_is_skipped_but_grid_survives() {
        // hand-built line with one good field and one unknown metric
        val line = "7|Mixed|6|0,0,M:POWER;1,0,M:WATTAGE_OF_DOOM"
        val decoded = CustomGridCodec.decode(line)
        assertEquals(1, decoded.size)
        assertEquals(listOf(MetricTile(MetricId.POWER)), decoded[0].fields.map { it.content })
    }

    @Test fun malformed_lines_are_dropped_not_thrown() {
        assertEquals(emptyList<CustomGrid>(), CustomGridCodec.decode("garbage-without-pipes"))
        assertEquals(emptyList<CustomGrid>(), CustomGridCodec.decode("id|name|notanumber|0,0,M:POWER"))
    }

    @Test fun invalid_decoded_grid_is_dropped() {
        // map at col 1 is invalid → grid dropped
        assertEquals(emptyList<CustomGrid>(), CustomGridCodec.decode("id|name|6|0,1,MAP"))
    }
}
