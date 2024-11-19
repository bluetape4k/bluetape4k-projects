package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Alpha
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.CashTag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Email
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Foreign
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Hashtag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Number
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Punctuation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ScreenName
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Space
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.URL
import io.bluetape4k.tokenizer.korean.utils.isSpaceChar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 한글 문장을 어절(chunk)로 분리하는 유틸리티 클래스입니다.
 *
 * ```
 * KoreanChunker.getChunks("안녕? iphone6안녕? 세상아?")  // ["안녕", "?", " ", "iphone", "6", "안녕", "?", " ", "세상아", "?"]
 * ```
 */
object KoreanChunker: KLogging() {

    val POS_PATTERNS: Map<KoreanPos, Pattern> = mapOf(
        Korean to """([가-힣]+)""".toRegex().toPattern(),
        Alpha to """(\p{Alpha}+)""".toRegex().toPattern(),
        Number to ("""(\$?\p{Digit}+""" +
                """(,\p{Digit}{3})*([/~:\.-]\p{Digit}+)?""" +
                """(천|만|억|조)*(%|원|달러|위안|옌|엔|유로|등|년|월|일|회|시간|시|분|초)?)""").toRegex().toPattern(),
        KoreanParticle to """([ㄱ-ㅣ]+)""".toRegex().toPattern(),
        Punctuation to """([\p{Punct}·…’]+)""".toRegex().toPattern(),
        URL to com.twitter.twittertext.Regex.VALID_URL,
        Email to """([\p{Alnum}.\-_]+@[\p{Alnum}\.]+)""".toRegex().toPattern(),
        Hashtag to com.twitter.twittertext.Regex.VALID_HASHTAG,
        ScreenName to com.twitter.twittertext.Regex.VALID_MENTION_OR_LIST,
        CashTag to com.twitter.twittertext.Regex.VALID_CASHTAG,
        Space to """\s+""".toRegex().toPattern()
    )

    private val CHUNKING_ORDER: List<KoreanPos> = listOf(
        URL,
        Email,
        ScreenName,
        Hashtag,
        CashTag,
        Number,
        Korean,
        KoreanParticle,
        Alpha,
        Punctuation
    )

    /**
     * [input] 문자열을 어절(chunk)로 분리합니다.
     *
     * ```
     * getChunks("안녕? iphone6안녕? 세상아?")  // ["안녕", "?", " ", "iphone", "6", "안녕", "?", " ", "세상아", "?"]
     *
     * getChunks("#해쉬태그 이라는 것 #hash @hello 123 이런이런 #여자최애캐_5명으로_취향을_드러내자")
     * // ["#해쉬태그", " ", "이라는", " ", "것", " ", "#hash", " ", "@hello", " ", "123", " ", "이런이런", " ", "#여자최애캐_5명으로_취향을_드러내자"]
     * ```
     */
    fun getChunks(input: String, keepSpace: Boolean = true): List<String> {
        return chunk(input).map { if (keepSpace) it.text else it.text.trim() }.toList()
    }

    data class ChunkMatch(
        val start: Int,
        val end: Int,
        val text: String,
        val pos: KoreanPos,
    ) {
        val range: IntRange = start..end

        fun disjoint(that: ChunkMatch): Boolean {
            return (that.start < this.start && that.end <= this.start) ||
                    (that.start >= this.end && that.end > this.end)
        }
    }

    private fun splitBySpaceKeepingSpace(s: CharSequence): Flow<String> = flow {
        val space = POS_PATTERNS[Space]!!
        val m = space.matcher(s)
        var index = 0

        while (m.find()) {
            if (index < m.start()) {
                emit(s.subSequence(index, m.start()).toString())
            }
            emit(s.subSequence(m.start(), m.end()).toString())
            index = m.end()
        }

        if (index < s.length) {
            emit(s.subSequence(index, s.length).toString())
        }
    }

    /**
     * Recursively call m.find() to find all the matches.
     * 모든 매치가 찾아질 때까지 `m.find()` 를 재귀적으로 호출합니다.
     * `tail-recursion` 최적화를 사용하여 stack overflow를 피합니다.
     *
     * URL 추출
     * ```
     * var actual = findAllPatterns(
     *      getPatternMatcher(URL, "스팀(http://store.steampowered.com)에서 드디어 여름세일을 시작합니다."),
     *      URL
     * )  // [ChunkMatch(2, 32, "(http://store.steampowered.com", URL)]
     * ```
     *
     * HASH_TAG 찾기
     * ```
     * actual = findAllPatterns(getPatternMatcher(Hashtag, "구글에는 정말로 이쁜 자전거가 있다. #Google #이쁜자전거 #갖고싶다"), Hashtag)
     * actual shouldBeEqualTo listOf(
     *     ChunkMatch(start = 35, end = 41, text = " #갖고싶다", pos = Hashtag),
     *     ChunkMatch(start = 28, end = 35, text = " #이쁜자전거", pos = Hashtag),
     *     ChunkMatch(start = 20, end = 28, text = " #Google", pos = Hashtag)
     * )
     * ```
     *
     *
     * @param m input Matcher
     * @param pos KoreanPos to attach
     * @param matches ouput list of ChunkMatch
     * @return list of ChunkMatches
     */
    tailrec fun findAllPatterns(
        m: Matcher,
        pos: KoreanPos,
        matches: MutableList<ChunkMatch> = mutableListOf(),
    ): List<ChunkMatch> {
        return if (m.find()) {
            matches.add(0, ChunkMatch(m.start(), m.end(), m.group(), pos))
            findAllPatterns(m, pos, matches)
        } else {
            matches
        }
    }

