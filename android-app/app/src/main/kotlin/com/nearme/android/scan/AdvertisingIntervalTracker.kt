package com.nearme.android.scan

/**
 * Estimates advertising cadence by tracking inter-packet arrival time per raw
 * MAC address. This is only meaningful within one MAC "session" — once a
 * device rotates its address, the interval resets — but that's fine, because
 * FingerprintExtractor only needs a rough cadence bucket, not a long-running
 * average.
 *
 * Entries are evicted after [staleAfterMs] of inactivity so a long scan
 * session doesn't grow this map unbounded.
 */
class AdvertisingIntervalTracker(private val staleAfterMs: Long = 60_000) {
    private data class Entry(var lastSeenMs: Long, var lastIntervalMs: Long?)

    private val lastSeenByMac = mutableMapOf<String, Entry>()

    @Synchronized
    fun onAdvertisement(macAddress: String, timestampMs: Long): Long? {
        val entry = lastSeenByMac[macAddress]
        val interval = if (entry != null && timestampMs - entry.lastSeenMs < staleAfterMs) {
            timestampMs - entry.lastSeenMs
        } else {
            null
        }
        lastSeenByMac[macAddress] = Entry(timestampMs, interval)
        evictStale(timestampMs)
        return interval
    }

    private fun evictStale(nowMs: Long) {
        lastSeenByMac.values.removeAll { nowMs - it.lastSeenMs > staleAfterMs }
    }
}
