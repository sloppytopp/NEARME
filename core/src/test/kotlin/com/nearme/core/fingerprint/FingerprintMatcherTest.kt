package com.nearme.core.fingerprint

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FingerprintMatcherTest {

    private fun fp(
        uuids: Set<String> = emptySet(),
        mfgIds: Set<Int> = emptySet(),
        prefixes: Map<Int, List<Byte>> = emptyMap(),
        name: Int? = null,
        tx: Int? = null,
        interval: IntervalBucket? = null,
    ) = DeviceFingerprint(uuids, mfgIds, prefixes, name, tx, interval)

    @Test
    fun `identical fingerprints score 1`() {
        val a = fp(uuids = setOf("uuid-1"), mfgIds = setOf(76), tx = -55, interval = IntervalBucket.FAST)
        assertTrue(FingerprintMatcher.similarity(a, a) == 1.0)
    }

    @Test
    fun `same service uuids and manufacturer id but different tx power still scores high`() {
        val a = fp(uuids = setOf("uuid-1", "uuid-2"), mfgIds = setOf(76), interval = IntervalBucket.FAST, tx = -50)
        val b = fp(uuids = setOf("uuid-1", "uuid-2"), mfgIds = setOf(76), interval = IntervalBucket.FAST, tx = -58)
        assertTrue(FingerprintMatcher.similarity(a, b) > 0.75) { "expected high similarity for near-identical fingerprints" }
    }

    @Test
    fun `disjoint service uuids score low`() {
        val a = fp(uuids = setOf("uuid-1"), mfgIds = setOf(76))
        val b = fp(uuids = setOf("uuid-99"), mfgIds = setOf(6))
        assertTrue(FingerprintMatcher.similarity(a, b) < 0.3)
    }

    @Test
    fun `two fully empty fingerprints are not confidently matched`() {
        assertTrue(FingerprintMatcher.similarity(fp(), fp()) == 0.0)
    }
}
