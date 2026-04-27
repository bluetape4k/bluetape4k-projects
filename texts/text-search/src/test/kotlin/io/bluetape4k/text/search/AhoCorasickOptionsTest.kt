package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [SearchOptions] 매트릭스 검증 테스트.
 *
 * spec §7.2 옵션 조합을 개별 테스트로 커버한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickOptionsTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── Test 1 ────────────────────────────────

    @Test
    fun `ignoreCase=false allowOverlaps=true NONE NONE - 모든 매치 포함`() {
        // Arrange: 기본 옵션 — 대소문자 구분, 겹침 허용
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("she", "SHE")
            .add("his", "HIS")
            .add("hers", "HERS")
            .options(
                SearchOptions(
                    ignoreCase = false,
                    allowOverlaps = true,
                    wordBoundary = WordBoundary.NONE,
                    normalization = NormalizationForm.NONE,
                )
            )
            .build()

        // Act
        val matches = automaton.parseText("ushers")

        // Assert: "ushers" 에서 she(1), he(2), hers(2) 가 매치됨
        matches.shouldNotBeEmpty()
        val keywords = matches.map { it.keyword }.toSet()
        keywords.contains("she").shouldBeTrue()
        keywords.contains("he").shouldBeTrue()
        keywords.contains("hers").shouldBeTrue()
        log.debug { "기본 옵션 매치: $matches" }
    }

    // ──────────────────────────────── Test 2 ────────────────────────────────

    @Test
    fun `ignoreCase=true - 대소문자 무시`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .add("banana", "BANANA")
            .options(SearchOptions(ignoreCase = true))
            .build()

        // Act
        val matches = automaton.parseText("APPLE and Banana")

        // Assert: 대소문자 무관하게 2건 매치
        matches shouldHaveSize 2
        val keywords = matches.map { it.keyword }.toSet()
        // ignoreCase=true 시 키워드는 소문자로 정규화됨
        keywords.contains("apple").shouldBeTrue()
        keywords.contains("banana").shouldBeTrue()
        log.debug { "ignoreCase=true 매치: $matches" }
    }

    // ──────────────────────────────── Test 3 ────────────────────────────────

    @Test
    fun `allowOverlaps=false - 더 긴 keyword 우선 (hotel 케이스)`() {
        // Arrange: "hot"과 "hotel" 둘 다 등록 — "hotel" 텍스트에서 겹침 제거 시 "hotel"이 우선
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = false))
            .build()

        // Act
        val matches = automaton.parseText("hotel")

        // Assert: "hotel"만 살아남아야 함 (더 긴 키워드 우선)
        matches shouldHaveSize 1
        matches[0].keyword shouldBeEqualTo "hotel"
        matches[0].value shouldBeEqualTo "HOTEL"
        log.debug { "allowOverlaps=false hotel 케이스: $matches" }
    }

    // ──────────────────────────────── Test 4 ────────────────────────────────

    @Test
    fun `wordBoundary=LATIN_ALPHA - 부분 단어 제외`() {
        // Arrange: "apple"을 LATIN_ALPHA 경계로만 매치 — "pineapple" 내부에서는 매치 안 됨
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .options(SearchOptions(wordBoundary = WordBoundary.LATIN_ALPHA))
            .build()

        // Act
        val noMatch = automaton.parseText("pineapple")
        val matched = automaton.parseText("eat apple now")

        // Assert: 합성어 내부는 매치 안 됨
        noMatch shouldHaveSize 0

        // 단독 단어는 매치됨
        matched shouldHaveSize 1
        matched[0].keyword shouldBeEqualTo "apple"
        log.debug { "LATIN_ALPHA 경계 검증 — noMatch: $noMatch, matched: $matched" }
    }

    // ──────────────────────────────── Test 5 ────────────────────────────────

    @Test
    fun `wordBoundary=WHITESPACE_SEPARATED - 공백 경계만 매치`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("run", "RUN")
            .options(SearchOptions(wordBoundary = WordBoundary.WHITESPACE_SEPARATED))
            .build()

        // Act
        val noMatch = automaton.parseText("running fast")    // 뒤에 문자가 붙어 있음
        val matched = automaton.parseText("please run now")  // 공백으로 분리된 단독 단어

        // Assert
        noMatch shouldHaveSize 0
        matched shouldHaveSize 1
        matched[0].keyword shouldBeEqualTo "run"
        log.debug { "WHITESPACE_SEPARATED 경계 검증 — noMatch: $noMatch, matched: $matched" }
    }

    // ──────────────────────────────── Test 6 ────────────────────────────────

    @Test
    fun `stopOnFirstMatch=true - 첫 매치 후 중단`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("one", "1")
            .add("two", "2")
            .add("three", "3")
            .options(SearchOptions(stopOnFirstMatch = true))
            .build()

        // Act
        val matches = automaton.parseText("one two three")

        // Assert: stopOnFirstMatch=true이므로 정확히 1건만 반환
        matches shouldHaveSize 1
        matches[0].keyword shouldBeEqualTo "one"
        log.debug { "stopOnFirstMatch=true 결과: $matches" }
    }

    // ──────────────────────────────── Test 7 ────────────────────────────────

    @Test
    fun `stopOnFirstMatch=true는 blocking parseText API에서 첫 매치만 반환함`() {
        // Arrange: stopOnFirstMatch=true 옵션으로 automaton 생성
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("alpha", "ALPHA")
            .add("beta", "BETA")
            .add("gamma", "GAMMA")
            .options(SearchOptions(stopOnFirstMatch = true))
            .build()

        val allOpts = AhoCorasickAutomaton.builder<String>()
            .add("alpha", "ALPHA")
            .add("beta", "BETA")
            .add("gamma", "GAMMA")
            .build()

        val text = "alpha beta gamma"

        // Act
        val stoppedMatches = automaton.parseText(text)
        val allMatches = allOpts.parseText(text)

        // Assert: stopOnFirstMatch 버전은 1건만 반환
        stoppedMatches shouldHaveSize 1
        // 전체 매치 버전은 3건 반환
        allMatches shouldHaveSize 3
        // 첫 매치는 동일해야 함
        stoppedMatches[0].keyword shouldBeEqualTo allMatches[0].keyword
        log.debug { "stopOnFirstMatch 비교 — stopped: $stoppedMatches, all: $allMatches" }
    }
}
