package com.nearme.core.identity

import com.nearme.core.fingerprint.DeviceFingerprint
import com.nearme.core.fingerprint.FingerprintExtractor
import com.nearme.core.fingerprint.FingerprintMatcher
import com.nearme.core.model.BleAdvertisement
import com.nearme.core.model.Sighting
import java.util.UUID

/**
 * Maintains the set of [TrackedIdentity] candidates and resolves each new
 * advertisement to an existing identity (same physical device, rotated MAC)
 * or spawns a new one.
 *
 * Not thread-safe; callers should serialize access (e.g. process advertisements
 * on a single scanning coroutine/dispatcher).
 */
class IdentityResolver(
    private val matchThreshold: Double = 0.75,
    private val stalenessWindowMs: Long = 14L * 24 * 60 * 60 * 1000, // 14 days
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val identities = mutableMapOf<String, TrackedIdentity>()

    fun resolve(advertisement: BleAdvertisement): TrackedIdentity {
        val fingerprint = FingerprintExtractor.extract(advertisement)
        val sighting = Sighting(
            timestampMs = advertisement.timestampMs,
            rssi = advertisement.rssi,
            locationBucket = advertisement.locationBucket,
        )

        val best = identities.values
            .asSequence()
            .filter { advertisement.timestampMs - it.lastSeenMs <= stalenessWindowMs }
            .map { it to FingerprintMatcher.similarity(it.fingerprint, fingerprint) }
            .maxByOrNull { it.second }

        val resolved = if (best != null && best.second >= matchThreshold) {
            val existing = best.first
            existing.copy(
                fingerprint = mergeFingerprints(existing.fingerprint, fingerprint),
                lastSeenMs = advertisement.timestampMs,
                sightings = existing.sightings + sighting,
            )
        } else {
            TrackedIdentity(
                id = idGenerator(),
                fingerprint = fingerprint,
                lastSeenMs = advertisement.timestampMs,
                sightings = listOf(sighting),
            )
        }

        identities[resolved.id] = resolved
        return resolved
    }

    fun allIdentities(): List<TrackedIdentity> = identities.values.toList()

    /** Seeds the resolver with identities loaded from persistent storage (e.g. on app start). */
    fun restore(saved: List<TrackedIdentity>) {
        saved.forEach { identities[it.id] = it }
    }

    /** Drops identities not seen within [stalenessWindowMs] of [nowMs], to bound memory. */
    fun pruneStale(nowMs: Long) {
        identities.values
            .filter { nowMs - it.lastSeenMs > stalenessWindowMs }
            .forEach { identities.remove(it.id) }
    }

    /**
     * Incrementally widens the tracked fingerprint with newly observed fields.
     * Service UUIDs and manufacturer IDs accumulate (a device sometimes omits
     * fields on a given advertisement cycle); scalar fields take the latest
     * non-null reading.
     */
    private fun mergeFingerprints(existing: DeviceFingerprint, incoming: DeviceFingerprint): DeviceFingerprint {
        return DeviceFingerprint(
            serviceUuids = existing.serviceUuids + incoming.serviceUuids,
            manufacturerCompanyIds = existing.manufacturerCompanyIds + incoming.manufacturerCompanyIds,
            manufacturerPayloadPrefixes = existing.manufacturerPayloadPrefixes + incoming.manufacturerPayloadPrefixes,
            nameHash = incoming.nameHash ?: existing.nameHash,
            txPowerLevel = incoming.txPowerLevel ?: existing.txPowerLevel,
            advertisingIntervalBucket = incoming.advertisingIntervalBucket ?: existing.advertisingIntervalBucket,
        )
    }
}
