package io.bluetape4k.text.search

import java.io.Serializable

/**
 * [io.bluetape4k.text.search.AhoCorasickAutomaton.tokenize] 결과의 토큰.
 *
 * 입력 텍스트를 키워드 매치 구간([Match])과 비매치 구간([Fragment])으로 분리한다.
 *
 * @param V 키워드에 연관된 값 타입 (공변 `out V`)
 */
sealed interface SearchToken<out V> : Serializable {

    /**
     * 키워드 매치 구간.
     *
     * @param text 원본 텍스트에서 추출된 매치 문자열
     * @param match 매치 상세 정보 ([AhoCorasickMatch])
     */
    data class Match<out V>(
        val text: String,
        val match: AhoCorasickMatch<V>,
    ) : SearchToken<V> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /**
     * 비매치 구간 (키워드 사이의 일반 텍스트).
     *
     * @param text 원본 텍스트에서 추출된 비매치 문자열
     */
    data class Fragment(
        val text: String,
    ) : SearchToken<Nothing> {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}
