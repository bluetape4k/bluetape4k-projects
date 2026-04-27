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
 * 하나의 어절에 대한 형태소 분석 후보와 점수 계산 로직을 담습니다.
 *
 * ## 동작/계약
 * - `score`는 토큰 수/미등록/빈도/조사 불일치 등 항목을 `TokenizerProfile` 가중치로 선형 결합한다.
 * - `plus` 연산은 토큰 목록과 단어 수를 누적한 새 `ParsedChunk`를 만든다.
 * - `josaMismatched`는 명사-조사 결합 규칙 위반이 있으면 `1`, 아니면 `0`이다.
 *
 * ```kotlin
 * val chunk = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1)
 * // chunk.countTokens == 1
 * ```
 *
 * @property posNodes 후보를 구성하는 토큰 목록
 * @property words 트라이 전이 기준 단어 수
 * @property profile 점수 계산 프로필
 */
data class ParsedChunk(
    val posNodes: List<KoreanToken>,
    val words: Int,
    val profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
): Serializable {
    companion object: KLogging() {
        /**
         * 접미/어미/조사 계열 품사 집합입니다.
         *
         * ## 동작/계약
         * - 시작 위치 페널티와 공백 가이드 판정에서 제외 품사로 사용된다.
         *
         * ```kotlin
         * val suffix = ParsedChunk.suffixes.contains(Josa)
         * // suffix == true
         * ```
         */
        val suffixes = setOf(Suffix, Eomi, Josa, PreEomi)
        /**
         * `하다/해` 결합 선호 판정에서 앞 토큰으로 허용하는 품사 집합입니다.
         *
         * ## 동작/계약
         * - `isNounHa` 계산 시 첫 토큰 품사가 이 집합에 포함되면 가산점 조건을 검사한다.
         *
         * ```kotlin
         * val preferred = ParsedChunk.preferredBeforeHaVerb.contains(Noun)
         * // preferred == true
         * ```
         */
        val preferredBeforeHaVerb = setOf(Noun, ProperNoun, VerbPrefix)

        private val josaMatchedSet = hashSetOf("는", "를", "다")
        private val josaMatchedSet2 = hashSetOf("은", "을", "이")
    }

    /**
     * 프로필 가중치 기반 최종 후보 점수입니다.
     *
     * ## 동작/계약
     * - 값이 작을수록 우선 후보가 되며 정렬 기준으로 사용된다.
     * - 각 보조 지표(`countUnknowns`, `isNounHa`, `josaMismatched` 등)를 합산해 계산한다.
     *
     * ```kotlin
     * val score = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1).score
     * // score is Float
     * ```
     */
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

    /**
     * 미등록(unknown) 토큰 개수입니다.
     *
     * ## 동작/계약
     * - `posNodes`에서 `unknown == true`인 토큰 수를 센다.
     *
     * ```kotlin
     * val n = ParsedChunk(listOf(KoreanToken("x", Unknown, 0, 1, unknown = true)), 1).countUnknowns
     * // n == 1
     * ```
     */
    val countUnknowns: Int get() = posNodes.count { it.unknown }
    /**
     * 후보 토큰 개수입니다.
     *
     * ## 동작/계약
     * - `posNodes.size`를 그대로 반환한다.
     *
     * ```kotlin
     * val n = ParsedChunk(emptyList(), 0).countTokens
     * // n == 0
     * ```
     */
    val countTokens: Int get() = posNodes.size

    /**
     * 후보 첫 토큰이 접미/조사 계열로 시작하는지 나타내는 페널티 지표입니다.
     *
     * ## 동작/계약
     * - 첫 토큰이 `suffixes`에 속하면 `1`, 아니면 `0`을 반환한다.
     *
     * ```kotlin
     * val v = ParsedChunk(listOf(KoreanToken("은", Josa, 0, 1)), 1).isInitialPosPosition
     * // v == 1
     * ```
     */
    val isInitialPosPosition: Int
        get() = if (posNodes.firstOrNull() != null && suffixes.contains(posNodes.first().pos)) 1 else 0
    //        get() = if ((posNodes.firstOrNull()?.pos ?: Unknown) in suffixes) 1 else 0

    /**
     * 단일 토큰 정확 매칭 여부 지표입니다.
     *
     * ## 동작/계약
     * - 토큰이 1개면 `0`, 2개 이상이면 `1`을 반환한다.
     *
     * ```kotlin
     * val v = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1).isExactMatch
     * // v == 0
     * ```
     */
    val isExactMatch: Int get() = if (posNodes.size == 1) 0 else 1

    /**
     * 공백 가이드 위반 개수 지표입니다.
     *
     * ## 동작/계약
     * - `profile.spaceGuide`가 비어 있으면 `0`이다.
     * - 접미/조사 계열을 제외한 토큰의 `offset`이 가이드에 없으면 위반으로 집계한다.
     *
     * ```kotlin
     * val v = ParsedChunk(emptyList(), 0, TokenizerProfile(spaceGuide = intArrayOf(1))).hasSpaceOutOfGuide
     * // v == 0
     * ```
     */
    val hasSpaceOutOfGuide: Int
        get() =
            if (profile.spaceGuide.isEmpty()) {
                0
            } else {
                posNodes
                    .filter { it.pos !in suffixes }
                    .count { it.offset !in profile.spaceGuide }
            }

    /**
     * 전체가 명사 계열인지 판정하는 지표입니다.
     *
     * ## 동작/계약
     * - 하나라도 `Noun`/`ProperNoun`이 아닌 품사가 있으면 `1`, 전부 명사 계열이면 `0`이다.
     *
     * ```kotlin
     * val v = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1).isAllNouns
     * // v == 0
     * ```
     */
    val isAllNouns: Int
        get() = if (posNodes.any { it.pos != Noun && it.pos != ProperNoun }) 1 else 0

    /**
     * 선호 품사 패턴 매칭 지표입니다.
     *
     * ## 동작/계약
     * - 토큰이 2개이고 품사열이 `profile.preferredPatterns`에 포함되면 `0`, 아니면 `1`이다.
     *
     * ```kotlin
     * val v = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2), KoreanToken("을", Josa, 2, 1)), 1).isPreferredPattern
     * // v == 0
     * ```
     */
    val isPreferredPattern: Int
        get() = if (posNodes.size == 2 && profile.preferredPatterns.contains(posNodes.map { it.pos })) 0 else 1

    /**
     * 명사+하다/해 결합 선호 지표입니다.
     *
     * ## 동작/계약
     * - 첫 토큰이 `preferredBeforeHaVerb`이고 둘째 토큰이 `Verb`이며 `하/해`로 시작하면 `0`, 아니면 `1`이다.
     *
     * ```kotlin
     * val v = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2), KoreanToken("해", Verb, 2, 1)), 1).isNounHa
     * // v == 0
     * ```
     */
    val isNounHa: Int
        get() {
            val notNoun =
                posNodes.size >= 2 &&
                    preferredBeforeHaVerb.contains(posNodes.first().pos) &&
                    posNodes[1].pos == Verb &&
                    (posNodes[1].text.startsWith('하') || posNodes[1].text.startsWith('해'))

            return if (notNoun) 0 else 1
        }

    /**
     * 동일 점수 후보 간 품사 ordinal 합 기반 tie-breaker 값입니다.
     *
     * ## 동작/계약
     * - `posNodes.sumOf { it.pos.ordinal }`를 반환한다.
     *
     * ```kotlin
     * val tie = ParsedChunk(emptyList(), 0).posTieBreaker
     * // tie == 0
     * ```
     */
    val posTieBreaker: Int get() = posNodes.sumOf { it.pos.ordinal }

    /**
     * 미등록 토큰 텍스트 길이 총합을 반환합니다.
     *
     * ## 동작/계약
     * - `unknown == true`인 토큰의 `text.length`만 합산한다.
     *
     * ```kotlin
     * val c = ParsedChunk(listOf(KoreanToken("xx", Noun, 0, 2, unknown = true)), 1).getUnknownCoverage()
     * // c == 2
     * ```
     */
    fun getUnknownCoverage(): Int =
        posNodes.fold(0) { sum, p ->
            if (p.unknown) sum + p.text.length else sum
        }

    /**
     * 명사/고유명사 빈도 사전을 이용한 평균 빈도 점수를 반환합니다.
     *
     * ## 동작/계약
     * - 명사/고유명사는 `1.0 - entityFreq`, 그 외 품사는 `1.0`을 사용한다.
     * - 모든 토큰 점수 합을 토큰 개수로 나눈 값을 반환한다.
     *
     * ```kotlin
     * val score = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1).getFreqScore()
     * // score is Float
     * ```
     */
    fun getFreqScore(): Float {
        val freqScoreSum =
            posNodes.sumOf { p ->
                if (p.pos == Noun || p.pos == ProperNoun) {
                    val freq = KoreanDictionaryProvider.koreanEntityFreq.getOrDefault(p.text, 0.0f)
                    1.0 - freq
                } else {
                    1.0
                }
            }
        return (freqScoreSum / posNodes.size).toFloat()
    }

    /**
     * 두 파싱 후보를 이어붙인 새 `ParsedChunk`를 만듭니다.
     *
     * ## 동작/계약
     * - 토큰 목록과 `words`를 각각 덧셈으로 누적한다.
     * - `profile`은 왼쪽 피연산자 값을 유지한다.
     *
     * ```kotlin
     * val a = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1)
     * val b = ParsedChunk(listOf(KoreanToken("해", Verb, 2, 1)), 1)
     * // (a + b).countTokens == 2
     * ```
     */
    operator fun plus(that: ParsedChunk): ParsedChunk {
        return copy(
            posNodes = posNodes + that.posNodes,
            words = words + that.words,
            profile = profile,
        )
        // ParsedChunk(posNodes + that.posNodes, words + that.words, profile)
    }

    /**
     * 특정 품사의 토큰 개수를 반환합니다.
     *
     * ## 동작/계약
     * - `posNodes`에서 `it.pos == pos`인 원소만 집계한다.
     *
     * ```kotlin
     * val c = ParsedChunk(listOf(KoreanToken("사랑", Noun, 0, 2)), 1).countPos(Noun)
     * // c == 1
     * ```
     */
    fun countPos(pos: KoreanPos): Int = posNodes.count { it.pos == pos }

    /**
     * 명사-조사 결합 불일치 여부 지표입니다.
     *
     * ## 동작/계약
     * - 인접 2토큰 슬라이딩에서 `Noun + Josa` 쌍의 받침 규칙 위반이 있으면 `1`, 아니면 `0`이다.
     *
     * ```kotlin
     * val mismatch = ParsedChunk(
     *     listOf(KoreanToken("사랑", Noun, 0, 2), KoreanToken("는", Josa, 2, 1)),
     *     1
     * ).josaMismatched
     * // mismatch == 1
     * ```
     */
    val josaMismatched: Int
        get() {
            val mismatched: Boolean =
                posNodes.sliding(2).any { tokens ->
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
