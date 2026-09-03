package com.nearme.core.model

/**
 * One raw BLE advertisement observation, as read straight off the radio.
 * macAddress is recorded but never trusted as a long-lived identity — modern
 * devices rotate it specifically to defeat that, which is the whole reason
 * FingerprintExtractor exists.
 */
data class BleAdvertisement(
    val macAddress: String,
    val timestampMs: Long,
    val rssi: Int,
    val txPowerLevel: Int?,
    val deviceName: String?,
    val serviceUuids: List<String>,
    val manufacturerData: Map<Int, ByteArray>,
    val advertisingIntervalMs: Long?,
    val locationBucket: LocationBucket,
)
