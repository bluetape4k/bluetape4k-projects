package io.bluetape4k.javatimes.range

import java.time.temporal.Temporal

/**
 * [TemporalOpenedRange] 를 생성합니다.
 */
infix fun <T> T.until(other: T): TemporalOpenedRange<T> where T: Temporal, T: Comparable<T> =
    TemporalOpenedRange(this, other)
