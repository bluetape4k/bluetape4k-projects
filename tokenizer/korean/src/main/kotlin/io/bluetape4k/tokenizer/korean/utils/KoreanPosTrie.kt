package io.bluetape4k.tokenizer.korean.utils

import java.io.Serializable

/**
 * 한글 품사에 대한 Trie 구조
 *
 * @property curPos 현재 품사
 * @property nextTrie 다음 트라이
 * @property ending 종료 품사
 */
data class KoreanPosTrie(
    val curPos: KoreanPos?,
    val nextTrie: List<KoreanPosTrie>? = null,
    val ending: KoreanPos? = null,
): Serializable
