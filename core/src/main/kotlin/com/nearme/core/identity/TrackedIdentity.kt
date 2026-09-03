package com.nearme.core.identity

import com.nearme.core.fingerprint.DeviceFingerprint
import com.nearme.core.model.Sighting

/**
 * A physical device tracked across MAC address rotations, identified purely by
 * fingerprint similarity rather than by any single stable address. [id] is an
 * internal handle generated on first sighting — it has no relation to any
 * hardware identifier.
 */
data class TrackedIdentity(
    val id: String,
    val fingerprint: DeviceFingerprint,
    val lastSeenMs: Long,
    val sightings: List<Sighting>,
)
