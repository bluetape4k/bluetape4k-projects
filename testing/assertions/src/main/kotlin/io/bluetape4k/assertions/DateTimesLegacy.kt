package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import java.util.Date

// ── java.util.Date ────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [Date]
 * @param expected 비교 기준 [Date]
 * @return receiver (체이닝 지원)
 */
infix fun Date.shouldBeAfter(expected: Date): Date {
    if (!this.after(expected)) {
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
 * @receiver 검증할 [Date]
 * @param expected 비교 기준 [Date]
 * @return receiver (체이닝 지원)
 */
infix fun Date.shouldBeBefore(expected: Date): Date {
    if (!this.before(expected)) {
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
 * 밀리초 단위 비교: `this.time >= expected.time`
 *
 * @receiver 검증할 [Date]
 * @param expected 비교 기준 [Date]
 * @return receiver (체이닝 지원)
 */
infix fun Date.shouldBeOnOrAfter(expected: Date): Date {
    if (this.before(expected)) {
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
 * 밀리초 단위 비교: `this.time <= expected.time`
 *
 * @receiver 검증할 [Date]
 * @param expected 비교 기준 [Date]
 * @return receiver (체이닝 지원)
 */
infix fun Date.shouldBeOnOrBefore(expected: Date): Date {
    if (this.after(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}
