package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.collections.eclipse.multi.listMultimapOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.korean.stemmer.KoreanStemmer
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider.koreanDictionary
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Unknown
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPosx
import io.bluetape4k.tokenizer.korean.utils.KoreanSubstantive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.eclipse.collections.api.multimap.MutableMultimap

/**
 * 한국어 문장을 형태소 토큰 열로 분석하는 기본 토크나이저입니다.
 *
 * ## 동작/계약
 * - 입력은 먼저 `KoreanChunker.chunk`로 청크 분할되고, `Korean` 품사 청크만 동적 계획법 파서를 적용한다.
 * - 후보 분석 결과는 각 청크별로 최대 `topN`개를 반환하고, `KoreanSubstantive.collapseNouns`로 명사 연쇄를 보정한다.
 * - 최종 `tokenize`는 `KoreanStemmer.stem`을 거쳐 용언 원형 정보를 채운다.
 *
 * ```kotlin
 * val tokens = KoreanTokenizer.tokenize("엄청작아서귀엽다")
 * // tokens == [엄청(Adverb), 작아서(Adjective, stem=작다), 귀엽다(Adjective, stem=귀엽다)]
 * ```
 */
object KoreanTokenizer: KLogging() {

    private const val TOP_N_PER_STATE = 5
    private const val MAX_TRACE_BACK = 8

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
     * m Modifier: 관형사 ('초'대박)
     * v VerbPrefix: 동사 접두어 ('쳐'먹어)
     * s Suffix: 접미사 (~적)
     */
    private val sequenceDefinition = mapOf(
        // Substantive
        "D0m*N1s0j0" to Noun,
        // Predicate 초기뻐하다, 와주세요, 초기뻤었고, 추첨하다, 구경하기힘들다, 기뻐하는, 기쁜, 추첨해서, 좋아하다, 걸려있을
        "v*V1r*e0" to Verb,
        "v*J1r*e0" to Adjective,
        // Modifier 부사
        "A1" to Adverb,
        // Standalone
        "C1" to Conjunction,
        "E+" to Exclamation,
        "j1" to Josa
    )

    private val koreanPosTrie by lazy {
        KoreanPosx.getTrie(sequenceDefinition)
    }

