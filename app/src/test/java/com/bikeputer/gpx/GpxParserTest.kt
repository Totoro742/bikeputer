package com.bikeputer.gpx

import com.bikeputer.domain.GeoPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxParserTest {
    private fun parse(xml: String) = GpxParser.parse(xml.byteInputStream())

    @Test fun parses_track_points_and_name() {
        val xml = """
            <gpx><trk><name>Morning Loop</name><trkseg>
              <trkpt lat="50.0" lon="19.0"/>
              <trkpt lat="50.1" lon="19.1"/>
            </trkseg></trk></gpx>
        """.trimIndent()
        val r = parse(xml)
        assertEquals("Morning Loop", r.name)
        assertEquals(listOf(GeoPos(50.0, 19.0), GeoPos(50.1, 19.1)), r.points)
    }

    @Test fun parses_route_points() {
        val xml = """<gpx><rte><rtept lat="1.0" lon="2.0"/><rtept lat="1.5" lon="2.5"/></rte></gpx>"""
        assertEquals(listOf(GeoPos(1.0, 2.0), GeoPos(1.5, 2.5)), parse(xml).points)
    }

    @Test fun handles_namespaced_gpx() {
        val xml = """
            <?xml version="1.0"?>
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1">
              <trk><trkseg><trkpt lat="3.0" lon="4.0"/></trkseg></trk>
            </gpx>
        """.trimIndent()
        assertEquals(listOf(GeoPos(3.0, 4.0)), parse(xml).points)
    }

    @Test fun prefers_track_name_over_metadata() {
        val xml = """
            <gpx><metadata><name>File Meta</name></metadata>
              <trk><name>Real Route</name><trkseg><trkpt lat="0.0" lon="0.0"/></trkseg></trk></gpx>
        """.trimIndent()
        assertEquals("Real Route", parse(xml).name)
    }

    @Test fun null_name_when_absent() {
        val xml = """<gpx><trk><trkseg><trkpt lat="0.0" lon="0.0"/></trkseg></trk></gpx>"""
        assertNull(parse(xml).name)
    }

    @Test fun empty_points_when_no_track_or_route() {
        assertTrue(parse("""<gpx><metadata><name>Nothing</name></metadata></gpx>""").points.isEmpty())
    }

    @Test fun throws_on_malformed_xml() {
        assertThrows(GpxParseException::class.java) { parse("<gpx><trk>") }
    }
}
