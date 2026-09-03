package com.nearme.core.identity

import com.nearme.core.model.BleAdvertisement
import com.nearme.core.model.LocationBucket
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class IdentityResolverTest {

    private fun advertisement(
        mac: String,
        timestampMs: Long,
        location: LocationBucket,
        uuids: List<String> = listOf("uuid-shared"),
        mfgId: Int = 76,
    ) = BleAdvertisement(
        macAddress = mac,
        timestampMs = timestampMs,
        rssi = -60,
        txPowerLevel = -55,
        deviceName = null,
        serviceUuids = uuids,
        manufacturerData = mapOf(mfgId to byteArrayOf(0x01, 0x02, 0x03, 0x04)),
        advertisingIntervalMs = 150,
        locationBucket = location,
    )

    @Test
    fun `same fingerprint under rotating MAC resolves to the same identity`() {
        val resolver = IdentityResolver()
        val home = LocationBucket("home")

        val first = resolver.resolve(advertisement(mac = "AA:AA:AA:AA:AA:01", timestampMs = 0, location = home))
        val second = resolver.resolve(advertisement(mac = "BB:BB:BB:BB:BB:02", timestampMs = 60_000, location = home))

        assertEquals(first.id, second.id)
        assertEquals(2, resolver.allIdentities().single().sightings.size)
    }

    @Test
    fun `unrelated fingerprints get distinct identities`() {
        val resolver = IdentityResolver()
        val home = LocationBucket("home")

        val first = resolver.resolve(advertisement(mac = "AA:AA:AA:AA:AA:01", timestampMs = 0, location = home, uuids = listOf("uuid-a"), mfgId = 76))
        val second = resolver.resolve(advertisement(mac = "BB:BB:BB:BB:BB:02", timestampMs = 1_000, location = home, uuids = listOf("uuid-z"), mfgId = 6))

        assertNotEquals(first.id, second.id)
        assertEquals(2, resolver.allIdentities().size)
    }

    @Test
    fun `restored identities are matched against new sightings`() {
        val home = LocationBucket("home")
        val original = IdentityResolver()
        val first = original.resolve(advertisement(mac = "AA:AA:AA:AA:AA:01", timestampMs = 0, location = home))

        val restored = IdentityResolver()
        restored.restore(original.allIdentities())
        val second = restored.resolve(advertisement(mac = "CC:CC:CC:CC:CC:03", timestampMs = 5_000, location = home))

        assertEquals(first.id, second.id)
    }

    @Test
    fun `identities beyond staleness window are not matched`() {
        val resolver = IdentityResolver(stalenessWindowMs = 1_000)
        val home = LocationBucket("home")

        val first = resolver.resolve(advertisement(mac = "AA:AA:AA:AA:AA:01", timestampMs = 0, location = home))
        val second = resolver.resolve(advertisement(mac = "BB:BB:BB:BB:BB:02", timestampMs = 10_000, location = home))

        assertNotEquals(first.id, second.id)
    }
}
