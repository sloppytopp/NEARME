package com.nearme.core.verdict

import com.nearme.core.correlation.CorrelationResult
import java.util.concurrent.TimeUnit

/** Translates a [CorrelationResult] into a plain-language verdict a non-technical user can act on. */
object VerdictEngine {
    private const val SUSPICIOUS_THRESHOLD = 0.35

    fun evaluate(result: CorrelationResult): VerdictResult {
        val verdict = when {
            result.distinctLocations.size <= 1 -> Verdict.NORMAL
            result.followingScore >= SUSPICIOUS_THRESHOLD -> Verdict.SUSPICIOUS
            else -> Verdict.WORTH_NOTING
        }
        return VerdictResult(verdict, explain(verdict, result))
    }

    private fun explain(verdict: Verdict, result: CorrelationResult): String {
        val locationCount = result.distinctLocations.size
        val span = formatSpan(result.timeSpanMs)
        return when (verdict) {
            Verdict.NORMAL ->
                "Seen only at one place (${result.totalSightings} time(s)). This is most likely " +
                    "something that belongs there — a neighbor's device, a smart-home gadget, or your own hardware."
            Verdict.WORTH_NOTING ->
                "Appeared at $locationCount different places you've been, spread over $span. " +
                    "Could be coincidence (e.g. a device someone else is carrying on a similar routine), but worth keeping an eye on."
            Verdict.SUSPICIOUS ->
                "Followed you to $locationCount different places over just $span. That pattern is unusual " +
                    "for a stationary device and is worth investigating further."
        }
    }

    private fun formatSpan(spanMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(spanMs)
        return when {
            hours < 1 -> "under an hour"
            hours < 24 -> "$hours hour(s)"
            else -> "${TimeUnit.MILLISECONDS.toDays(spanMs)} day(s)"
        }
    }
}
