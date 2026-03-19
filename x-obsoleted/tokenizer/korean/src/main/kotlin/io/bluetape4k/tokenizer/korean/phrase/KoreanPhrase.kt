package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import java.io.Serializable

/**
 * 추출된 한국어 phrase를 토큰 묶음으로 표현합니다.
 *
 * ## 동작/계약
 * - `text`는 `tokens`의 `text`를 순서대로 이어 붙여 계산한다.
 * - `offset`은 첫 토큰의 시작 위치이며 토큰이 비어 있으면 `-1`이다.
 * - `length`는 모든 토큰 텍스트 길이 합이다.
 *
 * ```kotlin
 * val phrase = KoreanPhrase(listOf(KoreanToken("동네", KoreanPos.Noun, 0, 2)))
 * // phrase.text == "동네"
 * ```
 *
 * @property tokens 구를 구성하는 토큰 리스트
 * @property pos 구의 대표 품사
 */
data class KoreanPhrase(
    val tokens: List<KoreanToken>,
    val pos: KoreanPos = KoreanPos.Noun,
): Serializable {

    /**
     * phrase의 시작 오프셋입니다.
     *
     * ## 동작/계약
     * - 토큰이 있으면 첫 토큰 `offset`을 반환하고, 없으면 `-1`을 반환한다.
     *
     * ```kotlin
     * val offset = KoreanPhrase(emptyList()).offset
     * // offset == -1
     * ```
     */
    val offset: Int by lazy {
        if (tokens.isNotEmpty()) this.tokens[0].offset else -1
    }

    /**
     * phrase 전체 텍스트입니다.
     *
     * ## 동작/계약
     * - 토큰 텍스트를 구분자 없이 순서대로 결합한다.
     *
     * ```kotlin
     * val text = KoreanPhrase(listOf(KoreanToken("사람", KoreanPos.Noun, 0, 2))).text
     * // text == "사람"
     * ```
     */
    val text: String by lazy {
        this.tokens.joinToString("") { it.text }
    }

    /**
     * phrase 텍스트 길이 합계입니다.
     *
     * ## 동작/계약
     * - 각 토큰의 `text.length`를 더해 계산한다.
     *
     * ```kotlin
     * val len = KoreanPhrase(listOf(KoreanToken("사람", KoreanPos.Noun, 0, 2))).length
     * // len == 2
     * ```
     */
    val length: Int by lazy {
        this.tokens.sumOf { it.text.length }
    }

    override fun toString(): String =
        "${this.text}($pos: ${this.offset}, ${this.length})"
}
