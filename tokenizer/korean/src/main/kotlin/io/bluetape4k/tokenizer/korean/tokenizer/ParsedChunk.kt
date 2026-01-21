package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.collections.sliding
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.Hangul
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Determiner
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Unknown
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix
import java.io.Serializable

/**
 * 형태소 분석된 어절(Chunk)을 나타내는 클래스
 *
 * @property posNodes 형태소 분석된 형태소들
 * @property words 형태소 분석된 단어 수
 * @property profile 형태소 분석기 프로파일
 */
data class ParsedChunk(
    val posNodes: List<KoreanToken>,
    val words: Int,
    val profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
): Serializable {

    companion object: KLogging() {
        val suffixes = setOf(Suffix, Eomi, Josa, PreEomi)
        val preferredBeforeHaVerb = setOf(Noun, ProperNoun, VerbPrefix)

        private val josaMatchedSet = hashSetOf("는", "를", "다")
        private val josaMatchedSet2 = hashSetOf("은", "을", "이")
    }

    val score: Float by lazy {
        countTokens * profile.tokenCount +
                countUnknowns * profile.unknown +
                words * profile.wordCount +
                getUnknownCoverage() * profile.unknownCoverage +
                getFreqScore() * profile.freq +
                countPos(Unknown) * profile.unknownPosCount +
                isExactMatch * profile.exactMatch +
                isAllNouns * profile.allNoun +
                isPreferredPattern * profile.preferredPattern +
                countPos(Determiner) * profile.determinerPosCount +
                countPos(Exclamation) * profile.exclamationPosCount +
                isInitialPosPosition * profile.initialPostPosition +
                isNounHa * profile.haVerb +
                hasSpaceOutOfGuide * profile.spaceGuidePenalty +
                josaMismatched * profile.josaUnmatchedPenalty
    }

    val countUnknowns: Int get() = posNodes.count { it.unknown }
    val countTokens: Int get() = posNodes.size

    val isInitialPosPosition: Int
        get() = if (posNodes.firstOrNull() != null && suffixes.contains(posNodes.first().pos)) 1 else 0
    //        get() = if ((posNodes.firstOrNull()?.pos ?: Unknown) in suffixes) 1 else 0

    val isExactMatch: Int get() = if (posNodes.size == 1) 0 else 1

    val hasSpaceOutOfGuide: Int
        get() = if (profile.spaceGuide.isEmpty()) {
            0
        } else {
            posNodes
                .filter { it.pos !in suffixes }
                .count { it.offset !in profile.spaceGuide }

        }

    val isAllNouns: Int
        get() = if (posNodes.any { it.pos != Noun && it.pos != ProperNoun }) 1 else 0

    val isPreferredPattern: Int
        get() = if (posNodes.size == 2 && profile.preferredPatterns.contains(posNodes.map { it.pos })) 0 else 1

    val isNounHa: Int
        get() {
            val notNoun = posNodes.size >= 2 &&
                    preferredBeforeHaVerb.contains(posNodes.first().pos) &&
                    posNodes[1].pos == Verb &&
                    (posNodes[1].text.startsWith('하') || posNodes[1].text.startsWith('해'))

            return if (notNoun) 0 else 1
        }

    val posTieBreaker: Int get() = posNodes.sumOf { it.pos.ordinal }

    fun getUnknownCoverage(): Int {
        return posNodes.fold(0) { sum, p ->
            if (p.unknown) sum + p.text.length else sum
        }
    }

    fun getFreqScore(): Float {
        val freqScoreSum = posNodes.sumOf { p ->
            if (p.pos == Noun || p.pos == ProperNoun) {
                val freq = KoreanDictionaryProvider.koreanEntityFreq.getOrDefault(p.text, 0.0f)
                1.0 - freq
            } else {
                1.0
            }
        }
        return (freqScoreSum / posNodes.size).toFloat()
    }

    operator fun plus(that: ParsedChunk): ParsedChunk {
        return copy(
            posNodes = posNodes + that.posNodes,
            words = words + that.words,
            profile = profile
        )
        // ParsedChunk(posNodes + that.posNodes, words + that.words, profile)
    }

    fun countPos(pos: KoreanPos): Int = posNodes.count { it.pos == pos }

    val josaMismatched: Int
        get() {
            val mismatched: Boolean = posNodes.sliding(2).any { tokens ->
                if (tokens.first().pos == Noun && tokens.last().pos == Josa) {
                    if (Hangul.hasCoda(tokens.first().text.last())) {
                        val nounEnding = Hangul.decomposeHangul(tokens.first().text.last())
                        (nounEnding.coda != 'ㄹ' && tokens.last().text.first() == '로') ||
                                tokens.last().text in josaMatchedSet
                    } else {
                        tokens.last().text.first() == '으' || tokens.last().text in josaMatchedSet2
                    }
                } else {
                    false
                }
            }
            return if (mismatched) 1 else 0
        }
}
