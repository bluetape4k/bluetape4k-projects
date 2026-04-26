package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeEmpty
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickReplaceTest {

    companion object : KLogging()

    @Test
    fun `기본 마스킹 - 키워드를 고정 문자열로 치환`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("APPL", "****")
            .add("NYC", "***")
            .build()
        val input = "APPL HQ in NYC"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }

        // Assert
        result shouldBeEqualTo "**** HQ in ***"
        log.debug { "마스킹 결과: $result" }
    }

    @Test
    fun `transform이 match value를 사용하여 약어를 풀네임으로 치환`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("NYC", "New York City")
            .add("LA", "Los Angeles")
            .add("SF", "San Francisco")
            .build()
        val input = "Offices in NYC, LA and SF"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }

        // Assert
        result shouldBeEqualTo "Offices in New York City, Los Angeles and San Francisco"
        log.debug { "풀네임 치환 결과: $result" }
    }

    @Test
    fun `매치가 0개일 때 입력 텍스트를 그대로 반환`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .add("banana", "BANANA")
            .build()
        val input = "no keywords here"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }
        val matches = automaton.parseText(input)

        // Assert
        matches.shouldBeEmpty()
        result shouldBeEqualTo input
        log.debug { "비매치 결과: $result" }
    }

    @Test
    fun `overlapping 매치에서 leftmost-longest 규칙 적용`() {
        // Arrange - "she"와 "he"가 "ushers"에서 겹침
        // "she"는 start=1, "he"는 start=2 — replaceAll은 "she"를 먼저 처리하고 "he"를 skip
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("she", "SHE")
            .add("he", "HE")
            .options(SearchOptions(allowOverlaps = true))
            .build()
        val input = "ushers"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }

        // Assert: "she" 치환, "he"는 겹침으로 skip
        result shouldBeEqualTo "uSHErs"
        log.debug { "overlapping 처리 결과: $result" }
    }

    @Test
    fun `한글 텍스트와 ASCII 키워드 혼합 치환`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("AI", "인공지능")
            .add("ML", "머신러닝")
            .build()
        val input = "AI와 ML 기술은 현대 사회에서 중요하다"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }

        // Assert
        result shouldBeEqualTo "인공지능와 머신러닝 기술은 현대 사회에서 중요하다"
        log.debug { "한글+ASCII 혼합 치환 결과: $result" }
    }
}
