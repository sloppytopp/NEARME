package com.nearme.core.fingerprint

import com.nearme.core.model.BleAdvertisement

/**
 * Derives a [DeviceFingerprint] from a raw advertisement.
 *
 * The manufacturer payload prefix length is intentionally short: many chipsets
 * put a rolling counter or rotating token *later* in the payload (that's often
 * literally the anti-tracking mechanism), so hashing the whole payload would
 * defeat the fingerprint on every reading. Truncating to a short prefix keeps
 * whatever fixed protocol header/company-specific type byte is there while
 * dropping the volatile tail. This is a heuristic, not a guarantee.
 */
object FingerprintExtractor {
    private const val PAYLOAD_PREFIX_LEN = 4

    fun extract(advertisement: BleAdvertisement): DeviceFingerprint {
        val prefixes = advertisement.manufacturerData.mapValues { (_, payload) ->
            payload.take(PAYLOAD_PREFIX_LEN)
        }
        return DeviceFingerprint(
            serviceUuids = advertisement.serviceUuids.toSet(),
            manufacturerCompanyIds = advertisement.manufacturerData.keys.toSet(),
            manufacturerPayloadPrefixes = prefixes,
            nameHash = advertisement.deviceName?.takeIf { it.isNotBlank() }?.hashCode(),
            txPowerLevel = advertisement.txPowerLevel,
            advertisingIntervalBucket = IntervalBucket.fromMillis(advertisement.advertisingIntervalMs),
        )
    }
}
