package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging

/**
 * 한글 문장을 문장 단위로 분리하는 유틸리티 클래스입니다.
 */
object KoreanSentenceSplitter: KLogging() {

    private val re: Regex =
        """
        |(?x)[^.!?…\s]   # First char is non-punct, non-ws
        |[^.!?…]*        # Greedily consume up to punctuation.
        |(?:             # Group for unrolling the loop.
        |[.!?…]          # (special) inner punctuation ok if
        |(?!['\"]?\s|$)  # not followed by ws or EOS.
        |[^.!?…]*        # Greedily consume up to punctuation.
        |)*              # Zero or more (special normal*)
        |[.!?…]?         # Optional ending punctuation.
        |['\"]?          # Optional closing quote.
        |(?=\s|$)
        |"""
            .trimMargin()
            .toRegex()

    /**
     * 한글 문장을 문장(Sentence) 단위로 분리합니다.
     *
     * ```
     * var actual = split("안녕? iphone6안녕? 세상아?").toList()
     * actual shouldContainSame listOf(
     *     Sentence("안녕?", 0, 3),
     *     Sentence("iphone6안녕?", 4, 14),
     *     Sentence("세상아?", 15, 19)
     * )
     *```
     */
    fun split(text: CharSequence): List<Sentence> {
        if (text.isEmpty()) {
            return emptyList()
        }

        return re
            .findAll(text)
            .map { mr ->
                Sentence(mr.groupValues[0], mr.range.first, mr.range.last + 1)
            }
            .toList()
    }

}
