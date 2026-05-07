package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime

// ── Instant ──────────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [Instant]
 * @param expected 비교 기준 [Instant]
 * @return receiver (체이닝 지원)
 */
infix fun Instant.shouldBeAfter(expected: Instant): Instant {
    if (!this.isAfter(expected)) {
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
    if (!this.isBefore(expected)) {
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
    if (this.isBefore(expected)) {
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
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── ZonedDateTime ─────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [ZonedDateTime]
 * @param expected 비교 기준 [ZonedDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun ZonedDateTime.shouldBeAfter(expected: ZonedDateTime): ZonedDateTime {
    if (!this.isAfter(expected)) {
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
 * @receiver 검증할 [ZonedDateTime]
 * @param expected 비교 기준 [ZonedDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun ZonedDateTime.shouldBeBefore(expected: ZonedDateTime): ZonedDateTime {
    if (!this.isBefore(expected)) {
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
 * @receiver 검증할 [ZonedDateTime]
 * @param expected 비교 기준 [ZonedDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun ZonedDateTime.shouldBeOnOrAfter(expected: ZonedDateTime): ZonedDateTime {
    if (this.isBefore(expected)) {
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
 * @receiver 검증할 [ZonedDateTime]
 * @param expected 비교 기준 [ZonedDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun ZonedDateTime.shouldBeOnOrBefore(expected: ZonedDateTime): ZonedDateTime {
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── OffsetDateTime ────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [OffsetDateTime]
 * @param expected 비교 기준 [OffsetDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun OffsetDateTime.shouldBeAfter(expected: OffsetDateTime): OffsetDateTime {
    if (!this.isAfter(expected)) {
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
 * @receiver 검증할 [OffsetDateTime]
 * @param expected 비교 기준 [OffsetDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun OffsetDateTime.shouldBeBefore(expected: OffsetDateTime): OffsetDateTime {
    if (!this.isBefore(expected)) {
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
 * @receiver 검증할 [OffsetDateTime]
 * @param expected 비교 기준 [OffsetDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun OffsetDateTime.shouldBeOnOrAfter(expected: OffsetDateTime): OffsetDateTime {
    if (this.isBefore(expected)) {
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
 * @receiver 검증할 [OffsetDateTime]
 * @param expected 비교 기준 [OffsetDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun OffsetDateTime.shouldBeOnOrBefore(expected: OffsetDateTime): OffsetDateTime {
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}
