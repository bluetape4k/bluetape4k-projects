package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.collections.eclipse.unifiedSetOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.Hangul
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Alpha
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.CashTag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Hashtag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Number
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Space
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx.SelfNode
import io.bluetape4k.tokenizer.korean.utils.init

/**
 * 구 추출 후보 계산에서 사용하는 구 단위 컬렉션 타입입니다.
 *
 * ## 동작/계약
 * - 하나의 원소는 연속된 `KoreanPhrase` 묶음을 의미한다.
 * - 내부 후보 생성/중복 제거 단계에서 동일 타입을 공통으로 사용한다.
 *
 * ```kotlin
 * val chunk: KoreanPhraseChunk = listOf(KoreanPhrase(emptyList()))
 * // chunk.size == 1
 * ```
 */
typealias KoreanPhraseChunk = List<KoreanPhrase>

/**
 * 형태소 분석 결과에서 명사 중심 구 후보를 추출합니다.
 *
 * ## 동작/계약
 * - 토큰 POS 시퀀스를 접기(`collapsePos`) 후 길이/품사 조건으로 후보 구를 필터링한다.
 * - `filterSpam=true`이면 `KoreanDictionaryProvider.spamNouns` 포함 토큰이 있는 구를 제외한다.
 * - `enableHashtags=true`이면 입력의 `Hashtag`, `CashTag` 토큰을 결과에 추가한다.
 *
 * ```kotlin
 * val tokens = tokenize("성탄절 쇼핑 성탄절 쇼핑")
 * val phrases = KoreanPhraseExtractor.extractPhrases(tokens, filterSpam = false)
 * // phrases.map { it.text }.distinct() == phrases.map { it.text }
 * ```
 */
object KoreanPhraseExtractor: KLogging() {

    /**
     * 공백 제외 구 길이의 최소 문자 수입니다.
     *
     * ## 동작/계약
     * - 단일 구 후보는 이 값 이상이거나 최소 구 개수 조건을 만족해야 유지된다.
     *
     * ```kotlin
     * val minChars = KoreanPhraseExtractor.MinCharsPerPhraseChunkWithoutSpaces
     * // minChars == 2
     * ```
     */
    const val MinCharsPerPhraseChunkWithoutSpaces = 2
    /**
     * 공백 제외 구 길이 판정 시 사용하는 최소 구 개수입니다.
     *
     * ## 동작/계약
     * - 후보 구에 포함된 phrase 개수가 이 값 이상이면 길이 조건을 통과한다.
     *
     * ```kotlin
     * val minPhrases = KoreanPhraseExtractor.MinPhrasesPerPhraseChunk
     * // minPhrases == 3
     * ```
     */
    const val MinPhrasesPerPhraseChunk = 3

    /**
     * 공백 제외 구 길이의 최대 문자 수입니다.
     *
     * ## 동작/계약
     * - 후보 구 전체 문자 합이 이 값을 초과하면 제외된다.
     *
     * ```kotlin
     * val maxChars = KoreanPhraseExtractor.MaxCharsPerPhraseChunkWithoutSpaces
     * // maxChars == 30
     * ```
     */
    const val MaxCharsPerPhraseChunkWithoutSpaces = 30
    /**
     * 공백 제외 구 길이 판정 시 사용하는 최대 구 개수입니다.
     *
     * ## 동작/계약
     * - 후보 구 내 phrase 수가 이 값을 초과하면 제외된다.
     *
     * ```kotlin
     * val maxPhrases = KoreanPhraseExtractor.MaxPhrasesPerPhraseChunk
     * // maxPhrases == 8
     * ```
     */
    const val MaxPhrasesPerPhraseChunk = 8

    /**
     * 관형형 용언 판정에 사용하는 종성 집합입니다.
     *
     * ## 동작/계약
     * - 용언 후보의 마지막 글자 종성이 이 집합에 속하면 수식어 후보로 간주한다.
     *
     * ```kotlin
     * val contains = 'ㄴ' in KoreanPhraseExtractor.ModifyingPredicateEndings
     * // contains == true
     * ```
     */
    @JvmField
    val ModifyingPredicateEndings = setOf('ㄹ', 'ㄴ')

