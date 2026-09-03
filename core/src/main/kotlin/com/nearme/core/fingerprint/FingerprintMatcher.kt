package com.nearme.core.fingerprint

/**
 * Scores how likely two [DeviceFingerprint]s came from the same physical
 * device, on a 0.0-1.0 scale. This is the core answer to "the MAC rotated,
 * is it still the same tracker?" — since no single field is reliably present,
 * each field is scored independently and only fields where at least one
 * fingerprint actually has data ("informative" fields) count toward the
 * result. Two fingerprints with no informative overlap at all score 0.0
 * (can't tell -> don't claim a match) rather than defaulting to a false
 * high-confidence merge of unrelated anonymous devices.
 */
object FingerprintMatcher {
    private const val WEIGHT_SERVICE_UUIDS = 0.35
    private const val WEIGHT_MANUFACTURER_IDS = 0.15
    private const val WEIGHT_MANUFACTURER_PREFIX = 0.15
    private const val WEIGHT_INTERVAL = 0.10
    private const val WEIGHT_TX_POWER = 0.10
    private const val WEIGHT_NAME_HASH = 0.15

    fun similarity(a: DeviceFingerprint, b: DeviceFingerprint): Double {
        var weightedSum = 0.0
        var weightTotal = 0.0

        fun add(weight: Double, informative: Boolean, score: Double) {
            if (!informative) return
            weightedSum += weight * score
            weightTotal += weight
        }

        add(
            WEIGHT_SERVICE_UUIDS,
            informative = a.serviceUuids.isNotEmpty() || b.serviceUuids.isNotEmpty(),
            score = jaccard(a.serviceUuids, b.serviceUuids),
        )
        add(
            WEIGHT_MANUFACTURER_IDS,
            informative = a.manufacturerCompanyIds.isNotEmpty() || b.manufacturerCompanyIds.isNotEmpty(),
            score = jaccard(a.manufacturerCompanyIds, b.manufacturerCompanyIds),
        )

        val commonCompanyIds = a.manufacturerPayloadPrefixes.keys intersect b.manufacturerPayloadPrefixes.keys
        add(
            WEIGHT_MANUFACTURER_PREFIX,
            informative = commonCompanyIds.isNotEmpty(),
            score = if (commonCompanyIds.isEmpty()) {
                0.0
            } else {
                commonCompanyIds.count { id ->
                    a.manufacturerPayloadPrefixes[id] == b.manufacturerPayloadPrefixes[id]
                }.toDouble() / commonCompanyIds.size
            },
        )

        add(
            WEIGHT_INTERVAL,
            informative = a.advertisingIntervalBucket != null && b.advertisingIntervalBucket != null,
            score = if (a.advertisingIntervalBucket == b.advertisingIntervalBucket) 1.0 else 0.0,
        )

        add(
            WEIGHT_TX_POWER,
            informative = a.txPowerLevel != null && b.txPowerLevel != null,
            score = if (a.txPowerLevel != null && b.txPowerLevel != null) {
                val diff = kotlin.math.abs(a.txPowerLevel - b.txPowerLevel)
                (1.0 - (diff / 10.0)).coerceIn(0.0, 1.0)
            } else {
                0.0
            },
        )

        add(
            WEIGHT_NAME_HASH,
            informative = a.nameHash != null && b.nameHash != null,
            score = if (a.nameHash == b.nameHash) 1.0 else 0.0,
        )

        if (weightTotal == 0.0) return 0.0
        return weightedSum / weightTotal
    }

    private fun <T> jaccard(a: Set<T>, b: Set<T>): Double {
        if (a.isEmpty() && b.isEmpty()) return 0.0
        val union = a union b
        if (union.isEmpty()) return 0.0
        val intersection = a intersect b
        return intersection.size.toDouble() / union.size
    }
}
