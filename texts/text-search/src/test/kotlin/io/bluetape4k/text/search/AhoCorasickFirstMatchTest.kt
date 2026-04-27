package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [AhoCorasickAutomaton.firstMatch] R5 leftmost-longest 의미론을 검증하는 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickFirstMatchTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── Test 1 ────────────────────────────────

    @Test
    fun `ushers + he she hers allowOverlaps=true - firstMatch는 she (start=1)`() {
        // Arrange: allowOverlaps=true로 "ushers"에서 she(start=1), he(start=2), hers(start=2) 모두 매치
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("she", "SHE")
            .add("hers", "HERS")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        // Act
        val first = automaton.firstMatch("ushers")

        // Assert: leftmost 기준 she(start=1)이 he/hers(start=2)보다 앞섬
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "she"
        first.start shouldBeEqualTo 1
        first.end shouldBeEqualTo 3
        log.debug { "ushers firstMatch: $first" }
    }

    // ──────────────────────────────── Test 2 ────────────────────────────────

    @Test
    fun `동일 start이면 더 긴 keyword 우선`() {
        // Arrange: "he"와 "hers" 둘 다 start=0 에서 시작 ("hers" 텍스트)
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("he", "HE")
            .add("hers", "HERS")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        // Act
        val first = automaton.firstMatch("hers")

        // Assert: start=0으로 동일 — 더 긴 "hers"(length=4) 가 "he"(length=2) 보다 우선
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "hers"
        first.start shouldBeEqualTo 0
        first.end shouldBeEqualTo 3
        first.length shouldBeEqualTo 4
        log.debug { "동일 start 더 긴 keyword 우선: $first" }
    }

    // ──────────────────────────────── Test 3 ────────────────────────────────

    @Test
    fun `매치 없음 - firstMatch는 null`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .add("banana", "BANANA")
            .build()

        // Act
        val first = automaton.firstMatch("no keywords here")

        // Assert
        first.shouldBeNull()
        log.debug { "매치 없음 → firstMatch=null 검증 완료" }
    }

    // ──────────────────────────────── Test 4 ────────────────────────────────

    @Test
    fun `allowOverlaps=false와 true의 firstMatch 동작 비교`() {
        // Arrange
        // "hot", "hotel" — allowOverlaps=false 시 "hotel"만 남음
        val automatonOverlap = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = true))
            .build()

        val automatonNoOverlap = AhoCorasickAutomaton.builder<String>()
            .add("hot", "HOT")
            .add("hotel", "HOTEL")
            .options(SearchOptions(allowOverlaps = false))
            .build()

        val text = "hotel"

        // Act
        val firstWithOverlap = automatonOverlap.firstMatch(text)
        val firstNoOverlap = automatonNoOverlap.firstMatch(text)

        // Assert
        // allowOverlaps=true: "hot"(start=0, len=3)과 "hotel"(start=0, len=5) 중 더 긴 "hotel" 우선
        firstWithOverlap.shouldNotBeNull()
        firstWithOverlap.keyword shouldBeEqualTo "hotel"

        // allowOverlaps=false: IntervalTree가 겹침 제거 → "hotel"만 남음
        firstNoOverlap.shouldNotBeNull()
        firstNoOverlap.keyword shouldBeEqualTo "hotel"

        // 두 결과 모두 "hotel"을 가리켜야 함
        firstWithOverlap.keyword shouldBeEqualTo firstNoOverlap.keyword
        firstWithOverlap.start shouldBeEqualTo firstNoOverlap.start

        log.debug { "allowOverlaps 비교 — overlap: $firstWithOverlap, noOverlap: $firstNoOverlap" }
    }
}
