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
 * 입력 문자열을 품사 패턴에 따라 `KoreanToken` 청크로 분리합니다.
 *
 * ## 동작/계약
 * - URL/Email/해시태그/숫자/한글 순서로 패턴을 적용해 겹치지 않는 매치를 선택한다.
 * - 패턴으로 매치되지 않은 구간은 `Foreign` 품사로 채운다.
 * - 공백은 연속 구간 단위로 유지되며 `Space` 품사를 갖는다.
 *
 * ```kotlin
 * val chunks = KoreanChunker.getChunks("안녕? iphone6안녕? 세상아?")
 * // chunks == ["안녕", "?", " ", "iphone", "6", "안녕", "?", " ", "세상아", "?"]
 * ```
 */
object KoreanChunker: KLogging() {

    /**
     * 청킹에 사용하는 품사별 정규식/패턴 맵입니다.
     *
     * ## 동작/계약
     * - `findAllPatterns`와 `splitChunks`에서 동일한 패턴 소스를 참조한다.
     * - `KoreanChunkerTest`의 URL/Email/Hashtag/Number 추출 케이스를 통과하는 패턴 집합이다.
     *
     * ```kotlin
     * val matcher = KoreanChunker.POS_PATTERNS[KoreanPos.URL]!!.matcher("https://openkoreantext.org")
     * val found = matcher.find()
     * // found == true
     * ```
     */
    val POS_PATTERNS: Map<KoreanPos, Pattern> = mapOf(
        Korean to """([가-힣]+)""".toRegex().toPattern(),
        Alpha to """(\p{Alpha}+)""".toRegex().toPattern(),
        Number to ("""(\$?\p{Digit}+""" +
                """(,\p{Digit}{3})*([/~:\.-]\p{Digit}+)?""" +
                """(천|만|억|조)*(%|원|달러|위안|옌|엔|유로|등|년|월|일|회|시간|시|분|초)?)""").toRegex().toPattern(),
        KoreanParticle to """([ㄱ-ㅣ]+)""".toRegex().toPattern(),
        Punctuation to """([\p{Punct}·…’]+)""".toRegex().toPattern(),
        URL to com.twitter.twittertext.Regex.VALID_URL,
        Email to """([\p{Alnum}.\-_]+@[\p{Alnum}.]+)""".toRegex().toPattern(),
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
     * 문자열을 청크 텍스트 리스트로 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `chunk(input)`을 호출한 뒤 토큰 `text`만 추출한다.
     * - `keepSpace=false`이면 각 청크 문자열에 `trim()`을 적용한다.
     * - `KoreanChunkerTest` 기준으로 `"안녕? iphone6안녕? 세상아?"`를 10개 청크로 분리한다.
     *
     * ```kotlin
     * val chunks = KoreanChunker.getChunks("안녕? iphone6안녕? 세상아?")
     * // chunks == ["안녕", "?", " ", "iphone", "6", "안녕", "?", " ", "세상아", "?"]
     * ```
     */
    fun getChunks(input: String, keepSpace: Boolean = true): List<String> {
        return chunk(input).map { if (keepSpace) it.text else it.text.trim() }
    }

    /**
     * 패턴 매치 구간과 품사를 함께 보관하는 청크 매치 정보입니다.
     *
     * ## 동작/계약
     * - `range`는 `start..end`를 그대로 노출한다.
     * - `disjoint`는 두 매치의 겹침 여부를 시작/끝 인덱스로 판별한다.
     *
     * ```kotlin
     * val left = KoreanChunker.ChunkMatch(0, 2, "안녕", KoreanPos.Korean)
     * val right = KoreanChunker.ChunkMatch(2, 3, "?", KoreanPos.Punctuation)
     * // left.disjoint(right) == true
     * ```
     */
    data class ChunkMatch(
        val start: Int,
        val end: Int,
        val text: String,
        val pos: KoreanPos,
    ) {
        /**
         * 매치 시작/끝을 포함하는 인덱스 범위입니다.
         *
         * ## 동작/계약
         * - 생성자에 전달한 `start`, `end`를 그대로 사용해 계산한다.
         *
         * ```kotlin
         * val range = KoreanChunker.ChunkMatch(3, 5, "abc", KoreanPos.Alpha).range
         * // range == 3..5
         * ```
         */
        val range: IntRange = start..end

        /**
         * 두 매치가 서로 겹치지 않는지 확인합니다.
         *
         * ## 동작/계약
         * - 현재 매치의 앞에서 끝나거나, 현재 매치 뒤에서 시작하면 `true`를 반환한다.
         * - 경계가 맞닿는 경우는 겹치지 않는 것으로 본다.
         *
         * ```kotlin
         * val a = KoreanChunker.ChunkMatch(0, 2, "ab", KoreanPos.Alpha)
         * val b = KoreanChunker.ChunkMatch(2, 3, "c", KoreanPos.Alpha)
         * // a.disjoint(b) == true
         * ```
         */
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
     * 주어진 `Matcher`에서 모든 매치를 역순 누적 후 원래 순서로 반환합니다.
     *
     * ## 동작/계약
     * - `tailrec`으로 구현되어 각 매치를 `matches`의 앞에 삽입한다.
     * - `Matcher.find()`가 더 이상 성공하지 않으면 누적 리스트를 반환한다.
     * - `KoreanChunkerTest` 기준으로 URL/Hashtag 다중 매치를 모두 수집한다.
     *
     * ```kotlin
     * val matcher = KoreanChunker.POS_PATTERNS[KoreanPos.Email]!!.matcher("문의: a@b.com")
     * val matches = KoreanChunker.findAllPatterns(matcher, KoreanPos.Email)
     * // matches.first().text == "a@b.com"
     * ```
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

                cm.start > prevEnd -> {
                    val cm2 = ChunkMatch(prevEnd, cm.start, text.slice(prevEnd until cm.start), pos)
                    chunksWithForeign.add(0, cm2)
                    chunksWithForeign.add(0, cm)
                    cm.end
                }

                else               ->
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
     * 분리된 청크 중 지정 품사에 해당하는 토큰만 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `chunk(input)` 결과에서 `it.pos == pos`만 필터링한다.
     * - `KoreanChunkerTest` 기준으로 URL/Email/Hashtag/CashTag 추출을 지원한다.
     *
     * ```kotlin
     * val urls = KoreanChunker.getChunksByPos("openkoreantext.org에서 확인", KoreanPos.URL)
     * // urls.map { it.text } == ["openkoreantext.org"]
     * ```
     */
    fun getChunksByPos(input: String, pos: KoreanPos): List<KoreanToken> {
        return chunk(input).filter { it.pos == pos }
    }

    /**
     * 입력 문자열을 청크 단위 `KoreanToken` 목록으로 분할합니다.
     *
     * ## 동작/계약
     * - 공백 보존 분할 후 각 세그먼트를 `splitChunks`로 분석해 순서대로 `KoreanToken`을 만든다.
     * - 토큰 offset은 원문에서 `indexOf(m.text, i)`로 계산해 누적 위치를 갱신한다.
     * - `KoreanChunkerTest` 기준으로 `"중·고등학교에서…"`를 `[중, ·, 고등학교에서, …]` 4개 토큰으로 반환한다.
     *
     * ```kotlin
     * val tokens = KoreanChunker.chunk("중·고등학교에서…")
     * // tokens.map { it.text } == ["중", "·", "고등학교에서", "…"]
     * ```
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
