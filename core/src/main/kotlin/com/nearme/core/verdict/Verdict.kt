package com.nearme.core.verdict

enum class Verdict {
    /** Stationary device — only ever seen at one place. */
    NORMAL,

    /** Seen at more than one place, but weakly enough to plausibly be coincidence. */
    WORTH_NOTING,

    /** Seen across multiple distinct places on a schedule consistent with following the user. */
    SUSPICIOUS,
}

data class VerdictResult(
    val verdict: Verdict,
    val explanation: String,
)
