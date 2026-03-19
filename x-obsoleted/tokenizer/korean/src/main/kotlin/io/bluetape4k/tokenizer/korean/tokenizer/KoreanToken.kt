package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import java.io.Serializable

/**
 * 형태소 분석 결과의 단일 토큰 정보를 표현합니다.
 *
 * ## 동작/계약
 * - `text`, `pos`, `offset`, `length`는 파싱 결과의 원문 구간 정보를 그대로 담는다.
 * - `stem`은 용언 원형이 계산된 경우에만 채워진다.
 * - `unknown=true`면 사전에 없는 미등록 명사 후보임을 의미한다.
 *
 * ```kotlin
 * val token = KoreanToken("사랑", KoreanPos.Noun, 0, 2)
 * // token.toString() == "사랑(Noun: 0, 2)"
 * ```
 *
 * @property text 토큰 문자열
 * @property pos 토큰 품사
 * @property offset 원문 시작 위치
 * @property length 토큰 길이
 * @property stem 용언 원형
 * @property unknown 미등록 후보 여부
 */
data class KoreanToken(
    val text: String,
    val pos: KoreanPos,
    val offset: Int,
    val length: Int,
    val stem: String? = null,
    val unknown: Boolean = false,
): Serializable {

    override fun toString(): String {
        val unknownStar = if (unknown) "*" else ""
        val stemString = if (stem != null) "($stem)" else ""
        return "$text$unknownStar($pos$stemString: $offset, $length)"
    }

    /**
     * 현재 토큰을 동일한 값으로 복사하되 품사만 [pos]로 교체합니다.
     *
     * ## 동작/계약
     * - `copy(pos = pos)`를 그대로 위임 호출한다.
     * - `text`, `offset`, `length`, `stem`, `unknown` 값은 유지된다.
     *
     * ```kotlin
     * val noun = KoreanToken("사랑", KoreanPos.Noun, 0, 2)
     * val verb = noun.copyWithNewPos(KoreanPos.Verb)
     * // verb.pos == KoreanPos.Verb
     * ```
     */
    fun copyWithNewPos(pos: KoreanPos): KoreanToken = copy(pos = pos)
}
