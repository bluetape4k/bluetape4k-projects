package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * 한글(Hangul) 텍스트에 대한 Aho-Corasick 검색 동작 검증.
 *
 * - NFC 정규화로 conjoining 자모 분리(`U+1100~`) 입력에서도 음절(`U+AC00~`) 키워드가 매치되어야 한다.
 * - NFKC 정규화로 합성 기호(`㈜` 등)가 풀어진 형태(`(주)`) 키워드와 매치되어야 한다.
 * - 한글에는 [WordBoundary.LATIN_ALPHA]가 적합하지 않음을 명시적으로 검증한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AhoCorasickKoreanTest {

    @Suppress("ConstPropertyName")
    companion object: KLogging() {
        // U+1100 Hangul Choseong Kiyeok + U+1161 Hangul Jungseong A → NFC → U+AC00 "가"
        // 명시적 \uXXXX escape — IDE/source 인코딩이 자모를 자동 합성하지 않도록 보장
        private const val DECOMPOSED_GA = "가"  // 2 chars
        private const val COMPOSED_GA = "가"          // 1 char "가"

        // U+321C PARENTHESIZED HANGUL CIEUC U "㈜" → NFKC → "(주)" (3 chars)
        private const val CIRCLED_JU = "㈜"
    }

    @Test
    fun `NFC 정규화로 자모 분리 텍스트에서 키워드 매치`() {
        // Arrange — 키워드는 음절 형태("가"), 입력은 자모 분리 형태(ᄀ + ᅡ)
        val automaton = ahoCorasick<String> {
            normalization = NormalizationForm.NFC
            keyword(COMPOSED_GA, "GA")
        }
        val input = "X" + DECOMPOSED_GA + "Y"  // 4 chars: 'X', ᄀ(U+1100), ᅡ(U+1161), 'Y'
        input.length shouldBeEqualTo 4

        // Act
        val matches = automaton.parseText(input)

        // Assert — 1건 매치 발생.
        // OffsetMapping 알고리즘 특성: NFC 합성 시 "마지막 기여 origPos"가 매핑되므로
        // 합성 음절 '가'(norm pos 1)는 종결 자모 ᅡ(origPos=2)로 역매핑된다.
        // 따라서 (start, end) == (2, 2) — 합성에 마지막으로 기여한 단일 char만 가리킴.
        matches shouldHaveSize 1
        val first = matches.first()
        first.value shouldBeEqualTo "GA"
        first.start shouldBeEqualTo 2
        first.end shouldBeEqualTo 2
        log.debug {
            "NFC 자모-분리 매치: $first (참고: 합성 결과 위치는 마지막 기여 origPos에 매핑됨)"
        }
    }

    @Test
    fun `NFC + WHITESPACE_SEPARATED로 한글 단어 경계 매치`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            normalization = NormalizationForm.NFC
            wordBoundary = WordBoundary.WHITESPACE_SEPARATED
            keyword("사과", "APPLE")
            keyword("바나나", "BANANA")
        }
        val input = "오늘 사과 와 바나나 를 먹었다"

        // Act
        val matches = automaton.parseText(input)

        // Assert — 공백으로 분리된 두 토큰만 매치
        matches shouldHaveSize 2
        val byValue = matches.associateBy { it.value }
        byValue["APPLE"].shouldNotBeNull()
        byValue["BANANA"].shouldNotBeNull()
        // 각 매치의 substring이 원본과 일치
        input.substring(byValue["APPLE"]!!.start, byValue["APPLE"]!!.end + 1) shouldBeEqualTo "사과"
        input.substring(byValue["BANANA"]!!.start, byValue["BANANA"]!!.end + 1) shouldBeEqualTo "바나나"
        log.debug { "공백 경계 한글 매치: $matches" }
    }

    @Test
    fun `NFKC 정규화로 ㈜ 포함 텍스트에서 키워드 매치`() {
        // Arrange — 키워드는 풀어진 형태 "(주)", 입력은 합성 기호 "㈜"
        val automaton = ahoCorasick<String> {
            normalization = NormalizationForm.NFKC
            keyword("(주)", "CORP")
        }
        val input = "회사명: " + CIRCLED_JU + "블루테이프"
        // 회(0)사(1)명(2):(3) (4)㈜(5)블(6)루(7)테(8)이(9)프(10)
        val expectedStart = 5

        // Act
        val matches = automaton.parseText(input)

        // Assert — 1건 매치, 원본 offset은 ㈜ 위치 (start == end == 5)
        matches shouldHaveSize 1
        val first = matches.first()
        first.value shouldBeEqualTo "CORP"
        first.start shouldBeEqualTo expectedStart
        first.end shouldBeEqualTo expectedStart
        input.substring(first.start, first.end + 1) shouldBeEqualTo CIRCLED_JU
        log.debug { "NFKC ㈜ 매치: $first" }
    }

    @Test
    fun `LATIN_ALPHA boundary는 한글에 적합하지 않음 - CJK도 alphabetic이라 경계가 형성되지 않음`() {
        // Arrange — Character.isAlphabetic()은 한글도 true이므로 한글 사이 경계가 인식되지 않음
        // → "사과"가 "사과나무" 같은 합성어 안에 있어도 매치되지 않는다는 점을 보여주는 경고 케이스
        val automaton = ahoCorasick<String> {
            normalization = NormalizationForm.NFC
            wordBoundary = WordBoundary.LATIN_ALPHA
            keyword("사과", "APPLE")
        }
        // 합성어 "사과나무" — "사과" 뒤에 한글이 이어지므로 LATIN_ALPHA 경계 미형성 → 매치 실패
        val compounded = "사과나무"

        // Act
        val matches = automaton.parseText(compounded)

        // Assert — 한글은 모두 alphabetic이라 경계로 분리되지 않아 매치되지 않음
        matches shouldHaveSize 0
        log.debug { "LATIN_ALPHA 한글 부적합 케이스: matches=$matches (한글에는 WHITESPACE_SEPARATED 권장)" }
    }

    @Test
    fun `한글 텍스트에서 replaceAll 마스킹`() {
        // Arrange
        val automaton = ahoCorasick<String> {
            normalization = NormalizationForm.NFC
            keyword("비밀", "***")
            keyword("암호", "***")
        }
        val input = "이것은 비밀 이고 저것은 암호 이다"

        // Act
        val result = automaton.replaceAll(input) { match -> match.value }

        // Assert
        result shouldBeEqualTo "이것은 *** 이고 저것은 *** 이다"
        log.debug { "한글 마스킹 결과: $result" }
    }
}
