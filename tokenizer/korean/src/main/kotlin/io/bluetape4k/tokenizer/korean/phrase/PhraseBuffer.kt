package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * 한국어 구(phrase)를 나타내는 데이터 클래스입니다.
 *
 * @property phrases 구를 구성하는 한국어 구(phrase) 리스트
 * @property curTrie [KoreanPosTrie] 리스트
 * @property ending 구(phrase)의 끝 품사
 */
data class PhraseBuffer(
    val phrases: List<KoreanPhrase>,
    val curTrie: List<KoreanPosTrie?>,
    val ending: KoreanPos?,
): Serializable
