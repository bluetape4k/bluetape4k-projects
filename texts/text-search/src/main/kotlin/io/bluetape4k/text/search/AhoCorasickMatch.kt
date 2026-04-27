package io.bluetape4k.text.search

import java.io.Serializable

/**
 * Aho-Corasick 검색 결과 — 텍스트 내 키워드 매치 정보.
 *
 * **주의**: `kotlin.text.MatchResult`와 이름 충돌을 피하기 위해 `AhoCorasickMatch`로 명명됨.
 *
 * @param V 키워드에 연관된 값 타입 (공변 `out V`)
 * @param start 매치 시작 위치 (원본 텍스트 기준, inclusive)
 * @param end 매치 종료 위치 (원본 텍스트 기준, inclusive)
 * @param keyword 매치된 키워드 (정규화가 적용된 경우 정규화된 키워드)
 * @param value 키워드에 연관된 값
 */
data class AhoCorasickMatch<out V>(
    val start: Int,
    val end: Int,
    val keyword: String,
    val value: V,
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    /** 매치 길이 (end - start + 1) */
    val length: Int get() = end - start + 1
}
