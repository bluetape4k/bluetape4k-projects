package io.bluetape4k.tokenizer.korean.tokenizer

import java.io.Serializable

data class Sentence(
    val text: String,
    val start: Int,
    val end: Int,
): Serializable {
    override fun toString(): String = "$text($start,$end)"
}
