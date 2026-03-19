package io.bluetape4k.tokenizer.korean.tokenizer

import java.io.Serializable

/**
 * 문장 분리 결과의 단일 문장과 위치 정보를 담습니다.
 *
 * ## 동작/계약
 * - `text`는 분리된 문장 문자열이다.
 * - `start`는 원문 시작 인덱스(0-based), `end`는 원문 끝 인덱스(exclusive)다.
 *
 * ```kotlin
 * val sentence = Sentence("안녕?", 0, 3)
 * // sentence.toString() == "안녕?(0,3)"
 * ```
 *
 * @property text 문장 텍스트
 * @property start 원문 시작 위치
 * @property end 원문 끝 위치(exclusive)
 */
data class Sentence(
    val text: String,
    val start: Int,
    val end: Int,
): Serializable {
    /**
     * 문장의 문자열 표현을 반환합니다.
     * 형식: "텍스트(시작,끝)"
     */
    override fun toString(): String = "$text($start,$end)"
}
