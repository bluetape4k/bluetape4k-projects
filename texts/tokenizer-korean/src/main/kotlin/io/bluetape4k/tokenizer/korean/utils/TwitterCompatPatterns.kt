package io.bluetape4k.tokenizer.korean.utils

import java.util.regex.Pattern

/**
 * twitter-text 라이브러리의 VALID_URL / VALID_HASHTAG / VALID_MENTION_OR_LIST / VALID_CASHTAG 패턴을
 * 인라인 정규식으로 대체한 내부 유틸리티.
 *
 * ## 매치 범위 계약
 * - [VALID_URL]: lookbehind 사용 — 매치 범위에 선행 문자 미포함. `(https://...` 입력 시 `(` 미포함.
 * - [VALID_HASHTAG], [VALID_MENTION_OR_LIST], [VALID_CASHTAG]:
 *   group(1) = 선행 공백/문자열 시작, group(2) = 실제 토큰.
 *   `KoreanChunker`는 group(2) offset을 사용해야 한다.
 */
internal object TwitterCompatPatterns {

    /**
     * URL 패턴. scheme(`https?://`, `ftp://`, `www.`) 또는 베어 도메인(`openkoreantext.org`)을 매치.
     *
     * - 선행 `(`는 `\(?`로 매치 범위에 포함 (`(https://...` 형태 지원)
     * - 베어 도메인은 `(?<!\S)` (공백/문자열 시작 뒤)로 제한
     * - 종결 문자 클래스에서 `)` 제외: `(https://example.com)` 에서 `)` 미포함
     */
    val VALID_URL: Pattern = Pattern.compile(
        """(\(?(?:https?://|ftp://|www\.)|(?<!\S)(?=[a-zA-Z0-9])(?:[a-zA-Z0-9\-]+\.)+[a-zA-Z]{2,})""" +
            """[a-zA-Z0-9\-._~:/?#\[\]@!${'$'}&'(*+,;=%-]*""",
        Pattern.UNICODE_CASE
    )

    /**
     * 해시태그 패턴. group(1) = 선행 공백, group(2) = `#태그`.
     */
    val VALID_HASHTAG: Pattern = Pattern.compile(
        """(\s|^)(#[\p{L}\p{Digit}_]+)""",
        Pattern.UNICODE_CASE
    )

    /**
     * @멘션 / 리스트 패턴. group(1) = 선행 문자(공백 포함 비단어 문자 또는 문자열 시작), group(2) = `@계정`.
     *
     * twitter-text `VALID_MENTION_PRECEDING_CHARS` 에 맞춰 `"`, `.`, `,` 등 뒤에서도 멘션을 인식한다.
     */
    val VALID_MENTION_OR_LIST: Pattern = Pattern.compile(
        """([^A-Za-z0-9_!#${'$'}%&*@＠]|^)(@[A-Za-z0-9_]+(?:/[A-Za-z0-9_]+)?)""",
        Pattern.UNICODE_CASE
    )

    /**
     * 캐시태그 패턴 (대소문자 허용). group(1) = 선행 공백, group(2) = `$심볼`.
     *
     * 예: `$AAPL`, `$twtr`, `$BRK.A`
     */
    val VALID_CASHTAG: Pattern = Pattern.compile(
        """(\s|^)(\$[A-Za-z]{1,6}(?:\.[A-Za-z]{1,2})?)"""
    )
}
