package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickDslTest {

    companion object : KLogging()

    @Test
    fun `DSL로 옵션과 키워드 5개 등록 후 parseText 결과 검증`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            keyword("apple", "APPLE")
            keyword("banana", "BANANA")
            keyword("cherry", "CHERRY")
            keyword("date", "DATE")
            keyword("elderberry", "ELDERBERRY")
        }

        // Act
        val matches = automaton.parseText("I have Apple, Banana, Cherry, Date and Elderberry")

        // Assert
        matches shouldHaveSize 5
        val keywords = matches.map { it.value }
        keywords.containsAll(listOf("APPLE", "BANANA", "CHERRY", "DATE", "ELDERBERRY")) shouldBeEqualTo true
        log.debug { "DSL 매치 결과: $matches" }
    }

    @Test
    fun `ahoCorasickOf vararg 헬퍼에서 keyword==value 검증`() {
        // Arrange
        val automaton = ahoCorasickOf("foo", "bar", "baz")

        // Act
        val matches = automaton.parseText("foo and bar and baz")

        // Assert
        matches shouldHaveSize 3
        matches.forEach { match ->
            // keyword와 value가 동일해야 함
            match.keyword shouldBeEqualTo match.value
        }
        log.debug { "vararg 헬퍼 결과: $matches" }
    }

    @Test
    fun `Map으로 keyword 등록 후 모든 키워드 매치 검증`() {
        // Arrange
        val keywordMap = mapOf(
            "NYC" to "New York City",
            "LA" to "Los Angeles",
            "SF" to "San Francisco",
        )
        val automaton = ahoCorasick<String> {
            keywords(keywordMap)
        }

        // Act
        val matches = automaton.parseText("Visiting NYC, LA and SF this summer")

        // Assert
        matches shouldHaveSize 3
        val valueMap = matches.associate { it.keyword to it.value }
        valueMap["NYC"] shouldBeEqualTo "New York City"
        valueMap["LA"] shouldBeEqualTo "Los Angeles"
        valueMap["SF"] shouldBeEqualTo "San Francisco"
        log.debug { "Map 키워드 등록 결과: $matches" }
    }

    @Test
    fun `4가지 옵션 동시 적용 - ignoreCase + wordBoundary + allowOverlaps + stopOnFirstMatch`() {
        // Arrange
        // ignoreCase=true, wordBoundary=WHITESPACE_SEPARATED, allowOverlaps=true, stopOnFirstMatch=true
        // "He" substring이 포함된 "ushers"는 WHITESPACE_SEPARATED 경계로 인해 매치 안 됨
        // 단독 단어인 "he"만 매치되어야 함
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            wordBoundary = WordBoundary.WHITESPACE_SEPARATED
            allowOverlaps = true
            stopOnFirstMatch = true
            keyword("he", "HE")
            keyword("she", "SHE")
        }

        // Act
        // "ushers"에 포함된 "she"/"he"는 단어 경계가 없어서 매치 안 됨
        // 단독 단어 "He"는 ignoreCase로 매치
        val matches = automaton.parseText("He likes ushers she said")

        // Assert: stopOnFirstMatch=true이므로 정확히 1개만 반환
        matches shouldHaveSize 1
        val first = matches.first()
        first.shouldNotBeNull()
        first.value shouldBeEqualTo "HE"
        log.debug { "4-옵션 동시 적용 결과: $matches" }
    }

    @Test
    fun `blank keyword 등록 시 IllegalArgumentException 발생`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            ahoCorasick<String> {
                keyword("  ", "value")
            }
        }
    }
}
