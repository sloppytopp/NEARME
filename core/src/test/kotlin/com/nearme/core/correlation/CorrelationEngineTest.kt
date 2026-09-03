package com.nearme.core.correlation

import com.nearme.core.identity.TrackedIdentity
import com.nearme.core.model.LocationBucket
import com.nearme.core.model.Sighting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CorrelationEngineTest {

    private fun identity(sightings: List<Sighting>) = TrackedIdentity(
        id = "id-1",
        fingerprint = com.nearme.core.fingerprint.DeviceFingerprint(emptySet(), emptySet(), emptyMap(), null, null, null),
        lastSeenMs = sightings.maxOf { it.timestampMs },
        sightings = sightings,
    )

    @Test
    fun `single location scores zero regardless of repeat count`() {
        val home = LocationBucket("home")
        val sightings = (0 until 10).map { Sighting(timestampMs = it * 60_000L, rssi = -50, locationBucket = home) }

        val result = CorrelationEngine.evaluate(identity(sightings))

        assertEquals(0.0, result.followingScore)
        assertEquals(1, result.distinctLocations.size)
    }

    @Test
    fun `multiple locations compressed in time score high`() {
        val hour = 60 * 60 * 1000L
        val sightings = listOf(
            Sighting(timestampMs = 0, rssi = -50, locationBucket = LocationBucket("home")),
            Sighting(timestampMs = hour, rssi = -50, locationBucket = LocationBucket("gym")),
            Sighting(timestampMs = 2 * hour, rssi = -50, locationBucket = LocationBucket("grocery-store")),
        )

        val result = CorrelationEngine.evaluate(identity(sightings))

        assertTrue(result.followingScore > 0.5) { "expected a high following score, got ${result.followingScore}" }
    }

    @Test
    fun `multiple locations spread over months score lower than compressed sightings`() {
        val month = 30L * 24 * 60 * 60 * 1000
        val compressed = listOf(
            Sighting(timestampMs = 0, rssi = -50, locationBucket = LocationBucket("home")),
            Sighting(timestampMs = 3600_000, rssi = -50, locationBucket = LocationBucket("gym")),
        )
        val spread = listOf(
            Sighting(timestampMs = 0, rssi = -50, locationBucket = LocationBucket("home")),
            Sighting(timestampMs = 3 * month, rssi = -50, locationBucket = LocationBucket("gym")),
        )

        val compressedScore = CorrelationEngine.evaluate(identity(compressed)).followingScore
        val spreadScore = CorrelationEngine.evaluate(identity(spread)).followingScore

        assertTrue(compressedScore > spreadScore)
    }
}
