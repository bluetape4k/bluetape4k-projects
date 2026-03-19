package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.ahocorasick.interval.Interval

/**
 * Aho-Corasick 알고리즘의 키워드를 가지는 Emit을 나타내는 데이터 클래스.
 *
 * ## 동작/계약
 * - [start], [end] 구간과 매칭 [keyword]를 함께 보관합니다.
 * - [Interval]을 상속하므로 구간 비교/정렬 계약을 따릅니다.
 *
 * ```kotlin
 * val emit = Emit(2, 5, "hers")
 * // emit.size == 4
 * ```
 *
 * @property start Emit의 시작 위치
 * @property end Emit의 끝 위치
 * @property keyword Emit의 키워드 (키워드가 없으면 null)
 */
class Emit(
    override val start: Int,
    override val end: Int,
    val keyword: String? = null,
): Interval(start, end) {

    override fun toString(): String = super.toString() + "=${keyword ?: "<null>"}"

}
