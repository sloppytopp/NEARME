package com.nearme.core.correlation

import com.nearme.core.model.LocationBucket

data class CorrelationResult(
    val identityId: String,
    val distinctLocations: Set<LocationBucket>,
    val totalSightings: Int,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    /** 0.0 (definitely stationary) to 1.0 (strongly following-pattern). */
    val followingScore: Double,
) {
    val timeSpanMs: Long get() = lastSeenMs - firstSeenMs
}
