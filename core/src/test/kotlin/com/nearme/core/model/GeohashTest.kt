package com.nearme.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GeohashTest {

    @Test
    fun `matches the well-known Wikipedia example vector`() {
        // en.wikipedia.org/wiki/Geohash worked example: this point encodes to "ezs42".
        assertEquals("ezs42", Geohash.encode(42.60498046875, -5.60302734375, precision = 5))
    }

    @Test
    fun `encoding is deterministic`() {
        val a = Geohash.encode(34.052235, -118.243683, precision = 7)
        val b = Geohash.encode(34.052235, -118.243683, precision = 7)
        assertEquals(a, b)
    }

    @Test
    fun `decoded bounds always contain the original point`() {
        val points = listOf(
            0.0 to 0.0,
            89.9 to 179.9,
            -89.9 to -179.9,
            34.052235 to -118.243683,
            51.5074 to -0.1278,
        )
        for ((lat, lon) in points) {
            for (precision in 1..9) {
                val hash = Geohash.encode(lat, lon, precision)
                val bounds = Geohash.decodeBounds(hash)
                assertTrue(bounds.contains(lat, lon)) {
                    "precision $precision hash $hash bounds $bounds did not contain ($lat, $lon)"
                }
            }
        }
    }

    @Test
    fun `points within the same cell share a bucket`() {
        // A raw fixed-degree offset can cross a cell boundary near an edge, so
        // derive the second point from the first cell's own bounds instead of
        // guessing an offset that happens to stay inside it.
        val a = Geohash.encode(37.422000, -122.084000, precision = 7)
        val bounds = Geohash.decodeBounds(a)
        val nearbyLat = bounds.latMin + (bounds.latMax - bounds.latMin) * 0.5
        val nearbyLon = bounds.lonMin + (bounds.lonMax - bounds.lonMin) * 0.5
        val b = Geohash.encode(nearbyLat, nearbyLon, precision = 7)
        assertEquals(a, b)
    }

    @Test
    fun `distant points never share a bucket`() {
        val home = Geohash.encode(37.7749, -122.4194, precision = 7) // San Francisco
        val farAway = Geohash.encode(40.7128, -74.0060, precision = 7) // New York
        assertNotEquals(home, farAway)
    }
}
