package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.Hangul
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Alpha
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Number
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ProperNoun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Space
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPosTrie
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import java.util.concurrent.CopyOnWriteArrayList


/**
 * 명사 중심 규칙으로 한국어 phrase를 추출합니다.
 *
 * ## 동작/계약
 * - POS 시퀀스를 접은 뒤(`collapsePos`) 길이/품사 제약으로 후보를 걸러낸다.
 * - 일반 phrase 추출기와 달리 해시태그 추가/스팸 필터 옵션 없이 명사구 추출에 집중한다.
 * - `NounPhraseExtractorTest` 기준으로 숫자+단위 표현(`3400원`, `1,200.34원`)을 명사구로 인식한다.
 *
 * ```kotlin
 * val phrases = NounPhraseExtractor.extractPhrases(KoreanProcessor.tokenizeForNoun("떡 만두국"))
 * // phrases.joinToString(", ") == "떡(Noun: 0, 1), 만두국(Noun: 2, 3)"
 * ```
 */
object NounPhraseExtractor: KLogging() {
    /**
     * 공백 제외 구 길이의 최소 문자 수입니다.
     *
     * ## 동작/계약
     * - 단일 구 후보의 최소 길이 조건으로 사용된다.
     *
     * ```kotlin
     * val minChars = NounPhraseExtractor.MinCharsPerPhraseChunkWithoutSpaces
     * // minChars == 2
     * ```
     */
    const val MinCharsPerPhraseChunkWithoutSpaces = 2
    /**
     * 공백 제외 구 길이 판정의 최소 phrase 개수입니다.
     *
     * ## 동작/계약
     * - 구 길이 합이 짧아도 phrase 개수가 이 값 이상이면 통과한다.
     *
     * ```kotlin
     * val minPhrases = NounPhraseExtractor.MinPhrasesPerPhraseChunk
     * // minPhrases == 3
     * ```
     */
    const val MinPhrasesPerPhraseChunk = 3
    /**
     * 공백 제외 구 길이의 최대 문자 수입니다.
     *
     * ## 동작/계약
     * - 후보 구 전체 문자 합이 이 값을 넘으면 제외된다.
     *
     * ```kotlin
     * val maxChars = NounPhraseExtractor.MaxCharsPerPhraseChunkWithoutSpaces
     * // maxChars == 30
     * ```
     */
    const val MaxCharsPerPhraseChunkWithoutSpaces = 30
    /**
     * 공백 제외 구 길이의 최대 phrase 개수입니다.
     *
     * ## 동작/계약
     * - 후보 구 내 phrase 수가 이 값을 넘으면 제외된다.
     *
     * ```kotlin
     * val maxPhrases = NounPhraseExtractor.MaxPhrasesPerPhraseChunk
     * // maxPhrases == 3
     * ```
     */
    const val MaxPhrasesPerPhraseChunk = 3

    /**
     * 관형형 용언 판정에 사용하는 종성 집합입니다.
     *
     * ## 동작/계약
     * - 마지막 글자 종성이 이 집합이면 비명사 연결 후보로 취급한다.
     *
     * ```kotlin
     * val contains = 'ㄹ' in NounPhraseExtractor.ModifyingPredicateEndings
     * // contains == true
     * ```
     */
    @JvmField
    val ModifyingPredicateEndings = setOf('ㄹ', 'ㄴ')

    /**
     * 관형형 용언 판정에서 제외하는 예외 문자 집합입니다.
     *
     * ## 동작/계약
     * - 마지막 글자가 이 집합이면 비명사 연결 후보에서 제외한다.
     *
     * ```kotlin
     * val excluded = '만' in NounPhraseExtractor.ModifyingPredicateExceptions
     * // excluded == true
     * ```
     */
    @JvmField
    val ModifyingPredicateExceptions = setOf('만')

    /**
     * phrase 결합을 유지할 명사 계열 품사 집합입니다.
     *
     * ## 동작/계약
     * - `collapsePhrases`에서 이 집합에 속하면 버퍼를 이어 붙인다.
     *
     * ```kotlin
     * val joinable = NounPhraseExtractor.PhraseTokens.contains(Noun)
     * // joinable == true
     * ```
     */
    @JvmField
    val PhraseTokens = setOf(Noun, ProperNoun)

    /**
     * 연결 조사로 취급하는 문자열 집합입니다.
     *
     * ## 동작/계약
     * - 현재 구현에서는 집합 정의만 존재하며 후보 필터에서 직접 사용하지 않는다.
     *
     * ```kotlin
     * val hasWa = "와" in NounPhraseExtractor.ConjunctionJosa
     * // hasWa == true
     * ```
     */
    @JvmField
    val ConjunctionJosa = hashSetOf("와", "과", "의")

    /**
     * 구 시작 위치에 허용하는 품사 집합입니다.
     *
     * ## 동작/계약
     * - `trimNonNouns`에서 앞부분 제거 기준으로 사용한다.
     *
     * ```kotlin
     * val allowed = NounPhraseExtractor.PhraseHeadPoses.contains(Adjective)
     * // allowed == true
     * ```
     */
    @JvmField
    val PhraseHeadPoses = setOf(Adjective, Noun, ProperNoun, Alpha, Number)