    /**
     * 관형형 용언 판정에서 제외하는 예외 문자 집합입니다.
     *
     * ## 동작/계약
     * - 마지막 글자가 이 집합에 속하면 관형형 판정에서 제외한다.
     *
     * ```kotlin
     * val excluded = '만' in KoreanPhraseExtractor.ModifyingPredicateExceptions
     * // excluded == true
     * ```
     */
    @JvmField
    val ModifyingPredicateExceptions = setOf('만')

    /**
     * 구 확장에서 연속 결합 대상으로 허용하는 품사 집합입니다.
     *
     * ## 동작/계약
     * - `collapsePhrases`에서 현재 phrase의 품사가 이 집합에 포함되면 버퍼를 유지하며 결합한다.
     *
     * ```kotlin
     * val joinable = KoreanPhraseExtractor.PhraseTokens.contains(Noun)
     * // joinable == true
     * ```
     */
    @JvmField
    val PhraseTokens = setOf(Noun, ProperNoun, Space)

    /**
     * 조사 연결어로 취급하는 문자열 집합입니다.
     *
     * ## 동작/계약
     * - `Josa` 품사 phrase가 이 집합이면 비명사 연결 후보로 유지한다.
     *
     * ```kotlin
     * val isConjunctionJosa = "와" in KoreanPhraseExtractor.ConjunctionJosa
     * // isConjunctionJosa == true
     * ```
     */
    @JvmField
    val ConjunctionJosa = setOf("와", "과", "의")

    /**
     * 트림 후 구의 시작으로 허용하는 품사 집합입니다.
     *
     * ## 동작/계약
     * - `trimNonNouns`는 앞쪽에서 이 집합이 아닌 phrase를 제거한다.
     *
     * ```kotlin
     * val allowed = KoreanPhraseExtractor.PhraseHeadPoses.contains(Adjective)
     * // allowed == true
     * ```
     */
    @JvmField
    val PhraseHeadPoses = setOf(Adjective, Noun, ProperNoun, Alpha, Number)

    /**
     * 트림 후 구의 끝으로 허용하는 품사 집합입니다.
     *
     * ## 동작/계약
     * - `trimNonNouns`는 뒤쪽에서 이 집합이 아닌 phrase를 제거한다.
     *
     * ```kotlin
     * val allowed = KoreanPhraseExtractor.PhraseTailPoses.contains(Noun)
     * // allowed == true
     * ```
     */
    @JvmField
    val PhraseTailPoses = setOf(Noun, ProperNoun, Alpha, Number)

    /**
     * 0 for optional, 1 for required
     * * for optional repeatable, + for required repeatable
     *
     * Substantive: 체언 (초거대기업의)
     * Predicate: 용언 (하였었습니다, 개예뻤었다)
     * Modifier: 수식언 (모르는 할수도있는 보이기도하는 예뻐 예쁜 완전 레알 초인간적인 잘 잘한)
     * Standalone: 독립언
     * Functional: 관계언 (조사)
     *
     * N Noun: 명사 (Nouns, Pronouns, Company Names, Proper Noun, Person Names, Numerals, Standalone, Dependent)
     * V Verb: 동사 (하, 먹, 자, 차)
     * J Adjective: 형용사 (예쁘다, 크다, 작다)
     * A Adverb: 부사 (잘, 매우, 빨리, 반드시, 과연)
     * D Determiner: 관형사 (새, 헌, 참, 첫, 이, 그, 저)
     * E Exclamation: 감탄사 (헐, ㅋㅋㅋ, 어머나, 얼씨구)
     *
     * C Conjunction: 접속사
     *
     * j SubstantiveJosa: 조사 (의, 에, 에서)
     * l AdverbialJosa: 부사격 조사 (~인, ~의, ~일)
     * e Eomi: 어말어미 (다, 요, 여, 하댘ㅋㅋ)
     * r PreEomi: 선어말어미 (었)
     *
     * p NounPrefix: 접두사 ('초'대박)
     * v VerbPrefix: 동사 접두어 ('쳐'먹어)
     * s Suffix: 접미사 (~적)
     *
     * a Alpha,
     * n Number
     * o Others
     */
    private val COLLAPSING_RULES = mapOf(
        "D0m*N1s0" to Noun, // Substantive
        "n*a+n*" to Noun,
        "n+" to Noun,
        /* Predicate 초기뻐하다, 와주세요, 초기뻤었고, 추첨하다, 구경하기힘들다, 기뻐하는, 기쁜, 추첨해서, 좋아하다, 걸려있을 */
        "v*V1r*e0" to Verb,
        "v*J1r*e0" to Adjective
    )

