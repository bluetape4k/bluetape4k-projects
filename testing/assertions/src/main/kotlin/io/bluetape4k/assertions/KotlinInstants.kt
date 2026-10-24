package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import kotlin.time.Instant

// ── Instant ──────────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [Instant]
 * @param expected 비교 기준 [Instant]
 * @return receiver (체이닝 지원)
 */
infix fun Instant.shouldBeAfter(expected: Instant): Instant {
    if (this <= expected) {
        Failures.failComparison(
            Messages.expectedToBe("be after", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected] 이전인지 검증한다.
 *
 * @receiver 검증할 [Instant]
 * @param expected 비교 기준 [Instant]
 * @return receiver (체이닝 지원)
 */
infix fun Instant.shouldBeBefore(expected: Instant): Instant {
    if (this >= expected) {
        Failures.failComparison(
            Messages.expectedToBe("be before", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected] 이후이거나 같은지 검증한다.
 *
 * @receiver 검증할 [Instant]
 * @param expected 비교 기준 [Instant]
 * @return receiver (체이닝 지원)
 */
infix fun Instant.shouldBeOnOrAfter(expected: Instant): Instant {
    if (this < expected) {
        Failures.failComparison(
            Messages.expectedToBe("be on or after", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected] 이전이거나 같은지 검증한다.
 *
 * @receiver 검증할 [Instant]
 * @param expected 비교 기준 [Instant]
 * @return receiver (체이닝 지원)
 */
infix fun Instant.shouldBeOnOrBefore(expected: Instant): Instant {
    if (this > expected) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}
