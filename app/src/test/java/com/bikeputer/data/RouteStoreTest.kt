package com.bikeputer.data

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RouteStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val trackGpx = """
        <gpx><trk><name>Loop</name><trkseg>
          <trkpt lat="50.0" lon="19.0"/><trkpt lat="50.1" lon="19.1"/>
        </trkseg></trk></gpx>
    """.trimIndent()

    @Test fun import_then_load_round_trips() {
        val store = RouteStore(tmp.root)
        val saved = store.import("file.gpx", trackGpx.byteInputStream())
        assertEquals("Loop", saved.name)
        assertEquals(2, saved.pointCount)
        assertEquals(listOf(GeoPos(50.0, 19.0), GeoPos(50.1, 19.1)), store.loadPoints(saved.id))
    }

    @Test fun import_falls_back_to_filename_when_no_gpx_name() {
        val store = RouteStore(tmp.root)
        val xml = """<gpx><trk><trkseg><trkpt lat="0.0" lon="0.0"/></trkseg></trk></gpx>"""
        assertEquals("my-ride.gpx", store.import("my-ride.gpx", xml.byteInputStream()).name)
    }

    @Test fun import_rejects_gpx_with_no_points() {
        val store = RouteStore(tmp.root)
        val xml = """<gpx><metadata><name>Empty</name></metadata></gpx>"""
        assertThrows(IllegalArgumentException::class.java) {
            store.import("e.gpx", xml.byteInputStream())
        }
    }

    @Test fun list_returns_saved_routes_newest_first() {
        var t = 0L
        val store = RouteStore(tmp.root, now = { t })
        t = 1000L; store.import("a.gpx", trackGpx.byteInputStream())
        t = 2000L; store.import("b.gpx", trackGpx.byteInputStream())
        assertEquals(listOf("2000", "1000"), store.list().map { it.id })
    }

    @Test fun delete_removes_route() {
        val store = RouteStore(tmp.root)
        val saved = store.import("file.gpx", trackGpx.byteInputStream())
        store.delete(saved.id)
        assertTrue(store.list().isEmpty())
        assertTrue(store.loadPoints(saved.id).isEmpty())
    }

    @Test fun load_points_missing_id_returns_empty() {
        assertTrue(RouteStore(tmp.root).loadPoints("nope").isEmpty())
    }
}
