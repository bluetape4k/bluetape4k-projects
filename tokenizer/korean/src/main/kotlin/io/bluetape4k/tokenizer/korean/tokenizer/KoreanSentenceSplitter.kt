package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging

/**
 * 입력 문자열을 문장 경계 정규식으로 분리합니다.
 *
 * ## 동작/계약
 * - 구두점(`.!?…`)과 공백/문자열 끝 조건으로 문장 경계를 계산한다.
 * - 빈 입력이면 `emptySequence()`를 반환한다.
 * - 반환 `Sentence.end`는 `mr.range.last + 1`로 계산되는 exclusive 인덱스다.
 *
 * ```kotlin
 * val sentences = KoreanSentenceSplitter.split("안녕? 세상아?").toList()
 * // sentences.map { it.text } == ["안녕?", "세상아?"]
 * ```
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
            |['"]?          # Optional closing quote.
            |(?=\s|$)
            |"""
            .trimMargin()
            .toRegex()

    /**
     * 문자열을 문장 단위 `Sequence<Sentence>`로 반환합니다.
     *
     * ## 동작/계약
     * - 정규식 `findAll` 결과를 순회해 각 매치를 `Sentence(text, start, endExclusive)`로 변환한다.
     * - `KoreanSentenceSplitterTest` 기준으로 `"안녕? iphone6안녕? 세상아?"`는 3문장으로 분리된다.
     *
     * ```kotlin
     * val list = KoreanSentenceSplitter.split("안녕? iphone6안녕? 세상아?").toList()
     * // list.size == 3
     * ```
     */
    fun split(text: CharSequence): Sequence<Sentence> {
        if (text.isEmpty()) {
            return emptySequence()
        }

        return re
            .findAll(text)
            .map { mr ->
                Sentence(mr.groupValues[0], mr.range.first, mr.range.last + 1)
            }
    }
}
