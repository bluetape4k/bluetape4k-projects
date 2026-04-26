package io.bluetape4k.text.search

import java.io.Serializable

/**
 * 단어 경계 탐지 방식을 정의하는 열거형.
 *
 * - [NONE]: 경계 없이 부분 문자열도 매치
 * - [LATIN_ALPHA]: [Character.isAlphabetic]을 기준으로 알파벳 경계에서만 매치 (한글에는 [WHITESPACE_SEPARATED] 권장)
 * - [WHITESPACE_SEPARATED]: 공백 문자 경계에서만 매치
 */
enum class WordBoundary {
    /** 경계 없음 — 부분 문자열 매치 허용 */
    NONE,

    /**
     * 알파벳 경계 — [Character.isAlphabetic] 기준.
     * **주의**: [Character.isAlphabetic]은 CJK 한자/한글도 포함하므로 한국어 텍스트에는 [WHITESPACE_SEPARATED] 권장.
     */
    LATIN_ALPHA,

    /** 공백 경계 — 공백 문자([Character.isWhitespace])로 구분된 토큰 단위로만 매치 */
    WHITESPACE_SEPARATED,
}

/**
 * 유니코드 정규화 형식.
 *
 * - [NONE]: 정규화 없음 (기본값, ASCII 전용 텍스트에 권장)
 * - [NFC]: Canonical Decomposition 후 Canonical Composition (한글 자모 합성에 효과적)
 * - [NFKC]: Compatibility Decomposition 후 Canonical Composition (전각 문자, 합성 기호 등 정규화)
 */
enum class NormalizationForm {
    /** 정규화 없음 */
    NONE,

    /**
     * NFC (Canonical Decomposition + Canonical Composition).
     * 예: 자모 분리 `ㄴㅏ` → `나`
     */
    NFC,

    /**
     * NFKC (Compatibility Decomposition + Canonical Composition).
     * 예: `㈜` → `(주)` (길이 변경 주의 — [io.bluetape4k.text.search.internal.OffsetMapping] 자동 처리)
     */
    NFKC,
}

/**
 * Aho-Corasick 검색 옵션.
 *
 * 모든 필드는 `val` (불변). 변경 시 [copy]를 사용한다.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, wordBoundary = WordBoundary.WHITESPACE_SEPARATED)
 * val opts2 = opts.copy(allowOverlaps = false)
 * ```
 *
 * **주의**: [stopOnFirstMatch]는 [io.bluetape4k.text.search.flow.matchesAsFlow]에서 무시된다.
 * Flow에서 첫 매치만 원하면 `take(1)`을 사용한다.
 */
data class SearchOptions(
    /** 대소문자를 무시할지 여부 (기본값: false) */
    val ignoreCase: Boolean = false,

    /** 겹치는 매치를 허용할지 여부 (기본값: true). false이면 더 긴 키워드가 우선. */
    val allowOverlaps: Boolean = true,

    /** 단어 경계 탐지 방식 (기본값: [WordBoundary.NONE]) */
    val wordBoundary: WordBoundary = WordBoundary.NONE,

    /** 유니코드 정규화 형식 (기본값: [NormalizationForm.NONE]) */
    val normalization: NormalizationForm = NormalizationForm.NONE,

    /**
     * 첫 번째 매치 발견 시 즉시 중단할지 여부 (기본값: false).
     * **주의**: [io.bluetape4k.text.search.flow.matchesAsFlow]에서는 무시된다.
     */
    val stopOnFirstMatch: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
