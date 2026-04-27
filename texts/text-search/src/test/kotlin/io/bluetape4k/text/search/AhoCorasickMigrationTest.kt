package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * x-obsoleted `TrieTest` API에서 신규 [AhoCorasickAutomaton] API로의 이행(migration) 검증.
 *
 * 구 `Trie` API와의 주요 차이점:
 * - `Trie.builder().addKeyword(k).build()` → `ahoCorasickOf(k)` 또는 `AhoCorasickAutomaton.builder<String>().add(k, k).build()`
 * - `trie.parseText(text)` → `automaton.parseText(text)` (동일)
 * - `trie.firstMatch(text)` → `automaton.firstMatch(text)` — **R5 leftmost-longest 의미론** 적용
 * - `Emit.keyword` → `AhoCorasickMatch.keyword`, `Emit.start/end` → `AhoCorasickMatch.start/end`
 * - `Trie.tokenize(text)[i].fragment` → `SearchToken.Fragment.text` 또는 `SearchToken.Match.text`
 * - `.ignoreOverlaps()` → `SearchOptions(allowOverlaps = false)`
 * - `.onlyWholeWords()` → `SearchOptions(wordBoundary = WordBoundary.LATIN_ALPHA)`
 * - `.ignoreCase()` → `SearchOptions(ignoreCase = true)`
 *
 * spec §7.6 의 12 케이스를 모두 포함한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickMigrationTest {

    companion object : KLogging() {
        private val ALPHABET = listOf("abc", "bcd", "cde")
        private val PRONOUNS = listOf("hers", "his", "she", "he")
        private val FOOD = listOf("veal", "cauliflower", "broccoli", "tomatoes")
        private val GREEK_LETTERS = listOf("Alpha", "Beta", "Gamma")
        private val UNICODE = listOf("turning", "once", "again", "börkü")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 1: keyword and text are same
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 1: 키워드와 검색 텍스트가 동일한 경우 — 정확히 1건 매치.
     *
     * 구 API: `Trie.builder().addKeyword("abc").build().parseText("abc")`
     */
    @Test
    fun `케이스1 - keyword와 text가 동일하면 1건 매치`() {
        // Arrange
        val automaton = ahoCorasickOf("abc")

        // Act
        val matches = automaton.parseText("abc")

        // Assert
        matches shouldHaveSize 1
        matches[0].start shouldBeEqualTo 0
        matches[0].end shouldBeEqualTo 2
        matches[0].keyword shouldBeEqualTo "abc"
        log.debug { "케이스1 매치: ${matches[0]}" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 2: ushers overlaps — he / she / hers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 2: "ushers" 텍스트에서 "he", "she", "hers" 겹침 매치 3건.
     *
     * 구 API `TrieTest.ushers test`: `he(2,3)`, `she(1,3)`, `hers(2,5)` 순서.
     * 신 API는 start ASC 정렬이므로: `she(1,3)`, `he(2,3)`, `hers(2,5)`.
     */
    @Test
    fun `케이스2 - ushers 겹침 매치 3건`() {
        // Arrange
        val automaton = ahoCorasickOf(PRONOUNS)

        // Act
        val matches = automaton.parseText("ushers")

        // Assert
        matches shouldHaveSize 3
        val byKeyword = matches.associateBy { it.keyword }
        byKeyword["she"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 1
            it.end shouldBeEqualTo 3
        }
        byKeyword["he"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 2
            it.end shouldBeEqualTo 3
        }
        byKeyword["hers"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 2
            it.end shouldBeEqualTo 5
        }
        log.debug { "케이스2 ushers 겹침 매치: $matches" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 3: food recipes — 다중 keyword + 다중 매치
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 3: 음식 재료 키워드 4개 + 레시피 텍스트에서 4건 매치.
     *
     * 구 API `TrieTest.food recipes`: `cauliflower(2,12)`, `tomatoes(18,25)`,
     * `veal(40,43)`, `broccoli(51,58)` 순서.
     */
    @Test
    fun `케이스3 - food recipes 다중 매치 4건`() {
        // Arrange
        val automaton = ahoCorasickOf(FOOD)
        val text = "2 cauliflowers, 3 tomatoes, 4 slices of veal, 100g broccoli"

        // Act
        val matches = automaton.parseText(text)

        // Assert
        matches shouldHaveSize 4
        val byKeyword = matches.associateBy { it.keyword }

        byKeyword["cauliflower"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 2
            it.end shouldBeEqualTo 12
        }
        byKeyword["tomatoes"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 18
            it.end shouldBeEqualTo 25
        }
        byKeyword["veal"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 40
            it.end shouldBeEqualTo 43
        }
        byKeyword["broccoli"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 51
            it.end shouldBeEqualTo 58
        }
        log.debug { "케이스3 food recipes 매치: $matches" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 4: start of churchill speech — ignoreOverlaps(allowOverlaps=false)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 4: "Turning" 텍스트 + 겹침 제거 → 비겹침 매치 2건.
     *
     * 구 API `TrieTest.start of churchill speech`:
     * `T(0,0)` + `urning(1,6)`.
     * `allowOverlaps=false` → IntervalTree 가 겹치는 짧은 매치를 제거.
     */
    @Test
    fun `케이스4 - start of churchill speech 겹침제거 2건`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            allowOverlaps = false
            keywords(
                "T" to "T",
                "u" to "u",
                "ur" to "ur",
                "r" to "r",
                "urn" to "urn",
                "ni" to "ni",
                "i" to "i",
                "in" to "in",
                "n" to "n",
                "urning" to "urning",
            )
        }

        // Act
        val matches = automaton.parseText("Turning")

        // Assert
        matches shouldHaveSize 2
        val sorted = matches.sortedBy { it.start }
        sorted[0].start shouldBeEqualTo 0
        sorted[0].end shouldBeEqualTo 0
        sorted[0].keyword shouldBeEqualTo "T"
        sorted[1].start shouldBeEqualTo 1
        sorted[1].end shouldBeEqualTo 6
        sorted[1].keyword shouldBeEqualTo "urning"
        log.debug { "케이스4 churchill 겹침제거 매치: $matches" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 5: partial match exclusion — wordBoundary=LATIN_ALPHA
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 5: 단어 경계([WordBoundary.LATIN_ALPHA]) 적용 시 부분 매치 제외.
     *
     * 구 API `TrieTest.partial match`:
     * `sugarcane sugarcane sugar canesugar` → 중앙 단독 "sugar"(20,24)만 1건.
     */
    @Test
    fun `케이스5 - partial match exclusion wordBoundary LATIN_ALPHA`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            wordBoundary = WordBoundary.LATIN_ALPHA
            keyword("sugar", "sugar")
        }
        val text = "sugarcane sugarcane sugar canesugar" // left, middle, right test

        // Act
        val matches = automaton.parseText(text)

        // Assert
        matches shouldHaveSize 1
        matches[0].start shouldBeEqualTo 20
        matches[0].end shouldBeEqualTo 24
        matches[0].keyword shouldBeEqualTo "sugar"
        log.debug { "케이스5 partial match 제외 결과: ${matches[0]}" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 6: tokenize full sentence
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 6: `tokenize` 결과 — [SearchToken.Fragment]와 [SearchToken.Match] 순서 검증.
     *
     * 구 API `TrieTest.tokenize full sentence`:
     * Fragment "Hear: " / Match "Alpha" / Fragment " team first, " /
     * Match "Beta" / Fragment " from the rear, " / Match "Gamma" / Fragment " in reserve"
     *
     * 신 API 차이: `tokens[i].fragment` 대신 `SearchToken.Fragment.text` 또는
     * `SearchToken.Match.text` 를 사용한다.
     */
    @Test
    fun `케이스6 - tokenize full sentence Fragment와 Match 순서 검증`() {
        // Arrange
        val automaton = ahoCorasickOf(GREEK_LETTERS)
        val text = "Hear: Alpha team first, Beta from the rear, Gamma in reserve"

        // Act
        val tokens = automaton.tokenize(text)

        // Assert: Fragment 4개 + Match 3개 = 7개 토큰
        tokens shouldHaveSize 7
        (tokens[0] as SearchToken.Fragment).text shouldBeEqualTo "Hear: "
        (tokens[1] as SearchToken.Match).text shouldBeEqualTo "Alpha"
        (tokens[2] as SearchToken.Fragment).text shouldBeEqualTo " team first, "
        (tokens[3] as SearchToken.Match).text shouldBeEqualTo "Beta"
        (tokens[4] as SearchToken.Fragment).text shouldBeEqualTo " from the rear, "
        (tokens[5] as SearchToken.Match).text shouldBeEqualTo "Gamma"
        (tokens[6] as SearchToken.Fragment).text shouldBeEqualTo " in reserve"

        // Match 토큰의 match 필드 검증
        tokens[1].shouldBeInstanceOf<SearchToken.Match<String>>()
        tokens[3].shouldBeInstanceOf<SearchToken.Match<String>>()
        tokens[5].shouldBeInstanceOf<SearchToken.Match<String>>()
        log.debug { "케이스6 tokenize 토큰: $tokens" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 7: ignoreCase — UNICODE keywords
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 7: `ignoreCase=true` + `wordBoundary=LATIN_ALPHA` 적용.
     *
     * 구 API `TrieTest.test ignorecase`:
     * "TurninG OnCe AgAiN BÖRKÜ" → `turning(0,6)`, `once(8,11)`, `again(13,17)`, `börkü(19,23)`.
     */
    @Test
    fun `케이스7 - ignoreCase UNICODE 4건 매치`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            wordBoundary = WordBoundary.LATIN_ALPHA
            keywords(UNICODE.associateWith { it })
        }

        // Act
        val matches = automaton.parseText("TurninG OnCe AgAiN BÖRKÜ")

        // Assert
        matches shouldHaveSize 4
        val byKeyword = matches.associateBy { it.keyword }
        byKeyword["turning"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 0
            it.end shouldBeEqualTo 6
        }
        byKeyword["once"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 8
            it.end shouldBeEqualTo 11
        }
        byKeyword["again"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 13
            it.end shouldBeEqualTo 17
        }
        byKeyword["börkü"].shouldNotBeNull().let {
            it.start shouldBeEqualTo 19
            it.end shouldBeEqualTo 23
        }
        log.debug { "케이스7 ignoreCase 유니코드 매치: $matches" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 8: replaceAll with map
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 8: `replaceAll { map[it.keyword]!! }` — 약어를 풀네임으로 치환.
     *
     * 구 API에는 직접 대응 케이스 없음; `food recipes` 변형으로 replaceAll 검증.
     */
    @Test
    fun `케이스8 - replaceAll map으로 약어를 풀네임으로 치환`() {
        // Arrange
        val replacementMap = mapOf(
            "cauliflower" to "콜리플라워",
            "tomatoes" to "토마토",
            "veal" to "송아지고기",
            "broccoli" to "브로콜리",
        )
        val automaton = ahoCorasickOf(FOOD)
        val text = "2 cauliflowers, 3 tomatoes, 4 slices of veal, 100g broccoli"

        // Act
        val result = automaton.replaceAll(text) { match ->
            replacementMap[match.keyword] ?: match.keyword
        }

        // Assert
        result shouldBeEqualTo "2 콜리플라워s, 3 토마토, 4 slices of 송아지고기, 100g 브로콜리"
        log.debug { "케이스8 replaceAll 치환 결과: $result" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 9: containsMatch → true/false
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 9: `containsMatch` — 매치 존재 여부 반환.
     *
     * 구 API `TrieTest.contains match`: `trie.containsMatch("ababcbab").shouldBeTrue()`.
     */
    @Test
    fun `케이스9 - containsMatch true false 반환`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            keyword("ab", "ab")
            keyword("cba", "cba")
            keyword("ababc", "ababc")
        }

        // Act & Assert
        automaton.containsMatch("ababcbab").shouldBeTrue()
        automaton.containsMatch("xyz") shouldBeEqualTo false
        log.debug { "케이스9 containsMatch 검증 완료" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 10: ignoreOverlaps (allowOverlaps=false)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 10: `allowOverlaps=false` — 겹치는 매치 제거 후 비겹침 2건.
     *
     * 구 API `TrieTest.non overlapping`:
     * "ababcbab" → `ababc(0,4)`, `ab(6,7)`.
     */
    @Test
    fun `케이스10 - allowOverlaps false 비겹침 2건`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            allowOverlaps = false
            keyword("ab", "ab")
            keyword("cba", "cba")
            keyword("ababc", "ababc")
        }

        // Act
        val matches = automaton.parseText("ababcbab")

        // Assert
        matches shouldHaveSize 2
        val sorted = matches.sortedBy { it.start }
        sorted[0].start shouldBeEqualTo 0
        sorted[0].end shouldBeEqualTo 4
        sorted[0].keyword shouldBeEqualTo "ababc"
        sorted[1].start shouldBeEqualTo 6
        sorted[1].end shouldBeEqualTo 7
        sorted[1].keyword shouldBeEqualTo "ab"
        log.debug { "케이스10 비겹침 매치: $matches" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 11: firstMatch - ushers (R5 leftmost-longest)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 11: `firstMatch("ushers")` — **R5 leftmost-longest** 의미론 적용.
     *
     * 구 API `TrieTest.ushers test first match` 에서는 Trie 탐색 순서상
     * `he(start=2)` 가 반환되었다.
     * 신 API의 [AhoCorasickAutomaton.firstMatch]는 start ASC → length DESC 기준으로
     * 가장 먼저(leftmost)이면서 길이가 더 긴 매치를 선택한다.
     *
     * - `she`: start=1, length=3
     * - `he`: start=2, length=2
     * - `hers`: start=2, length=4
     *
     * leftmost(start=1) 기준으로 **`she(start=1, end=3)`** 가 선택된다.
     *
     * ⚠️ **구 API와 의도적으로 다른 leftmost semantics — spec R5/§7.6 참조.**
     */
    @Test
    fun `케이스11 - firstMatch ushers R5 leftmost-longest she 반환`() {
        // Arrange
        val automaton = ahoCorasickOf(PRONOUNS)

        // Act
        val first = automaton.firstMatch("ushers")

        // Assert: R5 leftmost → she(start=1) 가 he(start=2)보다 앞서므로 she 반환
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "she"
        first.start shouldBeEqualTo 1
        first.end shouldBeEqualTo 3
        log.debug { "케이스11 firstMatch(ushers) R5 결과: $first" }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Case 12: firstMatch - unicode (R5 leftmost-longest)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 케이스 12: `firstMatch("TurninG OnCe AgAiN BÖRKÜ")` — **R5 leftmost-longest** 의미론.
     *
     * 구 API `TrieTest.test ignorecase first match` 에서는 `turning(0,6)` 을 반환했으며
     * 신 API도 동일하게 `turning(0,6)` 을 반환한다.
     * start=0 인 `turning` 이 가장 leftmost이므로 R5 규칙에 의해 동일 결과이지만,
     * 결과가 일치하는 이유는 leftmost 위치 때문이지 탐색 순서 때문이 아님을 명시적으로 검증한다.
     *
     * ⚠️ **구 API와 의도적으로 다른 leftmost semantics — spec R5/§7.6 참조.**
     * (이 케이스에서는 leftmost start가 동일하여 결과 값도 일치한다.)
     */
    @Test
    fun `케이스12 - firstMatch unicode R5 leftmost turning 반환`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            ignoreCase = true
            wordBoundary = WordBoundary.LATIN_ALPHA
            keywords(UNICODE.associateWith { it })
        }

        // Act
        val first = automaton.firstMatch("TurninG OnCe AgAiN BÖRKÜ")

        // Assert: leftmost match는 start=0 의 "turning" — R5 기준과 탐색 순서 기준이 동일 결과
        first.shouldNotBeNull()
        first.keyword shouldBeEqualTo "turning"
        first.start shouldBeEqualTo 0
        first.end shouldBeEqualTo 6
        log.debug { "케이스12 firstMatch unicode R5 결과: $first" }
    }
}
