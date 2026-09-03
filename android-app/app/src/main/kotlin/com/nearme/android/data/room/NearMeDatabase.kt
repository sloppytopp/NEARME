package com.nearme.android.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TrackedIdentityEntity::class, SightingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NearMeDatabase : RoomDatabase() {
    abstract fun identityDao(): IdentityDao

    companion object {
        @Volatile private var instance: NearMeDatabase? = null

        fun get(context: Context): NearMeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NearMeDatabase::class.java,
                    "nearme.db",
                ).build().also { instance = it }
            }
    }
}