    private val collapseTrie by lazy { KoreanPosx.getTrie(COLLAPSING_RULES) }


    private fun trimPhraseChunk(phrases: KoreanPhraseChunk): KoreanPhraseChunk {
        val phrasesToTrim = trimNonNouns(phrases)
        return trimSpacesFromPhrase(phrasesToTrim)
    }

    private fun trimNonNouns(phrases: KoreanPhraseChunk): List<KoreanPhrase> {
        return phrases
            .dropWhile { !PhraseHeadPoses.contains(it.pos) }
            .dropLastWhile { !PhraseTailPoses.contains(it.pos) }
    }

    private fun trimSpacesFromPhrase(phrasesToTrim: Collection<KoreanPhrase>): List<KoreanPhrase> {
        return phrasesToTrim.mapIndexed { i, phrase ->
            when {
                phrasesToTrim.size == 1 -> {
                    KoreanPhrase(
                        phrase.tokens
                            .dropWhile { it.pos == Space }
                            .dropLastWhile { it.pos == Space },
                        phrase.pos
                    )
                }

                i == 0                  ->
                    KoreanPhrase(phrase.tokens.dropWhile { it.pos == Space }, phrase.pos)

                i == phrasesToTrim.size - 1 -> {
                    val tokens = phrase.tokens.dropLastWhile { it.pos == Space }
                    KoreanPhrase(tokens, phrase.pos)
                }

                else                    -> phrase
            }
        }
    }


    private fun trimPhrase(phrase: KoreanPhrase): KoreanPhrase {
        val tokens = phrase.tokens
            .dropWhile { it.pos == Space }
            .dropLastWhile { it.pos == Space }

        return KoreanPhrase(tokens, phrase.pos)
    }

    private fun isProperPhraseChunk(phraseChunk: KoreanPhraseChunk): Boolean {
        fun notEndingInNonPhrasesSuffix(): Boolean {
            val lastToken = phraseChunk.last().tokens.last()
            return !(lastToken.pos == Suffix && lastToken.text == "적")
        }

        fun isRightLength(): Boolean {
            val phraseChunkWithoutSpaces = phraseChunk.filter { it.pos != Space }

            fun checkMaxLength(): Boolean {
                return phraseChunkWithoutSpaces.size <= MaxPhrasesPerPhraseChunk &&
                        phraseChunkWithoutSpaces.sumOf { it.length } <= MaxCharsPerPhraseChunkWithoutSpaces
            }

            fun checkMinLength(): Boolean {
                return phraseChunkWithoutSpaces.size >= MinPhrasesPerPhraseChunk ||
                        (phraseChunkWithoutSpaces.sumOf { it.length } >= MinCharsPerPhraseChunkWithoutSpaces)
            }

            fun checkMinLengthPerToken(): Boolean {
                return phraseChunkWithoutSpaces.any { it.length > 1 }
            }

            return checkMaxLength() && checkMinLength() && checkMinLengthPerToken()
        }

        return isRightLength() && notEndingInNonPhrasesSuffix()
    }

