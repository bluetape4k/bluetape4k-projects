package io.bluetape4k.tokenizer.korean.tokenizer

import java.io.Serializable

/**
 * 문장 분리 결과를 나타내는 데이터 클래스입니다.
 *
 * @property text 분리된 문장의 텍스트
 * @property start 원본 텍스트에서 문장의 시작 위치 (0-based index)
 * @property end 원본 텍스트에서 문장의 끝 위치 (exclusive)
 *
 * @see KoreanSentenceSplitter
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
