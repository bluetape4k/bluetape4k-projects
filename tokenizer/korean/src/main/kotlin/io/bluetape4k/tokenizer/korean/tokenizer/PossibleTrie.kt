package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import java.io.Serializable

data class PossibleTrie(
    val curTrie: KoreanPosTrie,
    val words: Int,
): Serializable
