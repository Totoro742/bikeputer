package com.bikeputer.gpx

import com.bikeputer.domain.GeoPos
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

class GpxParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ParsedRoute(val name: String?, val points: List<GeoPos>)

/**
 * Streaming GPX reader built on the JDK/Android SAX parser (works under plain
 * JVM unit tests, unlike org.xmlpull). Collects <trkpt>/<rtept> coordinates and
 * the route name; ignores <wpt>, elevation and time.
 */
object GpxParser {
    fun parse(input: InputStream): ParsedRoute {
        val handler = GpxHandler()
        try {
            val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
            factory.newSAXParser().parse(input, handler)
        } catch (e: Exception) {
            throw GpxParseException("Failed to parse GPX", e)
        }
        return ParsedRoute(handler.trkRteName ?: handler.otherName, handler.points)
    }
}

private class GpxHandler : DefaultHandler() {
    val points = mutableListOf<GeoPos>()
    var trkRteName: String? = null
    var otherName: String? = null

    private val stack = ArrayDeque<String>()
    private var capturing = false
    private var nameParent: String? = null
    private val buf = StringBuilder()

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        val name = local(localName, qName)
        when (name) {
            "trkpt", "rtept" -> {
                val lat = attributes?.getValue("lat")?.toDoubleOrNull()
                val lon = attributes?.getValue("lon")?.toDoubleOrNull()
                if (lat != null && lon != null) points.add(GeoPos(lat, lon))
            }
            "name" -> {
                capturing = true
                nameParent = stack.lastOrNull()
                buf.setLength(0)
            }
        }
        stack.addLast(name)
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (capturing) buf.append(ch, start, length)
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        val name = local(localName, qName)
        stack.removeLastOrNull()
        if (name == "name" && capturing) {
            val text = buf.toString().trim()
            if (text.isNotEmpty()) {
                if (nameParent == "trk" || nameParent == "rte") {
                    if (trkRteName == null) trkRteName = text
                } else if (otherName == null) {
                    otherName = text
                }
            }
            capturing = false
        }
    }

    private fun local(localName: String?, qName: String?): String =
        (localName?.ifEmpty { null } ?: qName ?: "").substringAfterLast(':')
}
