package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import java.io.Serializable

/**
 * 한글 형태소 분석의 결과인 한글 토큰
 *
 * @property text 토큰의 텍스트
 * @property pos 토큰의 품사
 * @property offset 토큰의 시작 위치
 * @property length 토큰의 길이
 * @property stem 토큰의 어간 (원형)
 * @property unknown 알 수 없는 토큰인지 여부
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
     * 새로운 품사로 복사본을 생성합니다.
     */
    fun copyWithNewPos(pos: KoreanPos): KoreanToken = copy(pos = pos)
}