    private fun splitChunks(text: String): List<ChunkMatch> {
        return if (text.isNotEmpty() && text[0].isSpaceChar) {
            listOf(ChunkMatch(0, text.length, text, Space))
        } else {
            val chunksBuf = mutableListOf<ChunkMatch>()
            var matchedLen = 0
            CHUNKING_ORDER.forEach { pos ->
                if (matchedLen < text.length) {
                    val m: Matcher = POS_PATTERNS[pos]!!.matcher(text)
                    while (m.find()) {
                        val cm = ChunkMatch(m.start(), m.end(), m.group(), pos)
                        if (chunksBuf.all { cm.disjoint(it) }) {
                            chunksBuf += cm
                            matchedLen += cm.end - cm.start
                        }
                    }
                }
            }

            val sorted = chunksBuf.sortedBy { it.start }
            fillInUnmatched(text, sorted, Foreign)
        }
    }

    /**
     * Fill in unmatched segments with given pos
     *
     * @param text input text
     * @param chunks matched chunks
     * @param pos KoreanPos to attach to the unmatched chunk
     * @return list of ChunkMatches
     */
    private fun fillInUnmatched(
        text: String,
        chunks: List<ChunkMatch>,
        pos: KoreanPos,
    ): List<ChunkMatch> {
        val chunksWithForeign = mutableListOf<ChunkMatch>()
        var prevEnd = 0

        chunks.forEach { cm ->
            prevEnd = when {
                cm.start == prevEnd -> {
                    chunksWithForeign.add(0, cm)
                    cm.end
                }

                cm.start > prevEnd  -> {
                    val cm2 = ChunkMatch(prevEnd, cm.start, text.slice(prevEnd until cm.start), pos)
                    chunksWithForeign.add(0, cm2)
                    chunksWithForeign.add(0, cm)
                    cm.end
                }

                else                ->
                    error("Non-disjoint chunk matches found. cm=$cm")
            }
        }

        if (prevEnd < text.length) {
            val cm = ChunkMatch(prevEnd, text.length, text.slice(prevEnd until text.length), pos)
            chunksWithForeign.add(0, cm)
        }

        return chunksWithForeign.reversed()
    }

    /**
     * Get chunks by given pos.
     *
     * @param input input string
     * @param pos one of supported KoreanPos's: URL, Email, ScreenName, Hashtag,
     *            CashTag, Korean, KoreanParticle, Number, Alpha, Punctuation
     * @return sequence of Korean chunk strings
     */
    fun getChunksByPos(input: String, pos: KoreanPos): List<KoreanToken> {
        return chunk(input).toList().filter { it.pos == pos }
    }

    /**
     * Split input text into a sequnce of KoreanToken. A candidate for Korean parser
     * gets tagged with KoreanPos.Korean.
     *
     * ```
     * chunk("중·고등학교에서…") shouldBeEqualTo listOf(
     *     KoreanToken("중", Korean, 0, 1),
     *     KoreanToken("·", Punctuation, 1, 1),
     *     KoreanToken("고등학교에서", Korean, 2, 6),
     *     KoreanToken("…", Punctuation, 8, 1)
     * )
     * ```
     *
     * @param input input string
     * @return sequence of KoreanTokens
     */
    fun chunk(input: CharSequence): List<KoreanToken> {
        val s = input.toString()

        // fold 대신 forEach 구문을 이용하여, 메모리를 절약하도록 했다
        val tokens = mutableListOf<KoreanToken>()
        var i = 0

        runBlocking(Dispatchers.Default) {
            splitBySpaceKeepingSpace(s)
                .flatMapConcat { splitChunks(it).asFlow() }
                .collect { m ->
                    val segStart = s.indexOf(m.text, i)
                    tokens.add(0, KoreanToken(m.text, m.pos, segStart, m.text.length))
                    i = segStart + m.text.length
                }
        }
        return tokens.reversed()
    }
}
