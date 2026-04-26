package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * [AhoCorasickAutomaton] 핵심 기능을 단위 검증하는 테스트.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickAutomatonTest : AbstractAhoCorasickTest() {

    companion object : KLogging()

    // ──────────────────────────────── parseText ────────────────────────────────

    @Test
    fun `parseText - 빈 keyword set은 empty result 반환`() {
        // Arrange: 키워드 없이 automaton 빌드
        val automaton = AhoCorasickAutomaton.builder<String>().build()

        // Act
        val matches = automaton.parseText("some text")

        // Assert
        matches.shouldHaveSize(0)
        log.debug { "빈 keyword set 결과: $matches" }
    }

    @Test
    fun `parseText - 빈 input은 empty result 반환`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE")
            .build()

        // Act
        val matches = automaton.parseText("")

        // Assert
        matches.shouldHaveSize(0)
        log.debug { "빈 input 결과: $matches" }
    }

    @Test
    fun `parseText - 단일 keyword 매치 start, end, keyword, value 검증`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("apple", "APPLE_VALUE")
            .build()

        // Act
        val matches = automaton.parseText("I like apple here")

        // Assert
        matches shouldHaveSize 1
        val match = matches.first()
        match.shouldNotBeNull()
        match.start shouldBeEqualTo 7     // "apple" starts at index 7
        match.end shouldBeEqualTo 11      // "apple" ends at index 11 (inclusive)
        match.keyword shouldBeEqualTo "apple"
        match.value shouldBeEqualTo "APPLE_VALUE"
        match.length shouldBeEqualTo 5
        log.debug { "단일 키워드 매치: $match" }
    }

    @Test
    fun `parseText - 다중 매치 start ascending 정렬 순서`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("one", "1")
            .add("two", "2")
            .add("three", "3")
            .build()

        // Act
        val matches = automaton.parseText("one two three")

        // Assert
        matches shouldHaveSize 3
        // 매치 결과는 start 위치 오름차순이어야 함
        for (i in 1 until matches.size) {
            (matches[i].start >= matches[i - 1].start).shouldBeTrue()
        }
        matches[0].keyword shouldBeEqualTo "one"
        matches[1].keyword shouldBeEqualTo "two"
        matches[2].keyword shouldBeEqualTo "three"
        log.debug { "다중 매치 정렬: $matches" }
    }

    // ──────────────────────────────── containsMatch ────────────────────────────────

    @Test
    fun `containsMatch - 매치 있을 때 true, 없을 때 false`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("hello", "HELLO")
            .add("world", "WORLD")
            .build()

        // Act & Assert — true case
        automaton.containsMatch("say hello there").shouldBeTrue()

        // Act & Assert — false case
        automaton.containsMatch("no keywords here").shouldBeFalse()
        log.debug { "containsMatch 검증 완료" }
    }

    // ──────────────────────────────── tokenize ────────────────────────────────

    @Test
    fun `tokenize - Match와 Fragment가 올바르게 교차함`() {
        // Arrange
        val automaton = AhoCorasickAutomaton.builder<String>()
            .add("foo", "FOO")
            .add("bar", "BAR")
            .build()
        val input = "prefix foo middle bar suffix"

        // Act
        val tokens = automaton.tokenize(input)

        // Assert: Fragment("prefix "), Match("foo"), Fragment(" middle "), Match("bar"), Fragment(" suffix")
        tokens.size shouldBeEqualTo 5

        (tokens[0] is SearchToken.Fragment).shouldBeTrue()
        (tokens[0] as SearchToken.Fragment).text shouldBeEqualTo "prefix "

        (tokens[1] is SearchToken.Match).shouldBeTrue()
        (tokens[1] as SearchToken.Match<String>).text shouldBeEqualTo "foo"
        (tokens[1] as SearchToken.Match<String>).match.keyword shouldBeEqualTo "foo"

        (tokens[2] is SearchToken.Fragment).shouldBeTrue()
        (tokens[2] as SearchToken.Fragment).text shouldBeEqualTo " middle "

        (tokens[3] is SearchToken.Match).shouldBeTrue()
        (tokens[3] as SearchToken.Match<String>).text shouldBeEqualTo "bar"

        (tokens[4] is SearchToken.Fragment).shouldBeTrue()
        (tokens[4] as SearchToken.Fragment).text shouldBeEqualTo " suffix"

        log.debug { "tokenize 결과: $tokens" }
    }

    // ──────────────────────────────── Builder validation ────────────────────────────────

    @Test
    fun `Builder add - 빈 keyword는 IllegalArgumentException`() {
        // Act & Assert — empty string
        assertThrows<IllegalArgumentException> {
            AhoCorasickAutomaton.builder<String>().add("", "value")
        }
        // Act & Assert — blank string (whitespace only)
        assertThrows<IllegalArgumentException> {
            AhoCorasickAutomaton.builder<String>().add("   ", "value")
        }
        log.debug { "빈 keyword 검증 완료" }
    }

    // ──────────────────────────────── Serializable round-trip ────────────────────────────────

    @Test
    fun `AhoCorasickMatch Serializable round-trip`() {
        // Arrange
        val original = AhoCorasickMatch(start = 3, end = 7, keyword = "hello", value = "HELLO")

        // Act
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use { it.readObject() }

        // Assert
        restored.shouldNotBeNull()
        restored.shouldBeInstanceOf<AhoCorasickMatch<*>>()
        @Suppress("UNCHECKED_CAST")
        restored as AhoCorasickMatch<String>
        restored.start shouldBeEqualTo original.start
        restored.end shouldBeEqualTo original.end
        restored.keyword shouldBeEqualTo original.keyword
        restored.value shouldBeEqualTo original.value
        restored shouldBeEqualTo original
        log.debug { "AhoCorasickMatch 직렬화 round-trip 성공: $restored" }
    }

    @Test
    fun `SearchToken Match Serializable round-trip`() {
        // Arrange
        val match = AhoCorasickMatch(start = 0, end = 2, keyword = "hi", value = "HI")
        val original = SearchToken.Match("hi", match)

        // Act
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use { it.readObject() }

        // Assert
        restored.shouldNotBeNull()
        restored.shouldBeInstanceOf<SearchToken.Match<*>>()
        @Suppress("UNCHECKED_CAST")
        restored as SearchToken.Match<String>
        restored.text shouldBeEqualTo original.text
        restored.match shouldBeEqualTo original.match
        restored shouldBeEqualTo original
        log.debug { "SearchToken.Match 직렬화 round-trip 성공: $restored" }
    }

    @Test
    fun `SearchOptions Serializable round-trip`() {
        // Arrange
        val original = SearchOptions(
            ignoreCase = true,
            allowOverlaps = false,
            wordBoundary = WordBoundary.LATIN_ALPHA,
            normalization = NormalizationForm.NFC,
            stopOnFirstMatch = true,
        )

        // Act
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use { it.readObject() }

        // Assert
        restored.shouldNotBeNull()
        restored.shouldBeInstanceOf<SearchOptions>()
        restored as SearchOptions
        restored.ignoreCase shouldBeEqualTo original.ignoreCase
        restored.allowOverlaps shouldBeEqualTo original.allowOverlaps
        restored.wordBoundary shouldBeEqualTo original.wordBoundary
        restored.normalization shouldBeEqualTo original.normalization
        restored.stopOnFirstMatch shouldBeEqualTo original.stopOnFirstMatch
        restored shouldBeEqualTo original
        log.debug { "SearchOptions 직렬화 round-trip 성공: $restored" }
    }
}
