package com.nearme.android.data.room

import com.nearme.core.fingerprint.DeviceFingerprint
import com.nearme.core.fingerprint.IntervalBucket
import com.nearme.core.identity.TrackedIdentity
import com.nearme.core.model.LocationBucket
import com.nearme.core.model.Sighting

private const val CSV_SEPARATOR = ","
private const val PREFIX_ENTRY_SEPARATOR = "|"
private const val PREFIX_KV_SEPARATOR = ":"

private fun Set<String>.toCsv() = joinToString(CSV_SEPARATOR)
private fun String.csvToStringSet() = if (isBlank()) emptySet() else split(CSV_SEPARATOR).toSet()
private fun Set<Int>.toCsvInts() = joinToString(CSV_SEPARATOR)
private fun String.csvToIntSet() = if (isBlank()) emptySet() else split(CSV_SEPARATOR).map { it.toInt() }.toSet()

private fun Map<Int, List<Byte>>.toPrefixCsv() = entries.joinToString(PREFIX_ENTRY_SEPARATOR) { (id, bytes) ->
    "$id$PREFIX_KV_SEPARATOR${bytes.joinToString("") { b -> "%02x".format(b) }}"
}

private fun String.csvToPrefixMap(): Map<Int, List<Byte>> {
    if (isBlank()) return emptyMap()
    return split(PREFIX_ENTRY_SEPARATOR).associate { entry ->
        val (idStr, hex) = entry.split(PREFIX_KV_SEPARATOR, limit = 2)
        val bytes = hex.chunked(2).filter { it.isNotEmpty() }.map { it.toInt(16).toByte() }
        idStr.toInt() to bytes
    }
}

fun TrackedIdentity.toEntity(): TrackedIdentityEntity = TrackedIdentityEntity(
    id = id,
    lastSeenMs = lastSeenMs,
    serviceUuidsCsv = fingerprint.serviceUuids.toCsv(),
    manufacturerIdsCsv = fingerprint.manufacturerCompanyIds.toCsvInts(),
    manufacturerPrefixesCsv = fingerprint.manufacturerPayloadPrefixes.toPrefixCsv(),
    nameHash = fingerprint.nameHash,
    txPowerLevel = fingerprint.txPowerLevel,
    advertisingIntervalBucket = fingerprint.advertisingIntervalBucket?.name,
)

fun Sighting.toEntity(identityId: String): SightingEntity = SightingEntity(
    identityId = identityId,
    timestampMs = timestampMs,
    rssi = rssi,
    locationBucketId = locationBucket.id,
    locationBucketLabel = locationBucket.label,
)

fun IdentityWithSightings.toDomain(): TrackedIdentity = TrackedIdentity(
    id = identity.id,
    lastSeenMs = identity.lastSeenMs,
    fingerprint = DeviceFingerprint(
        serviceUuids = identity.serviceUuidsCsv.csvToStringSet(),
        manufacturerCompanyIds = identity.manufacturerIdsCsv.csvToIntSet(),
        manufacturerPayloadPrefixes = identity.manufacturerPrefixesCsv.csvToPrefixMap(),
        nameHash = identity.nameHash,
        txPowerLevel = identity.txPowerLevel,
        advertisingIntervalBucket = identity.advertisingIntervalBucket?.let { IntervalBucket.valueOf(it) },
    ),
    sightings = sightings.map {
        Sighting(
            timestampMs = it.timestampMs,
            rssi = it.rssi,
            locationBucket = LocationBucket(it.locationBucketId, it.locationBucketLabel),
        )
    },
)