    /**
     * 구 끝 위치에 허용하는 품사 집합입니다.
     *
     * ## 동작/계약
     * - `trimNonNouns`에서 뒷부분 제거 기준으로 사용한다.
     *
     * ```kotlin
     * val allowed = NounPhraseExtractor.PhraseTailPoses.contains(Noun)
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
        "n+" to Noun
    )
    /* Predicate 초기뻐하다, 와주세요, 초기뻤었고, 추첨하다, 구경하기힘들다, 기뻐하는, 기쁜, 추첨해서, 좋아하다, 걸려있을 */
    //            "v*V1r*e0" to Verb,
    //            "v*J1r*e0" to Adjective)

    private val CollapseTrie by lazy { KoreanPosx.getTrie(COLLAPSING_RULES) }


    private fun trimPhraseChunk(phrases: KoreanPhraseChunk): KoreanPhraseChunk {
        fun trimNonNouns() = phrases
            .dropWhile { !PhraseHeadPoses.contains(it.pos) }
            .dropLastWhile { !PhraseTailPoses.contains(it.pos) }

        fun trimSpacesFromPhrase(phrasesToTrim: Collection<KoreanPhrase>): List<KoreanPhrase> {
            return phrasesToTrim
                .mapIndexed { i, phrase ->
                    when {
                        phrasesToTrim.size == 1 -> KoreanPhrase(
                            phrase.tokens
                                .dropWhile { it.pos == Space }
                                .dropLastWhile { it.pos == Space },
                            phrase.pos
                        )

                        i == 0                  -> KoreanPhrase(
                            phrase.tokens.dropWhile { it.pos == Space },
                            phrase.pos
                        )

                        i == phrasesToTrim.size - 1 -> {
                            val tokens = phrase.tokens.dropLastWhile { it.pos == Space }
                            KoreanPhrase(tokens, phrase.pos)
                        }

                        else                    -> phrase
                    }
                }
        }

        val trimNon = trimNonNouns()
        return trimSpacesFromPhrase(trimNon)
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

            fun checkNoneDictionary(): Boolean {
                if (phraseChunk.size == 1 && phraseChunk.all { it.tokens.size == 1 }) {
                    val singleTokenTest = phraseChunk[0].tokens[0].text
                    return KoreanDictionaryProvider.koreanDictionary[Noun]!!.contains(singleTokenTest)
                }
                return false
            }

            return checkMaxLength()
                    && phraseChunk.isNotEmpty()
                    && (checkNoneDictionary() || (checkMinLength() && checkMinLengthPerToken()))
        }

