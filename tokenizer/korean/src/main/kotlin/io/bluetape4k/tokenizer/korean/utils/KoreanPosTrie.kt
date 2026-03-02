package io.bluetape4k.tokenizer.korean.utils

import java.io.Serializable

/**
 * 품사 시퀀스 규칙을 표현하는 트라이 노드입니다.
 *
 * ## 동작/계약
 * - `curPos`는 현재 노드에서 소비할 품사다.
 * - `nextTrie`는 다음 품사 후보 노드 목록이며 null이면 더 이상 전이하지 않는다.
 * - `ending`이 null이 아니면 해당 지점에서 규칙 종결 품사가 확정된다.
 *
 * ```kotlin
 * val node = KoreanPosTrie(KoreanPos.Noun, ending = KoreanPos.Noun)
 * // node.curPos == KoreanPos.Noun
 * ```
 *
 * @property curPos 현재 품사
 * @property nextTrie 다음 트라이 후보
 * @property ending 종결 품사
 */
data class KoreanPosTrie(
    val curPos: KoreanPos?,
    val nextTrie: List<KoreanPosTrie>? = null,
    val ending: KoreanPos? = null,
): Serializable
