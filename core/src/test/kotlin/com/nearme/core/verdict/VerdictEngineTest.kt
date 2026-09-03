package com.nearme.core.verdict

import com.nearme.core.correlation.CorrelationResult
import com.nearme.core.model.LocationBucket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerdictEngineTest {

    @Test
    fun `single location is normal`() {
        val result = CorrelationResult(
            identityId = "id",
            distinctLocations = setOf(LocationBucket("home")),
            totalSightings = 5,
            firstSeenMs = 0,
            lastSeenMs = 10_000,
            followingScore = 0.0,
        )
        assertEquals(Verdict.NORMAL, VerdictEngine.evaluate(result).verdict)
    }

    @Test
    fun `high following score is suspicious`() {
        val result = CorrelationResult(
            identityId = "id",
            distinctLocations = setOf(LocationBucket("home"), LocationBucket("gym"), LocationBucket("store")),
            totalSightings = 3,
            firstSeenMs = 0,
            lastSeenMs = 3_600_000,
            followingScore = 0.8,
        )
        assertEquals(Verdict.SUSPICIOUS, VerdictEngine.evaluate(result).verdict)
    }

    @Test
    fun `low-but-nonzero following score is worth noting`() {
        val result = CorrelationResult(
            identityId = "id",
            distinctLocations = setOf(LocationBucket("home"), LocationBucket("gym")),
            totalSightings = 2,
            firstSeenMs = 0,
            lastSeenMs = 60L * 24 * 60 * 60 * 1000,
            followingScore = 0.1,
        )
        assertEquals(Verdict.WORTH_NOTING, VerdictEngine.evaluate(result).verdict)
    }
}
