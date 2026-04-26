package io.bluetape4k.text.search.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.text.search.NormalizationForm
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * [OffsetMapping] 단위 테스트.
 *
 * NFC/NFKC 정규화로 인한 길이 변화 시 원본 offset 복원이 정확한지 검증한다.
 *
 * **테스트 데이터**: 한글 conjoining jamo (U+1100~)는 NFC로 음절 블록(U+AC00~)에 합성된다.
 * 호환 자모(U+3130~)는 NFC로 합성되지 않으므로 테스트에 사용하지 않는다.
 * 모든 multi-byte 문자는 명시적 `\uXXXX` escape로 표기하여 source 인코딩 의존을 제거한다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OffsetMappingTest {

    @Suppress("ConstPropertyName")
    companion object: KLogging() {
        // U+1100 Hangul Choseong Kiyeok + U+1161 Hangul Jungseong A → NFC → U+AC00 "가"
        // 길이 변화: 2 chars → 1 char
        private const val DECOMPOSED_GA = "가"
        private const val COMPOSED_GA = "가"

        // U+321C PARENTHESIZED HANGUL CIEUC U "㈜" → NFKC → "(주)" (3 chars: '(' + U+C8FC + ')')
        private const val CIRCLED_JU = "㈜"
        private const val EXPANDED_JU = "(주)"
    }

    @Test
    fun `NONE 정규화 - mapping은 null이고 텍스트 그대로 반환`() {
        // Arrange
        val original = "hello world"

        // Act
        val (text, mapping) = OffsetMapping.build(original, NormalizationForm.NONE)

        // Assert
        text shouldBeEqualTo "hello world"
        mapping.shouldBeNull()
        log.debug { "NONE 결과: text='$text', mapping=$mapping" }
    }

    @Test
    fun `NFC - 자모 분리 입력의 길이 감소 매핑 검증`() {
        // Arrange: ᄀ(U+1100) + ᅡ(U+1161) + X → NFC → 가(U+AC00) + X (3 chars → 2 chars)
        val original = DECOMPOSED_GA + "X"
        original.length shouldBeEqualTo 3

        // Act
        val (normalized, mapping) = OffsetMapping.build(original, NormalizationForm.NFC)

        // Assert
        normalized shouldBeEqualTo COMPOSED_GA + "X"
        normalized.length shouldBeEqualTo 2

        val m = checkNotNull(mapping) { "mapping must not be null for NFC" }

        // incremental prefix 알고리즘 결과:
        // step0(origPos=0, ᄀ) → norm="ᄀ" len=1, normToOrigList=[0]
        // step1(origPos=1, ᅡ) → norm="가" len=1 (replace), normToOrigList=[1]
        // step2(origPos=2, X) → norm="가X" len=2, normToOrigList=[1, 2]
        // → 합성에 마지막으로 기여한 origPos가 매핑된다.
        m.toOriginal(0) shouldBeEqualTo 1   // '가'은 origPos=1(ᅡ)에서 합성 완료
        m.toOriginal(1) shouldBeEqualTo 2   // 'X'

        // end-inclusive: '가' (norm end=0) → 원본 [0..1] 구간 (inclusive end=1)
        m.toOriginalEndInclusive(0) shouldBeEqualTo 1
        // 'X' (norm end=1) → 원본 end=2
        m.toOriginalEndInclusive(1) shouldBeEqualTo 2

        log.debug { "NFC 매핑: original.length=${original.length}, normalized.length=${normalized.length}" }
    }

    @Test
    fun `NFKC - 합성 기호 길이 증가 매핑 검증`() {
        // Arrange: A + ㈜(U+321C) + B → NFKC → A(주)B (3 chars → 5 chars)
        val original = "A" + CIRCLED_JU + "B"
        original.length shouldBeEqualTo 3

        // Act
        val (normalized, mapping) = OffsetMapping.build(original, NormalizationForm.NFKC)

        // Assert
        normalized shouldBeEqualTo "A" + EXPANDED_JU + "B"
        normalized.length shouldBeEqualTo 5

        val m = checkNotNull(mapping) { "mapping must not be null for NFKC" }

        // incremental prefix 알고리즘 결과:
        // step0(A) → norm="A" len=1, list=[0]
        // step1(㈜) → norm="A(주)" len=4, expand by 3 → list=[0,1,1,1]
        // step2(B) → norm="A(주)B" len=5, expand by 1 → list=[0,1,1,1,2]
        m.toOriginal(0) shouldBeEqualTo 0   // 'A'
        m.toOriginal(1) shouldBeEqualTo 1   // '('  ← from ㈜
        m.toOriginal(2) shouldBeEqualTo 1   // '주' ← from ㈜
        m.toOriginal(3) shouldBeEqualTo 1   // ')'  ← from ㈜
        m.toOriginal(4) shouldBeEqualTo 2   // 'B'

        // end-inclusive: '(주)' 전체 매치 (norm end=3) → 원본 ㈜의 inclusive end=1
        m.toOriginalEndInclusive(3) shouldBeEqualTo 1
        // 'B' (norm end=4) → 원본 end=2
        m.toOriginalEndInclusive(4) shouldBeEqualTo 2

        log.debug { "NFKC 매핑: original.length=${original.length}, normalized.length=${normalized.length}" }
    }

    @Test
    fun `빈 문자열, 단일 char, ascii-only - 정규화 무영향 케이스`() {
        // Arrange & Act & Assert: 빈 문자열
        val (emptyText, emptyMapping) = OffsetMapping.build("", NormalizationForm.NFC)
        emptyText shouldBeEqualTo ""
        val em = checkNotNull(emptyMapping)
        em.toOriginal(0) shouldBeEqualTo 0

        // Arrange & Act & Assert: 단일 ascii char
        val (singleText, singleMapping) = OffsetMapping.build("X", NormalizationForm.NFC)
        singleText shouldBeEqualTo "X"
        val sm = checkNotNull(singleMapping)
        sm.toOriginal(0) shouldBeEqualTo 0
        sm.toOriginalEndInclusive(0) shouldBeEqualTo 0

        // Arrange & Act & Assert: 순수 ascii — 정규화로 길이 변화 없음 (identity 동작)
        val (asciiText, asciiMapping) = OffsetMapping.build("hello", NormalizationForm.NFKC)
        asciiText shouldBeEqualTo "hello"
        val am = checkNotNull(asciiMapping)
        for (i in 0 until 5) {
            am.toOriginal(i) shouldBeEqualTo i
            am.toOriginalEndInclusive(i) shouldBeEqualTo i
        }
        log.debug { "ASCII identity 동작 검증 완료" }
    }

    @Test
    fun `end-inclusive round-trip - NFKC 매치의 원본 substring 복원 검증`() {
        // Arrange: "X㈜Y" 에서 "(주)" 매치 → 원본의 ㈜ 1글자 복원
        val original = "X" + CIRCLED_JU + "Y"
        val (normalized, mapping) = OffsetMapping.build(original, NormalizationForm.NFKC)
        normalized shouldBeEqualTo "X" + EXPANDED_JU + "Y"

        val m = checkNotNull(mapping)

        // Act: normalized에서 "(주)" 매치 가정 — start=1, end=3 (inclusive)
        val normStart = 1
        val normEndInclusive = 3
        val origStart = m.toOriginal(normStart)
        val origEndInclusive = m.toOriginalEndInclusive(normEndInclusive)

        // Assert: 원본 substring 복원
        val recovered = original.substring(origStart, origEndInclusive + 1)
        recovered shouldBeEqualTo CIRCLED_JU
        origStart shouldBeEqualTo 1
        origEndInclusive shouldBeEqualTo 1

        // identity helper 검증
        val identity = OffsetMapping.identity(5)
        identity.toOriginal(0) shouldBeEqualTo 0
        identity.toOriginal(4) shouldBeEqualTo 4
        identity.toNormalized(3) shouldBeEqualTo 3
        identity.toOriginalEndInclusive(3) shouldBeEqualTo 3

        log.debug { "round-trip 복원: '$recovered' from original[$origStart..$origEndInclusive]" }
    }
}
