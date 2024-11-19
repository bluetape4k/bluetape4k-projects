package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

data class CandidateParse(
    val parse: ParsedChunk,
    val curTrie: List<KoreanPosTrie>,
    val ending: KoreanPos? = null,
): Serializable
