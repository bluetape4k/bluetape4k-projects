package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ── LocalDateTime ─────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [LocalDateTime]
 * @param expected 비교 기준 [LocalDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDateTime.shouldBeAfter(expected: LocalDateTime): LocalDateTime {
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
 * @receiver 검증할 [LocalDateTime]
 * @param expected 비교 기준 [LocalDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDateTime.shouldBeBefore(expected: LocalDateTime): LocalDateTime {
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
 * @receiver 검증할 [LocalDateTime]
 * @param expected 비교 기준 [LocalDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDateTime.shouldBeOnOrAfter(expected: LocalDateTime): LocalDateTime {
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
 * @receiver 검증할 [LocalDateTime]
 * @param expected 비교 기준 [LocalDateTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDateTime.shouldBeOnOrBefore(expected: LocalDateTime): LocalDateTime {
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── LocalDate ─────────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [LocalDate]
 * @param expected 비교 기준 [LocalDate]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDate.shouldBeAfter(expected: LocalDate): LocalDate {
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
 * @receiver 검증할 [LocalDate]
 * @param expected 비교 기준 [LocalDate]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDate.shouldBeBefore(expected: LocalDate): LocalDate {
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
 * @receiver 검증할 [LocalDate]
 * @param expected 비교 기준 [LocalDate]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDate.shouldBeOnOrAfter(expected: LocalDate): LocalDate {
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
 * @receiver 검증할 [LocalDate]
 * @param expected 비교 기준 [LocalDate]
 * @return receiver (체이닝 지원)
 */
infix fun LocalDate.shouldBeOnOrBefore(expected: LocalDate): LocalDate {
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── LocalTime ─────────────────────────────────────────────────────────────────

/**
 * receiver가 [expected] 이후인지 검증한다.
 *
 * @receiver 검증할 [LocalTime]
 * @param expected 비교 기준 [LocalTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalTime.shouldBeAfter(expected: LocalTime): LocalTime {
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
 * @receiver 검증할 [LocalTime]
 * @param expected 비교 기준 [LocalTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalTime.shouldBeBefore(expected: LocalTime): LocalTime {
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
 * @receiver 검증할 [LocalTime]
 * @param expected 비교 기준 [LocalTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalTime.shouldBeOnOrAfter(expected: LocalTime): LocalTime {
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
 * @receiver 검증할 [LocalTime]
 * @param expected 비교 기준 [LocalTime]
 * @return receiver (체이닝 지원)
 */
infix fun LocalTime.shouldBeOnOrBefore(expected: LocalTime): LocalTime {
    if (this.isAfter(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("be on or before", expected, this),
            expected,
            this
        )
    }
    return this
}
