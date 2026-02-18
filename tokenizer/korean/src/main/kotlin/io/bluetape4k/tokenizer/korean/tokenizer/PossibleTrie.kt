package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * 형태소 분석에서 가능한 트라이 경로를 나타내는 데이터 클래스입니다.
 *
 * @property curTrie 현재 위치한 품사 트라이 노드
 * @property words 현재까지 분석된 단어 수
 *
 * @see KoreanPosTrie
 * @see CandidateParse
 */
data class PossibleTrie(
    val curTrie: KoreanPosTrie,
    val words: Int,
): Serializable
