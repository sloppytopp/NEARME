package com.nearme.core.pipeline

import com.nearme.core.ScanPipeline
import com.nearme.core.model.BleAdvertisement
import com.nearme.core.model.LocationBucket
import com.nearme.core.verdict.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * End-to-end scenarios mirroring the two motivating cases from the design doc:
 * a tracker that follows the user across places (should end up SUSPICIOUS even
 * though its MAC rotates on every sighting) versus a neighbor's smart-home
 * device that only ever shows up at one place (should stay NORMAL).
 */
class ScanPipelineTest {

    private fun advertisement(
        mac: String,
        timestampMs: Long,
        location: LocationBucket,
    ) = BleAdvertisement(
        macAddress = mac,
        timestampMs = timestampMs,
        rssi = -55,
        txPowerLevel = -55,
        deviceName = null,
        serviceUuids = listOf("0000fe9f-0000-1000-8000-00805f9b34fb"),
        manufacturerData = mapOf(76 to byteArrayOf(0x10, 0x05, 0x01, 0x02, 0x99.toByte())),
        advertisingIntervalMs = 120,
        locationBucket = location,
    )

    @Test
    fun `tracker following user across three places over two hours is flagged suspicious`() {
        val pipeline = ScanPipeline()
        val hour = 60 * 60 * 1000L

        pipeline.ingest(advertisement(mac = "AA:00:00:00:00:01", timestampMs = 0, location = LocationBucket("home")))
        pipeline.ingest(advertisement(mac = "AA:00:00:00:00:02", timestampMs = hour, location = LocationBucket("coffee-shop")))
        val outcome = pipeline.ingest(
            advertisement(mac = "AA:00:00:00:00:03", timestampMs = 2 * hour, location = LocationBucket("grocery-store")),
        )

        assertEquals(Verdict.SUSPICIOUS, outcome.verdict.verdict)
        assertEquals(3, outcome.identity.sightings.size)
    }

    @Test
    fun `neighbor smart speaker seen only at home stays normal`() {
        val pipeline = ScanPipeline()
        val home = LocationBucket("home")
        val day = 24 * 60 * 60 * 1000L

        repeat(20) { i ->
            pipeline.ingest(advertisement(mac = "BB:00:00:00:00:0${i % 10}", timestampMs = i * day, location = home))
        }
        val outcome = pipeline.allOutcomes().single()

        assertEquals(Verdict.NORMAL, outcome.verdict.verdict)
        assertEquals(20, outcome.identity.sightings.size)
    }
}
