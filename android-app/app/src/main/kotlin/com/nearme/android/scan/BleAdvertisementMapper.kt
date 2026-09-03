package com.nearme.android.scan

import android.bluetooth.le.ScanResult
import com.nearme.android.data.LocationBucketProvider
import com.nearme.core.model.BleAdvertisement

/** Converts a platform [ScanResult] into the platform-agnostic core model. */
class BleAdvertisementMapper(
    private val locationBucketProvider: LocationBucketProvider,
    private val intervalTracker: AdvertisingIntervalTracker,
) {
    fun map(result: ScanResult): BleAdvertisement {
        val record = result.scanRecord
        val nowMs = System.currentTimeMillis()
        val manufacturerData = buildMap {
            record?.manufacturerSpecificData?.let { sparse ->
                for (i in 0 until sparse.size()) {
                    put(sparse.keyAt(i), sparse.valueAt(i) ?: ByteArray(0))
                }
            }
        }

        return BleAdvertisement(
            macAddress = result.device.address,
            timestampMs = nowMs,
            rssi = result.rssi,
            txPowerLevel = record?.txPowerLevel?.takeIf { it != Int.MIN_VALUE },
            deviceName = record?.deviceName,
            serviceUuids = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList(),
            manufacturerData = manufacturerData,
            advertisingIntervalMs = intervalTracker.onAdvertisement(result.device.address, nowMs),
            locationBucket = locationBucketProvider.currentLocationBucket(),
        )
    }
}
