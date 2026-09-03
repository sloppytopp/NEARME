package com.nearme.core.correlation

import com.nearme.core.identity.TrackedIdentity
import kotlin.math.min

/**
 * Turns a device's sighting history into a "does this look like it's following
 * the user" score. A device seen repeatedly at exactly one place (a neighbor's
 * smart speaker, the user's own earbuds) is never suspicious no matter how many
 * times it's seen there — the signal is appearing across *distinct* places the
 * user has been, especially when those appearances are compressed in time.
 */
object CorrelationEngine {
    /** Locations spread over this window or less count fully toward the time factor. */
    private const val COMPRESSED_WINDOW_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

    fun evaluate(identity: TrackedIdentity): CorrelationResult {
        val sightings = identity.sightings
        val distinctLocations = sightings.map { it.locationBucket }.toSet()
        val firstSeenMs = sightings.minOf { it.timestampMs }
        val lastSeenMs = sightings.maxOf { it.timestampMs }

        val followingScore = if (distinctLocations.size <= 1) {
            0.0
        } else {
            val locationFactor = min(1.0, (distinctLocations.size - 1) / 4.0)
            val timeSpanMs = lastSeenMs - firstSeenMs
            val timeFactor = if (timeSpanMs <= 0) {
                1.0
            } else {
                (1.0 - (timeSpanMs.toDouble() / COMPRESSED_WINDOW_MS)).coerceIn(0.0, 1.0)
            }
            ((locationFactor * 0.7) + (timeFactor * 0.3)).coerceIn(0.0, 1.0)
        }

        return CorrelationResult(
            identityId = identity.id,
            distinctLocations = distinctLocations,
            totalSightings = sightings.size,
            firstSeenMs = firstSeenMs,
            lastSeenMs = lastSeenMs,
            followingScore = followingScore,
        )
    }
}
