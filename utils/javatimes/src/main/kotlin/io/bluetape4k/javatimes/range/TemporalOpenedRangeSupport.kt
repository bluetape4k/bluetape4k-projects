package io.bluetape4k.javatimes.range

import java.time.temporal.Temporal

/**
 * [TemporalOpenedRange] 를 생성합니다.
 *
 * `start until end` 문법으로 반개방 범위(start 포함, end 제외)를 생성합니다.
 *
 * ```kotlin
 * val start = LocalDateTime.of(2024, 1, 1, 0, 0)
 * val end = LocalDateTime.of(2024, 1, 10, 0, 0)
 * val range = start until end
 * range.contains(LocalDateTime.of(2024, 1, 1, 0, 0)) // true
 * range.contains(LocalDateTime.of(2024, 1, 10, 0, 0)) // false (exclusive)
 * ```
 */
infix fun <T> T.until(other: T): TemporalOpenedRange<T> where T: Temporal, T: Comparable<T> =
    TemporalOpenedRange(this, other)