    /**
     * 토큰 POS 시퀀스를 규칙 트라이 기반 phrase 시퀀스로 접습니다.
     *
     * ## 동작/계약
     * - 현재 트라이에서 이어질 수 있으면 마지막 phrase에 토큰을 붙여 확장한다.
     * - 이어질 수 없지만 시작 가능한 품사면 새 phrase를 시작한다.
     * - `KoreanPhraseExtractorTest` 기준으로 `Modifier + Noun`은 하나의 `Noun` phrase로 합쳐진다.
     *
     * ```kotlin
     * val collapsed = KoreanPhraseExtractor.collapsePos(
     *     listOf(KoreanToken("m", Modifier, 0, 1), KoreanToken("N", Noun, 1, 1))
     * )
     * // collapsed.first().text == "mN"
     * ```
     */
    fun collapsePos(tokens: List<KoreanToken>): List<KoreanPhrase> {

        fun getTries(token: KoreanToken, trie: List<KoreanPosTrie?>): Pair<KoreanPosTrie?, List<KoreanPosTrie?>> {
            val curTrie = trie.firstOrNull { it != null && it.curPos == token.pos }
            val nextTrie = curTrie?.nextTrie?.map { if (it == SelfNode) curTrie else it } ?: emptyList()
            return curTrie to nextTrie
        }

        fun getInit(phraseBuffer: PhraseBuffer): List<KoreanPhrase> {
            return if (phraseBuffer.phrases.isEmpty()) emptyList()
            else phraseBuffer.phrases.init()
        }

        val phrases = mutableListOf<KoreanPhrase>()
        var curTrie: List<KoreanPosTrie?> = collapseTrie

        tokens
            .onEach { token ->
                when {
                    curTrie.any { it?.curPos == token.pos } -> {
                        // Extend the current phase
                        val (ct, nt) = getTries(token, curTrie)

                        if (phrases.isEmpty() || curTrie == collapseTrie) {
                            phrases.add(KoreanPhrase(arrayListOf(token), ct?.ending ?: Noun))
                        } else {
                            val newPhrase = KoreanPhrase(phrases.last().tokens + token, ct?.ending ?: Noun)
                            if (phrases.isEmpty()) {
                                phrases.add(newPhrase)
                            } else {
                                phrases[phrases.lastIndex] = newPhrase
                            }
                        }
                        curTrie = nt
                    }

                    collapseTrie.any { it.curPos == token.pos } -> {
                        // Start a new phrase
                        val (ct, nt) = getTries(token, collapseTrie)
                        phrases.add(KoreanPhrase(listOf(token), ct?.ending ?: Noun))
                        curTrie = nt
                    }

                    else                                    -> {
                        // Add a single word
                        phrases.add(KoreanPhrase(listOf(token), token.pos))
                        curTrie = collapseTrie
                    }
                }
            }

        return phrases
    }

    private fun distinctPhrases(chunks: List<KoreanPhraseChunk>): List<KoreanPhraseChunk> {
        val phraseChunks = mutableListOf<KoreanPhraseChunk>()
        val buffer = unifiedSetOf<String>()

        chunks
            .onEach { chunk ->
                val phraseText = chunk.joinToString("") {
                    it.tokens.joinToString("") { token -> token.text }
                }
                if (!buffer.contains(phraseText)) {
                    phraseChunks.add(0, chunk)
                    buffer.add(phraseText)
                }
            }
        return phraseChunks.reversed()
    }

