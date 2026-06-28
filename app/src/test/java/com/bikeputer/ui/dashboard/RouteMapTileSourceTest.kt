package com.bikeputer.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMapTileSourceTest {
    @Test fun light_theme_selects_positron() {
        assertEquals("CartoPositron", tileSource(true).name())
    }

    @Test fun dark_theme_selects_dark_matter() {
        assertEquals("CartoDarkMatter", tileSource(false).name())
    }
}
