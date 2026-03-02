package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * 동적 계획법 기반 형태소 분석의 후보 상태를 표현합니다.
 *
 * ## 동작/계약
 * - `parse`는 현재까지 누적 점수를 포함한 분석 결과를 담는다.
 * - `curTrie`는 다음 토큰 확장에서 사용할 품사 전이 노드 목록이다.
 * - `ending`이 null이 아니면 어미 종결 상태에서의 추가 전이를 허용한다.
 *
 * ```kotlin
 * val candidate = CandidateParse(ParsedChunk(emptyList(), 0), emptyList(), null)
 * // candidate.ending == null
 * ```
 *
 * @property parse 현재까지 누적된 파싱 결과
 * @property curTrie 현재 트라이 상태 목록
 * @property ending 현재 종결 품사
 */
data class CandidateParse(
    val parse: ParsedChunk,
    val curTrie: List<KoreanPosTrie>,
    val ending: KoreanPos? = null,
): Serializable
