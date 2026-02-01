package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import java.io.Serializable

/**
 * 한국어 구(phrase)를 나타내는 데이터 클래스입니다.
 *
 * @property tokens 구를 구성하는 토큰 리스트
 * @property pos 구의 품사
 */
data class KoreanPhrase(
    val tokens: List<KoreanToken>,
    val pos: KoreanPos = KoreanPos.Noun,
): Serializable {

    val offset: Int by lazy {
        if (tokens.isNotEmpty()) this.tokens[0].offset else -1
    }

    val text: String by lazy {
        this.tokens.joinToString("") { it.text }
    }

    val length: Int by lazy {
        this.tokens.sumOf { it.text.length }
    }

    override fun toString(): String =
        "${this.text}($pos: ${this.offset}, ${this.length})"
}
