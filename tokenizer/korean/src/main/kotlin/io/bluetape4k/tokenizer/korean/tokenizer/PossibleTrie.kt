package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * 현재 분석 지점에서 확장 가능한 품사 트라이 후보입니다.
 *
 * ## 동작/계약
 * - `curTrie`는 다음 토큰 매칭에 사용할 트라이 노드다.
 * - `words`는 이 후보를 선택했을 때 누적 단어 수 가중치다.
 *
 * ```kotlin
 * val candidate = PossibleTrie(KoreanPosTrie(null), 0)
 * // candidate.words == 0
 * ```
 *
 * @property curTrie 현재 트라이 노드
 * @property words 누적 단어 수
 */
data class PossibleTrie(
    val curTrie: KoreanPosTrie,
    val words: Int,
): Serializable