    private fun getCandidatePhraseChunks(
        phrases: KoreanPhraseChunk,
        filterSpam: Boolean = false,
    ): List<KoreanPhraseChunk> {
        fun isNotSpam(phrase: KoreanPhrase): Boolean {
            return !filterSpam ||
                    !phrase.tokens.any { KoreanDictionaryProvider.spamNouns.contains(it.text) }
        }

        fun isNonNounPhraseCandidate(phrase: KoreanPhrase): Boolean {
            val trimmed = trimPhrase(phrase)

            // 하는, 할인된, 할인될, exclude: 하지만
            fun isModifyingPredicate(): Boolean {
                val lastChar = trimmed.tokens.last().text.last()
                return (trimmed.pos == Verb || trimmed.pos == Adjective) &&
                        ModifyingPredicateEndings.contains(Hangul.decomposeHangul(lastChar).coda) &&
                        !ModifyingPredicateExceptions.contains(lastChar)
            }

            // 과, 와, 의
            fun isConjunction(): Boolean =
                trimmed.pos == Josa && ConjunctionJosa.contains(trimmed.tokens.last().text)

            fun isAlphaNumeric(): Boolean =
                trimmed.pos == Alpha || trimmed.pos == Number

            return isAlphaNumeric() || isModifyingPredicate() || isConjunction()
        }

        fun collapseNounPhrases(phrases1: KoreanPhraseChunk): KoreanPhraseChunk {

            val output = mutableListOf<KoreanPhrase>()
            val buffer = mutableListOf<KoreanPhrase>()

            phrases1
                .onEach { phrase ->
                    if (phrase.pos == Noun || phrase.pos == ProperNoun) {
                        buffer.add(phrase)
                    } else {
                        val tempPhrase =
                            if (buffer.isNotEmpty()) listOf(KoreanPhrase(buffer.flatMap { it.tokens }), phrase)
                            else listOf(phrase)

                        output.addAll(tempPhrase)
                        buffer.clear()
                    }
                }

            if (buffer.isNotEmpty()) {
                output.add(KoreanPhrase(buffer.flatMap { it.tokens }))
            }
            return output
        }

        fun collapsePhrases(phrases1: KoreanPhraseChunk): List<KoreanPhraseChunk> {
            fun addPhraseToBuffer(phrase: KoreanPhrase, buffer: List<KoreanPhraseChunk>) =
                buffer.map { it + phrase }.toList()

            // NOTE: 현재 이 부분은 변경하면 안됩니다.
            //
            fun newBuffer() = listOf(listOf<KoreanPhrase>())

            val output = mutableListOf<KoreanPhraseChunk>()
            var buffer = newBuffer()

            phrases1
                .onEach {
                    buffer = if (it.pos in PhraseTokens && isNotSpam(it)) {
                        val bufferWithThisPhrase = addPhraseToBuffer(it, buffer)
                        if (it.pos == Noun || it.pos == ProperNoun) {
                            output.addAll(bufferWithThisPhrase)
                        }
                        bufferWithThisPhrase.toList()
                    } else if (isNonNounPhraseCandidate(it)) {
                        addPhraseToBuffer(it, buffer).toList()
                    } else {
                        output.addAll(buffer)
                        newBuffer()
                    }
                }

            if (buffer.isNotEmpty()) {
                output.addAll(buffer)
                return output
            }
            return buffer
        }

        fun getSingleTokenNouns(): List<KoreanPhraseChunk> {

            fun isSingle(phrase: KoreanPhrase): Boolean {
                val trimmed = trimPhrase(phrase)

                return phrase.pos in setOf(Noun, ProperNoun) &&
                        isNotSpam(phrase) &&
                        (trimmed.length >= MinCharsPerPhraseChunkWithoutSpaces || trimmed.tokens.size >= MinPhrasesPerPhraseChunk)
            }

            return phrases
                .filter { isSingle(it) }
                .map { arrayListOf(trimPhrase(it)) }
        }

        val nounPhrases = collapseNounPhrases(phrases)
        val phraseCollapsed = collapsePhrases(nounPhrases)
        val singleTokenNouns = getSingleTokenNouns()

        val chunks = phraseCollapsed.map { trimPhraseChunk(it) } + singleTokenNouns

        return distinctPhrases(chunks)
    }

    /**
     * 토큰 목록에서 조건에 맞는 phrase를 추출합니다.
     *
     * ## 동작/계약
     * - `collapsePos -> getCandidatePhraseChunks -> permutateCandidates` 순서로 후보를 계산한다.
     * - `enableHashtags=true`이면 `Hashtag`, `CashTag` 토큰을 단일 phrase로 추가한다.
     * - `KoreanPhraseExtractorTest` 기준으로 `"성탄절 쇼핑..."` 입력에서 텍스트 중복 phrase를 제거해 반환한다.
     *
     * ```kotlin
     * val phrases = KoreanPhraseExtractor.extractPhrases(tokenize("성탄절 쇼핑 성탄절 쇼핑"), filterSpam = false)
     * // phrases.map { it.text }.distinct() == phrases.map { it.text }
     * ```
     */
    fun extractPhrases(
        tokens: List<KoreanToken>,
        filterSpam: Boolean = false,
        enableHashtags: Boolean = true,
    ): List<KoreanPhrase> {

        val collapsed = collapsePos(tokens)
        val candidates = getCandidatePhraseChunks(collapsed, filterSpam)
        val permutatedCandidates = permutateCandidates(candidates)

        val phrases = permutatedCandidates
            .map { KoreanPhrase(trimPhraseChunk(it).flatMap { chunk -> chunk.tokens }) }
            .toMutableList()

        if (enableHashtags) {
            val hashtags = tokens
                .filter { it.pos in listOf(Hashtag, CashTag) }
                .map { KoreanPhrase(listOf(it), it.pos) }

            phrases.addAll(hashtags)
        }
        return phrases
    }

    private fun permutateCandidates(candidates: List<KoreanPhraseChunk>): List<KoreanPhraseChunk> =
        distinctPhrases(candidates.filter { isProperPhraseChunk(it) })
}