        return isRightLength() && notEndingInNonPhrasesSuffix()
    }

    /**
     * 토큰 POS 시퀀스를 명사 중심 phrase 시퀀스로 접습니다.
     *
     * ## 동작/계약
     * - 규칙 트라이와 현재 상태를 이용해 phrase 확장/재시작을 결정한다.
     * - `NounPhraseExtractorTest` 기준으로 `Modifier + Noun`은 하나의 `Noun` phrase로 병합된다.
     * - 규칙에 맞지 않는 토큰은 단일 phrase로 유지된다.
     *
     * ```kotlin
     * val collapsed = NounPhraseExtractor.collapsePos(
     *     listOf(KoreanToken("m", Modifier, 0, 1), KoreanToken("N", Noun, 1, 1))
     * )
     * // collapsed.first().text == "mN"
     * ```
     */
    fun collapsePos(tokens: Collection<KoreanToken>): List<KoreanPhrase> {

        fun getTries(token: KoreanToken, trie: List<KoreanPosTrie?>): Pair<KoreanPosTrie?, List<KoreanPosTrie?>> {
            val curTrie = trie.firstOrNull { it != null && it.curPos == token.pos }
            val nextTrie = curTrie?.nextTrie
                ?.map { if (it == KoreanPosx.SelfNode) curTrie else it }
                ?.toList()
                ?: emptyList()

            return Pair(curTrie, nextTrie)
        }

        val phrases = mutableListOf<KoreanPhrase>()
        var curTrie: List<KoreanPosTrie?> = CollapseTrie

        tokens
            .onEach { token ->
                when {
                    curTrie.any { it != null && it.curPos == token.pos } -> {
                        // Extend the current phase
                        val (ct, nt) = getTries(token, curTrie)

                        if (phrases.isEmpty() || curTrie == CollapseTrie) {
                            phrases.add(KoreanPhrase(listOf(token), ct?.ending ?: Noun))
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

                    CollapseTrie.any { it.curPos == token.pos } -> {
                        // Start a new phrase
                        val (ct, nt) = getTries(token, CollapseTrie)
                        phrases.add(KoreanPhrase(listOf(token), ct?.ending ?: Noun))
                        curTrie = nt
                    }

                    else                                        -> {
                        // Add a single word
                        phrases.add(KoreanPhrase(listOf(token), token.pos))
                        curTrie = CollapseTrie
                    }
                }
            }

        return phrases
    }

    private fun distinctPhrases(chunks: Collection<KoreanPhraseChunk>): List<KoreanPhraseChunk> {

        val phraseChunks = mutableListOf<KoreanPhraseChunk>()
        val buffer = mutableSetOf<String>()

        chunks.forEach { chunk ->
            val phraseText = chunk.joinToString("") { phrase ->
                phrase.tokens.joinToString("") { it.text }
            }

            if (buffer.add(phraseText)) {
                phraseChunks.add(0, chunk)
            }
        }
        return phraseChunks.reversed()
    }

    private fun getCandidatePhraseChunks(phrases: KoreanPhraseChunk): List<KoreanPhraseChunk> {

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
                trimmed.pos == KoreanPos.Josa && ConjunctionJosa.contains(trimmed.tokens.last().text)

            fun isAlphaNumeric(): Boolean =
                trimmed.pos == Alpha || trimmed.pos == Number

            return isAlphaNumeric() || isModifyingPredicate()
        }

        fun collapseNounPhrases(phrases1: KoreanPhraseChunk): KoreanPhraseChunk {

            val output = CopyOnWriteArrayList<KoreanPhrase>()
            val buffer = CopyOnWriteArrayList<KoreanPhrase>()

            phrases1
                .onEach { phrase ->
                    if (phrase.pos == Noun || phrase.pos == ProperNoun) {
                        buffer.add(phrase)
                    } else {
                        val tempPhrase =
                            if (buffer.isNotEmpty()) mutableListOf(KoreanPhrase(buffer.flatMap { it.tokens }), phrase)
                            else listOf(phrase)
                        output.addAll(tempPhrase)
                        buffer.clear()
                    }
                }

            if (buffer.isNotEmpty()) {
                output.add(KoreanPhrase(tokens = buffer.flatMap { it.tokens }))
            }
            return output
        }

        fun collapsePhrases(phrases1: KoreanPhraseChunk): List<KoreanPhraseChunk> {
            fun addPhraseToBuffer(phrase: KoreanPhrase, buffer: List<KoreanPhraseChunk>): List<KoreanPhraseChunk> =
                buffer.map { it + phrase }.toList()

            // NOTE: 현재 이 부분은 변경하면 안됩니다.
            //
            fun newBuffer() = listOf(listOf<KoreanPhrase>())

            val output = CopyOnWriteArrayList<KoreanPhraseChunk>()
            var buffer = newBuffer()

            phrases1
                .onEach {
                    buffer = if (it.pos in PhraseTokens) {
                        val bufferWithThisPhrase = addPhraseToBuffer(it, buffer)
                        if (it.pos == Noun || it.pos == ProperNoun) {
                            output.addAll(bufferWithThisPhrase)
                        }
                        bufferWithThisPhrase
                    } else if (it.pos != Space && isNonNounPhraseCandidate(it)) {
                        addPhraseToBuffer(it, buffer)
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
                        (trimmed.length >= MinCharsPerPhraseChunkWithoutSpaces || trimmed.tokens.size >= MinPhrasesPerPhraseChunk)
            }

            return phrases
                .filter { isSingle(it) }
                .map { listOf(trimPhrase(it)) }
        }

        val nounPhrases = collapseNounPhrases(phrases)
        val phraseCollapsed = collapsePhrases(nounPhrases)

        return distinctPhrases(phraseCollapsed.map(this::trimPhraseChunk) + getSingleTokenNouns())
    }

    /**
     * 명사 중심 후보를 계산해 최종 phrase 목록을 반환합니다.
     *
     * ## 동작/계약
     * - `collapsePos -> getCandidatePhraseChunks -> permutateCandidates` 순서로 처리한다.
     * - `NounPhraseExtractorTest` 기준으로 `"떡 만두국"` 입력에서 `"떡"`, `"만두국"`을 반환한다.
     * - 최종 phrase는 `trimPhraseChunk`를 거쳐 앞뒤 공백 토큰이 제거된다.
     *
     * ```kotlin
     * val phrases = NounPhraseExtractor.extractPhrases(KoreanProcessor.tokenizeForNoun("짜장면 3400원."))
     * // phrases.map { it.text }.contains("3400원") == true
     * ```
     */
    fun extractPhrases(tokens: Collection<KoreanToken>): List<KoreanPhrase> {

        val collapsed = collapsePos(tokens)
        val candidates = getCandidatePhraseChunks(collapsed)
        val permutatedCandidates = permutateCandidates(candidates)

        return permutatedCandidates
            .map { chunk ->
                KoreanPhrase(trimPhraseChunk(chunk).flatMap { it.tokens })
            }
    }

    private fun permutateCandidates(candidates: List<KoreanPhraseChunk>): List<KoreanPhraseChunk> =
        distinctPhrases(candidates.filter { isProperPhraseChunk(it) })
}
