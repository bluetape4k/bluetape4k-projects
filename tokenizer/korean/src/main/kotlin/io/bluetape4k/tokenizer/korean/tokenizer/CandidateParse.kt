package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * 형태소 분석의 후보 해석을 나타내는 데이터 클래스입니다.
 *
 * Dynamic Programming을 사용한 형태소 분석 과정에서 중간 상태를 저장하는 데 사용됩니다.
 *
 * @property parse 현재까지 분석된 [ParsedChunk]
 * @property curTrie 현재 위치한 품사 트라이 노드 목록
 * @property ending 현재 위치가 어미(ending)인 경우의 품사, 아니면 null
 *
 * @see KoreanTokenizer
 * @see KoreanPosTrie
 */
data class CandidateParse(
    val parse: ParsedChunk,
    val curTrie: List<KoreanPosTrie>,
    val ending: KoreanPos? = null,
): Serializable
