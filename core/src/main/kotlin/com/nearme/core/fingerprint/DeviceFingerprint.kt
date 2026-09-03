package com.nearme.core.fingerprint

/**
 * Quantized advertising cadence. Raw interval readings are noisy (OS scan
 * coalescing, radio jitter), so we bucket them instead of comparing exact
 * milliseconds — two readings of "fast" from the same physical device should
 * match even if one measured 118ms and the other 142ms.
 */
enum class IntervalBucket {
    VERY_FAST, // < 100ms  (e.g. Apple Find My style rapid advertising)
    FAST, // 100-250ms
    MEDIUM, // 250-600ms
    SLOW, // 600-1200ms
    VERY_SLOW, // > 1200ms
    ;

    companion object {
        fun fromMillis(intervalMs: Long?): IntervalBucket? {
            if (intervalMs == null) return null
            return when {
                intervalMs < 100 -> VERY_FAST
                intervalMs < 250 -> FAST
                intervalMs < 600 -> MEDIUM
                intervalMs < 1200 -> SLOW
                else -> VERY_SLOW
            }
        }
    }
}

/**
 * A rotating-MAC-resistant identity signature for a BLE device, built entirely
 * from the parts of an advertisement that a MAC-randomizing device still has to
 * expose to be useful (service UUIDs, manufacturer data, tx power, cadence).
 *
 * This is deliberately NOT a single hash: no individual field is reliably present
 * or reliably stable across every device, so FingerprintMatcher scores partial
 * overlap instead of requiring an exact match.
 */
data class DeviceFingerprint(
    val serviceUuids: Set<String>,
    val manufacturerCompanyIds: Set<Int>,
    val manufacturerPayloadPrefixes: Map<Int, List<Byte>>,
    val nameHash: Int?,
    val txPowerLevel: Int?,
    val advertisingIntervalBucket: IntervalBucket?,
)
