package com.nearme.android.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirrors [com.nearme.core.fingerprint.DeviceFingerprint] + identity metadata.
 * Set-valued fields are stored as comma-joined strings and manufacturer prefix
 * bytes as a compact hex-joined string — deliberately not JSON, since every
 * field here is already a flat collection of primitives.
 */
@Entity(tableName = "tracked_identity")
data class TrackedIdentityEntity(
    @PrimaryKey val id: String,
    val lastSeenMs: Long,
    val serviceUuidsCsv: String,
    val manufacturerIdsCsv: String,
    val manufacturerPrefixesCsv: String, // "id1:aabbccdd|id2:eeff"
    val nameHash: Int?,
    val txPowerLevel: Int?,
    val advertisingIntervalBucket: String?,
)

@Entity(
    tableName = "sighting",
    foreignKeys = [
        ForeignKey(
            entity = TrackedIdentityEntity::class,
            parentColumns = ["id"],
            childColumns = ["identityId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("identityId")],
)
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val identityId: String,
    val timestampMs: Long,
    val rssi: Int,
    val locationBucketId: String,
    val locationBucketLabel: String?,
)

data class IdentityWithSightings(
    val identity: TrackedIdentityEntity,
    val sightings: List<SightingEntity>,
)
