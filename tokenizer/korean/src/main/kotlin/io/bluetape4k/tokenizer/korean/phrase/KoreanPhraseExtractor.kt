package io.bluetape4k.tokenizer.korean.phrase

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.toFastList
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


typealias KoreanPhraseChunk = List<KoreanPhrase>

/**
 * 요즘 주제에 적합한 구를 추출합니다.
 *
 * 1. Collapse sequence of POSes to phrase candidates (초 + 거대 + 기업 + 의 -> 초거대기업 + 의)
 * 2. Find suitable phrases
 *
 * ```
 * val text = """"@user: @user 내가 얼굴이 아가쟈나..ㅎ" 동네사람들...!"""

 * val normalized = KoreanNormalizer.normalize(text)  // 구어체를 정규화
 * val tokens = tokenize(normalized)
 * val phrases = extractPhrases(tokens).joinToString("/")
 * // 얼굴(Noun: 17, 2)/아가(Noun: 21, 2)/동네사람들(Noun: 30, 5)/동네(Noun: 30, 2)/사람들(Noun: 32, 3)
 * ```
 */
object KoreanPhraseExtractor: KLogging() {

    const val MinCharsPerPhraseChunkWithoutSpaces = 2
    const val MinPhrasesPerPhraseChunk = 3

    const val MaxCharsPerPhraseChunkWithoutSpaces = 30
    const val MaxPhrasesPerPhraseChunk = 8

    @JvmField
    val ModifyingPredicateEndings = setOf('ㄹ', 'ㄴ')

    @JvmField
    val ModifyingPredicateExceptions = setOf('만')

    @JvmField
    val PhraseTokens = setOf(Noun, ProperNoun, Space)

    @JvmField
    val ConjunctionJosa = setOf("와", "과", "의")

    @JvmField
    val PhraseHeadPoses = setOf(Adjective, Noun, ProperNoun, Alpha, Number)

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

                i == 0 ->
                    KoreanPhrase(phrase.tokens.dropWhile { it.pos == Space }, phrase.pos)

                i == phrasesToTrim.size - 1 -> {
                    val tokens = phrase.tokens.dropLastWhile { it.pos == Space }
                    KoreanPhrase(tokens, phrase.pos)
                }

                else -> phrase
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

        val phrases = fastListOf<KoreanPhrase>()
        var curTrie: List<KoreanPosTrie?> = collapseTrie

        tokens
            .onEach { token ->
                when {
                    curTrie.any { it?.curPos == token.pos } -> {
                        // Extend the current phase
                        val (ct, nt) = getTries(token, curTrie)

                        if (phrases.isEmpty || curTrie == collapseTrie) {
                            phrases.add(KoreanPhrase(arrayListOf(token), ct?.ending ?: Noun))
                        } else {
                            val newPhrase = KoreanPhrase(phrases.last().tokens + token, ct?.ending ?: Noun)
                            if (phrases.isEmpty) {
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

                    else -> {
                        // Add a single word
                        phrases.add(KoreanPhrase(listOf(token), token.pos))
                        curTrie = collapseTrie
                    }
                }
            }

        return phrases
    }

    private fun distinctPhrases(chunks: List<KoreanPhraseChunk>): List<KoreanPhraseChunk> {
        val phraseChunks = fastListOf<KoreanPhraseChunk>()
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

            val output = fastListOf<KoreanPhrase>()
            val buffer = fastListOf<KoreanPhrase>()

            phrases1
                .onEach { phrase ->
                    if (phrase.pos == Noun || phrase.pos == ProperNoun) {
                        buffer.add(phrase)
                    } else {
                        val tempPhrase =
                            if (buffer.isNotEmpty()) fastListOf(KoreanPhrase(buffer.flatMap { it.tokens }), phrase)
                            else fastListOf(phrase)

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
                buffer.map { it + phrase }.toFastList()

            // NOTE: 현재 이 부분은 변경하면 안됩니다.
            //
            fun newBuffer() = fastListOf(listOf<KoreanPhrase>())

            val output = fastListOf<KoreanPhraseChunk>()
            var buffer = newBuffer()

            phrases1
                .onEach {
                    buffer = if (it.pos in PhraseTokens && isNotSpam(it)) {
                        val bufferWithThisPhrase = addPhraseToBuffer(it, buffer)
                        if (it.pos == Noun || it.pos == ProperNoun) {
                            output.addAll(bufferWithThisPhrase)
                        }
                        bufferWithThisPhrase.toFastList()
                    } else if (isNonNounPhraseCandidate(it)) {
                        addPhraseToBuffer(it, buffer).toFastList()
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
     * 형태소 분석한 결과에서 [filterSpam], [enableHashtags]를 적용한 구문만을 추출합니다.
     *
     * ```
     * val phrases = extractPhrases(tokenize("성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑"), filterSpam = false)
     *
     * val expected = phrases.map { it.text }.distinct()
     * val actual = phrases.map { it.text }
     * actual shouldBeEqualTo expected
     * ```
     *
     * @param tokens A sequence of tokens
     * @param filterSpam true if spam words and slangs to be filtered out
     * @param enableHashtags true if #hashtags to be included
     * @return A list of KoreanPhrase
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
