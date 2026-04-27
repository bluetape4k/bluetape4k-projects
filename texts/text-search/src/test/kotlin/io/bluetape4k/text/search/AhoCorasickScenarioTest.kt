package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.text.search.flow.matchesAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * spec §7.7 실전 시나리오 검증 테스트.
 *
 * 1. 금칙어 검열
 * 2. 자동완성 사전
 * 3. 로그 키워드 알람 (Flow + take(1))
 * 4. URL 스킴 추출
 * 5. 코드 키워드 highlight
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickScenarioTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── 시나리오 1: 금칙어 검열 ────────────────────────────────

    @Test
    fun `금칙어 검열 - replaceAll로 마스킹 후 출력에 욕설 0건`() {
        // Arrange: 한글 욕설 키워드 6개 등록
        val profanity = listOf("바보", "멍청이", "못난이", "찐따", "쓸모없어", "꺼져")
        val automaton = AhoCorasickAutomaton.builder<String>()
            .apply { profanity.forEach { add(it, "[검열됨]") } }
            .options(SearchOptions(normalization = NormalizationForm.NFC))
            .build()

        val text = "너는 바보야! 멍청이처럼 굴지 마. 못난이 같으니라고. 그냥 꺼져."

        // Act: 금칙어를 "[검열됨]"으로 치환
        val masked = automaton.replaceAll(text) { match -> match.value }

        // Assert: 마스킹 결과에 금칙어가 하나도 없어야 함
        val matchesInMasked = automaton.parseText(masked)
        matchesInMasked shouldHaveSize 0
        // 원본에서는 매치가 있어야 함
        val originalMatches = automaton.parseText(text)
        originalMatches.shouldNotBeEmpty()
        log.debug { "금칙어 마스킹 결과: '$masked'" }
        log.debug { "원본 매치 수: ${originalMatches.size}" }
    }

    // ──────────────────────────────── 시나리오 2: 자동완성 사전 ────────────────────────────────

    @Test
    fun `자동완성 사전 - 제품명 20개 등록 후 긴 문장에서 추출 distinct + 정렬 검증`() {
        // Arrange: 제품명 20개
        val products = listOf(
            "Apple", "Microsoft", "Samsung", "Google", "Amazon",
            "Meta", "Netflix", "Tesla", "Intel", "AMD",
            "Nvidia", "Oracle", "IBM", "Sony", "LG",
            "Huawei", "Xiaomi", "Qualcomm", "Broadcom", "TSMC",
        )
        val automaton = AhoCorasickAutomaton.builder<String>()
            .apply { products.forEach { add(it, it) } }
            .options(SearchOptions(ignoreCase = true))
            .build()

        // 여러 제품명이 반복 등장하는 긴 문장
        val text = """
            Apple and Microsoft are tech giants. Samsung and Apple compete in smartphones.
            Google owns Android while Microsoft owns Windows. Amazon, Meta, and Netflix
            dominate cloud and streaming. Tesla, Intel, AMD, Nvidia are hardware leaders.
            Oracle, IBM, Sony, LG, Huawei, Xiaomi, Qualcomm, Broadcom, TSMC round out the list.
        """.trimIndent()

        // Act
        val matches = automaton.parseText(text)
        val distinctSorted = matches.map { it.value }.distinct().sorted()

        // Assert: 20개 제품 모두 등장. value는 원본 대소문자 그대로 ("Apple", "AMD" 등)
        // 대소문자 무시 정렬로 비교
        val distinctSortedCI = matches.map { it.value }.distinct().sortedBy { it.lowercase() }
        val expectedSorted = products.sortedBy { it.lowercase() }
        distinctSortedCI shouldBeEqualTo expectedSorted
        log.debug { "자동완성 사전 distinct+sorted: $distinctSorted" }
    }

    // ──────────────────────────────── 시나리오 3: 로그 키워드 알람 (Flow + take(1)) ────────────────────────────────

    @Test
    fun `로그 키워드 알람 - Flow take(1)로 첫 매치만 가져오기`() = runTest(timeout = 30.seconds) {
        // Arrange: 로그 레벨 키워드 등록
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("ERROR", "ALERT_ERROR")
            .add("WARN", "ALERT_WARN")
            .add("FATAL", "ALERT_FATAL")
            .build()

        val logLine = "2026-04-26 INFO Starting app... WARN disk low ERROR disk full FATAL system halt"

        // Act: Flow로 매치를 emit하되 take(1)로 첫 번째 경보만 수집
        val firstAlert = automaton.matchesAsFlow(logLine)
            .take(1)
            .toList()

        // Assert: 정확히 1건만 수집
        firstAlert shouldHaveSize 1
        // "WARN"이 텍스트 내 첫 번째로 나타나야 함
        firstAlert[0].keyword shouldBeEqualTo "WARN"
        firstAlert[0].value shouldBeEqualTo "ALERT_WARN"
        log.debug { "첫 번째 로그 알람: ${firstAlert[0]}" }
    }

    // ──────────────────────────────── 시나리오 4: URL 스킴 추출 ────────────────────────────────

    @Test
    fun `URL 스킴 추출 - http ftp https 스킴의 start 위치 검증`() {
        // Arrange: URL 스킴 키워드 등록 (wordBoundary=NONE — URL 중간에 나타나도 매치)
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("http://", "HTTP")
            .add("https://", "HTTPS")
            .add("ftp://", "FTP")
            .options(SearchOptions(wordBoundary = WordBoundary.NONE))
            .build()

        val text = "Visit http://example.com and https://secure.org or ftp://files.net"
        // "http://" starts at 6, "https://" at 31, "ftp://" at 51

        // Act
        val matches = automaton.parseText(text)

        // Assert: 3개의 URL 스킴이 검출되어야 함
        matches shouldHaveSize 3
        val byKeyword = matches.associateBy { it.keyword }

        // http:// 시작 위치 검증
        byKeyword["http://"].shouldNotBeNull()
        text.substring(byKeyword["http://"]!!.start, byKeyword["http://"]!!.end + 1) shouldBeEqualTo "http://"

        // https:// 시작 위치 검증
        byKeyword["https://"].shouldNotBeNull()
        text.substring(byKeyword["https://"]!!.start, byKeyword["https://"]!!.end + 1) shouldBeEqualTo "https://"

        // ftp:// 시작 위치 검증
        byKeyword["ftp://"].shouldNotBeNull()
        text.substring(byKeyword["ftp://"]!!.start, byKeyword["ftp://"]!!.end + 1) shouldBeEqualTo "ftp://"

        log.debug { "URL 스킴 추출 결과: $matches" }
    }

    // ──────────────────────────────── 시나리오 5: 코드 키워드 highlight ────────────────────────────────

    @Test
    fun `코드 키워드 highlight - Kotlin 예약어 10개 tokenize 후 HTML 변환`() {
        // Arrange: Kotlin 예약어 10개 등록
        val keywords = listOf("val", "var", "fun", "class", "when", "if", "for", "while", "return", "object")
        val automaton = AhoCorasickAutomaton.builder<String>()
            .apply { keywords.forEach { add(it, it) } }
            .options(
                SearchOptions(
                    wordBoundary = WordBoundary.LATIN_ALPHA,  // 예약어는 단어 경계에서만 매치
                )
            )
            .build()

        val code = "fun greet() { val name = \"world\"; return name }"

        // Act: tokenize 후 HTML 변환
        val tokens = automaton.tokenize(code)
        val html = buildString {
            tokens.forEach { token ->
                when (token) {
                    is SearchToken.Match -> append("<b>${token.text}</b>")
                    is SearchToken.Fragment -> append(token.text)
                }
            }
        }

        // Assert: Kotlin 예약어 "fun", "val", "return"이 <b>...</b>로 감싸져야 함
        html.contains("<b>fun</b>").shouldBeTrue()
        html.contains("<b>val</b>").shouldBeTrue()
        html.contains("<b>return</b>").shouldBeTrue()

        // tokenize 결과에서 Match 토큰에 Kotlin 예약어가 포함됨을 검증
        val matchedKeywords = tokens
            .filterIsInstance<SearchToken.Match<String>>()
            .map { it.match.keyword }
            .toSet()

        matchedKeywords.contains("fun").shouldBeTrue()
        matchedKeywords.contains("val").shouldBeTrue()
        matchedKeywords.contains("return").shouldBeTrue()

        log.debug { "HTML highlight 결과: $html" }
        log.debug { "매치된 예약어: $matchedKeywords" }
    }
}
