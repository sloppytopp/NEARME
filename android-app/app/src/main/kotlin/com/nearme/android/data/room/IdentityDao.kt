package com.nearme.android.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface IdentityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIdentity(identity: TrackedIdentityEntity)

    @Insert
    suspend fun insertSighting(sighting: SightingEntity)

    @Query("SELECT * FROM tracked_identity")
    suspend fun getAllIdentities(): List<TrackedIdentityEntity>

    @Query("SELECT * FROM sighting WHERE identityId = :identityId ORDER BY timestampMs ASC")
    suspend fun getSightingsFor(identityId: String): List<SightingEntity>

    @Transaction
    suspend fun getAllIdentitiesWithSightings(): List<IdentityWithSightings> =
        getAllIdentities().map { identity -> IdentityWithSightings(identity, getSightingsFor(identity.id)) }
}
