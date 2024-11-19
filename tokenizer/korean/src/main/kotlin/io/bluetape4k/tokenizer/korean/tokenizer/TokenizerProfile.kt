package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.support.emptyIntArray
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import java.io.Serializable

/**
 * 형태소 분석을 위한 옵션
 */
data class TokenizerProfile(
    val tokenCount: Float = 0.18f,
    val unknown: Float = 0.3f,
    val wordCount: Float = 0.3f,
    val freq: Float = 0.2f,
    val unknownCoverage: Float = 0.5f,
    val exactMatch: Float = 0.5f,
    val allNoun: Float = 0.1f,
    val unknownPosCount: Float = 10.0f,
    val determinerPosCount: Float = -0.01f,
    val exclamationPosCount: Float = 0.01f,
    val initialPostPosition: Float = 0.2f,
    val haVerb: Float = 0.3f,
    val preferredPattern: Float = 0.6f,
    val preferredPatterns: List<List<KoreanPos>> = listOf(listOf(Noun, Josa), listOf(ProperNoun, Josa)),
    val spaceGuide: IntArray = emptyIntArray,
    val spaceGuidePenalty: Float = 3.0f,
    val josaUnmatchedPenalty: Float = 3.0f,
): Serializable {
    companion object {
        @JvmField
        val DefaultProfile = TokenizerProfile()
    }
}