    /**
     * 문장을 1-best 분석으로 형태소 분해하고 원형(stem)을 보정한 토큰 리스트를 반환합니다.
     *
     * ## 동작/계약
     * - `tokenizeTopN(text, 1, profile)` 결과에서 각 청크의 첫 번째 후보만 사용한다.
     * - 비한글 청크(`Space`, `Punctuation` 등)는 원래 청크 토큰을 그대로 유지한다.
     * - `KoreanTokenizerTest` 기준으로 `"주말특가"`는 `[주말(Noun), 특가(Noun)]`로 분해된다.
     *
     * ```kotlin
     * val tokens = KoreanTokenizer.tokenize("야이건뭐")
     * // tokens == [야(Exclamation), 이건(Noun), 뭐(Noun)]
     * ```
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        val tokenized = tokenizeTopN(text, 1, profile)
            .flatMap { it.firstOrNull() ?: emptyList() }

        return KoreanStemmer.stem(tokenized)
    }

    /**
     * 문장을 청크별 상위 `topN` 후보 분석 결과로 반환합니다.
     *
     * ## 동작/계약
     * - 반환 타입은 `List<청크, List<후보, List<KoreanToken>>>` 구조다.
     * - 파싱 중 예외가 발생하면 `TokenizerException("Error tokenizing a chunk: $text", cause)`로 감싸서 던진다.
     * - `KoreanTokenizerTest`에서 사용자 사전 추가 전/후 결과가 달라지는 경로는 이 함수의 후보 생성 결과를 따른다.
     *
     * ```kotlin
     * val top = KoreanTokenizer.tokenizeTopN("가느다란", topN = 1)
     * // top.isNotEmpty() == true
     * ```
     */
    fun tokenizeTopN(
        text: CharSequence,
        topN: Int = 1,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<List<List<KoreanToken>>> {
        try {
            return KoreanChunker.chunk(text).map {
                when (it.pos) {
                    Korean -> {
                        // Get the best parse of each chunk
                        runBlocking(Dispatchers.IO) {
                            val parsed = parseKoreanChunk(it, profile, topN)

                            // Collapse sequence of one-char nouns into one unknown noun: (가Noun 회Noun -> 가회Noun*)
                            parsed.map(KoreanSubstantive::collapseNouns)
                        }
                    }

                    else -> listOf(listOf(it))
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Error tokenizing a chunk: $text" }
            throw TokenizerException("Error tokenizing a chunk: $text", e)
        }
    }

    /**
     * Find the best parse using dynamic programming.
     *
     * @param chunk Input chunk. The input has to be entirely. Check for input validity is skipped
     *              for performance optimization. This method is private and is called only by tokenize.
     * @return The best possible parse.
     */
    private fun parseKoreanChunk(
        chunk: KoreanToken,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
        topN: Int = 1,
    ): List<List<KoreanToken>> {
        return findTopCandidates(chunk, profile).take(topN)
    }

    private fun findTopCandidates(
        chunk: KoreanToken,
        profile: TokenizerProfile,
    ): List<List<KoreanToken>> {
        val directMatch = findDirectMatch(chunk)

        // Buffer for solution
        val candidateParse = CandidateParse(
            parse = ParsedChunk(listOf(), 1, profile),
            curTrie = koreanPosTrie,
            ending = null
        )
        val solutions = listMultimapOf<Int, CandidateParse>()
            .apply {
                put(0, candidateParse)
            }
        // val solutions = hashMapOf(0 to candidateParse)

        // Find N best parses per state
        for (end in 1..chunk.length) {
            for (start in end - 1 downTo maxOf(end - MAX_TRACE_BACK, 0)) {

                val word = chunk.text.slice(start until end)
                // log.trace { "chunk text=${chunk.text}, start=$start, end=$end, word=$word" }

                // Removing unused solutions from solutions hashmap as the chunk is getting processed
                removeUnusedSolutions(start, end, solutions)

                val curSolutions = solutions[start]!!
                // log.trace { "chunk=${chunk.text} word=$word, curSolutions=${curSolutions.joinToString()}" }

                val candidates: List<CandidateParse> = curSolutions.flatMap { solution: CandidateParse ->
                    val possiblePoses: List<PossibleTrie> = solution.ending
                        ?.let {
                            solution.curTrie.map { PossibleTrie(it, 0) } + koreanPosTrie.map { PossibleTrie(it, 1) }
                        }
                        ?: solution.curTrie.map { PossibleTrie(it, 0) }

                    possiblePoses
                        .filter {
                            it.curTrie.curPos == Noun ||
                                    (koreanDictionary[it.curTrie.curPos]?.contains(word.toCharArray()) == true)
                        }
                        .map { t: PossibleTrie ->
                            // log.trace { "word=$word, trie=${t.curTrie}, pos=${t.curTrie.curPos}" }
                            val candidateToAdd =
                                if (t.curTrie.curPos == Noun && koreanDictionary[Noun]?.contains(word.toCharArray()) == false) {
                                    val isWordName: Boolean = KoreanSubstantive.isName(word)
                                    val isKoreanNumber = KoreanSubstantive.isKoreanNumber(word)
                                    val isWordKoreanNameVariation = KoreanSubstantive.isKoreanNameVariation(word)

                                    val unknown = !(isWordName || isKoreanNumber || isWordKoreanNameVariation)
                                    val pos = Noun

                                    val token =
                                        KoreanToken(word, pos, chunk.offset + start, word.length, unknown = unknown)
                                    ParsedChunk(listOf(token), t.words, profile)
                                } else {
                                    val pos = t.curTrie.curPos ?: Unknown
                                    val token = KoreanToken(word, pos, chunk.offset + start, word.length)
                                    ParsedChunk(listOf(token), t.words, profile)
                                }
                            // log.trace { "candidateToAdd=$candidateToAdd" }

                            val nextTrie = t.curTrie.nextTrie
                                ?.map { if (it == KoreanPosx.SelfNode) t.curTrie else it }
                                ?.toList()
                                ?: emptyList()

                            CandidateParse(solution.parse + candidateToAdd, nextTrie, t.curTrie.ending)
                        }
                }

                val currentSolutions = solutions[end] ?: emptyList()

                val parses = (currentSolutions + candidates)
                    .sortedWith(compareBy({ it.parse.score }, { it.parse.posTieBreaker }))
                    .take(TOP_N_PER_STATE)

                solutions.removeAll(end)
                solutions.putAll(end, parses)
            }
        }

        val topCandidates =
            if (solutions[chunk.length]!!.isEmpty()) {
                val token = KoreanToken(chunk.text, Noun, 0, chunk.length, unknown = true)
                listOf(listOf(token))
            } else {
                solutions[chunk.length]!!.sortedBy { it.parse.score }.map { it.parse.posNodes }
            }

        return (directMatch + topCandidates).distinct()
    }

    private fun removeUnusedSolutions(
        start: Int,
        end: Int,
        solutions: MutableMultimap<Int, CandidateParse>,
    ): MutableMultimap<Int, CandidateParse> {
        // Make sure the solutions hashmap won't have references to unused objects...
        if (end > MAX_TRACE_BACK && start + 1 == end) {
            log.trace { "remove solution. index=${end - MAX_TRACE_BACK - 1}" }
            solutions.removeAll(end - MAX_TRACE_BACK - 1)
        }
        return solutions
    }

    private fun findDirectMatch(chunk: KoreanToken): List<List<KoreanToken>> {
        log.trace { "Find direct match. chunk=$chunk" }
        return koreanDictionary.entries
            .firstOrNull { (_, dict) ->
                dict.contains(chunk.text)
            }
            ?.let { (pos, _) -> listOf(listOf(chunk.copy(pos = pos))) }
            ?: emptyList()
    }
}
