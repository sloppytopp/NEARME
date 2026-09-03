package com.nearme.core

import com.nearme.core.correlation.CorrelationEngine
import com.nearme.core.identity.IdentityResolver
import com.nearme.core.identity.TrackedIdentity
import com.nearme.core.model.BleAdvertisement
import com.nearme.core.verdict.VerdictEngine
import com.nearme.core.verdict.VerdictResult

data class ScanOutcome(
    val identity: TrackedIdentity,
    val verdict: VerdictResult,
)

/**
 * Single entry point wiring identity resolution -> correlation -> verdict for
 * one incoming advertisement. This is what a platform scan layer (Android's
 * BluetoothLeScanner, etc.) should call for every advertisement it reads.
 */
class ScanPipeline(
    private val identityResolver: IdentityResolver = IdentityResolver(),
) {
    fun ingest(advertisement: BleAdvertisement): ScanOutcome {
        val identity = identityResolver.resolve(advertisement)
        val correlation = CorrelationEngine.evaluate(identity)
        val verdict = VerdictEngine.evaluate(correlation)
        return ScanOutcome(identity, verdict)
    }

    fun allOutcomes(): List<ScanOutcome> =
        identityResolver.allIdentities().map { identity ->
            ScanOutcome(identity, VerdictEngine.evaluate(CorrelationEngine.evaluate(identity)))
        }

    fun pruneStale(nowMs: Long) = identityResolver.pruneStale(nowMs)
}
