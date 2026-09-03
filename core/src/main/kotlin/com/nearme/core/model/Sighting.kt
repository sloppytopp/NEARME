package com.nearme.core.model

/** One confirmed observation of a resolved (post-fingerprint-match) device identity. */
data class Sighting(
    val timestampMs: Long,
    val rssi: Int,
    val locationBucket: LocationBucket,
)
