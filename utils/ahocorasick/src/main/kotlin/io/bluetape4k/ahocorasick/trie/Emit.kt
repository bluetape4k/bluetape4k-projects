package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.ahocorasick.interval.Interval

/**
 * Aho-Corasick 알고리즘의 키워드를 가지는 Emit을 나타내는 데이터 클래스.
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
