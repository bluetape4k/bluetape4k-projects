package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

/**
 * phrase 접기 과정의 중간 상태를 저장하는 버퍼입니다.
 *
 * ## 동작/계약
 * - `phrases`는 현재까지 생성된 phrase 시퀀스를 유지한다.
 * - `curTrie`는 다음 토큰에서 가능한 품사 전이 상태를 보관한다.
 * - `ending`은 현재 시점에서 확정 가능한 끝 품사를 기록한다.
 *
 * ```kotlin
 * val buffer = PhraseBuffer(emptyList(), emptyList(), null)
 * // buffer.ending == null
 * ```
 *
 * @property phrases 현재까지 누적된 phrase 목록
 * @property curTrie 현재 트라이 상태 목록
 * @property ending 현재 종결 품사 후보
 */
data class PhraseBuffer(
    val phrases: List<KoreanPhrase>,
    val curTrie: List<KoreanPosTrie?>,
    val ending: KoreanPos?,
): Serializable
