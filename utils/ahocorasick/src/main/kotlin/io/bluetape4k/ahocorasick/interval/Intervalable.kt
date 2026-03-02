package io.bluetape4k.ahocorasick.interval

import io.bluetape4k.ValueObject

/**
 * 시작/끝 인덱스를 가지는 구간 객체 계약입니다.
 *
 * ## 동작/계약
 * - [start], [end]는 양 끝 포함(inclusive) 인덱스입니다.
 * - [size]는 `end - start + 1`로 계산됩니다.
 *
 * ```kotlin
 * val i: Intervalable = Interval(2, 4)
 * // i.size == 3
 * ```
 */
interface Intervalable: Comparable<Intervalable>, ValueObject {

    val start: Int
    val end: Int

    val size: Int get() = end - start + 1
}
