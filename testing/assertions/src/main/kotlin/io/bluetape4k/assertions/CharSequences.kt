package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages

// ── shouldStartWith / shouldNotStartWith ─────────────────────────────────────

/**
 * CharSequence가 [prefix]로 시작하는지 검증한다.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param prefix 기대하는 접두사
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldStartWith(prefix: CharSequence): CharSequence {
    if (this == null || !this.startsWith(prefix)) {
        Failures.failComparison(
            Messages.expectedToBe("start with", prefix, this),
            prefix,
            this
        )
    }
    return this
}

/**
 * CharSequence가 [prefix]로 시작하지 않는지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param prefix 기대하지 않는 접두사
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldNotStartWith(prefix: CharSequence): CharSequence? {
    if (this != null && this.startsWith(prefix)) {
        Failures.failComparison(
            Messages.expectedNotToBe("start with", prefix, this),
            prefix,
            this
        )
    }
    return this
}

// ── shouldEndWith / shouldNotEndWith ─────────────────────────────────────────

/**
 * CharSequence가 [suffix]로 끝나는지 검증한다.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param suffix 기대하는 접미사
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldEndWith(suffix: CharSequence): CharSequence {
    if (this == null || !this.endsWith(suffix)) {
        Failures.failComparison(
            Messages.expectedToBe("end with", suffix, this),
            suffix,
            this
        )
    }
    return this
}

/**
 * CharSequence가 [suffix]로 끝나지 않는지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param suffix 기대하지 않는 접미사
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldNotEndWith(suffix: CharSequence): CharSequence? {
    if (this != null && this.endsWith(suffix)) {
        Failures.failComparison(
            Messages.expectedNotToBe("end with", suffix, this),
            suffix,
            this
        )
    }
    return this
}

// ── shouldContain / shouldNotContain ─────────────────────────────────────────

/**
 * CharSequence가 [substring]을 포함하는지 검증한다.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substring 기대하는 부분 문자열
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldContain(substring: CharSequence): CharSequence {
    if (this == null || !this.contains(substring)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", substring, this),
            substring,
            this
        )
    }
    return this
}

/**
 * CharSequence가 [substring]을 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substring 기대하지 않는 부분 문자열
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldNotContain(substring: CharSequence): CharSequence? {
    if (this != null && this.contains(substring)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", substring, this),
            substring,
            this
        )
    }
    return this
}

// ── shouldContainIgnoringCase ─────────────────────────────────────────────────

/**
 * CharSequence가 대소문자를 무시하고 [substring]을 포함하는지 검증한다.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substring 기대하는 부분 문자열 (대소문자 무시)
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldContainIgnoringCase(substring: CharSequence): CharSequence {
    if (this == null || !this.toString().lowercase().contains(substring.toString().lowercase())) {
        Failures.failComparison(
            Messages.expectedToBe("contain (ignoring case)", substring, this),
            substring,
            this
        )
    }
    return this
}

// ── shouldBeEmpty / shouldNotBeEmpty ─────────────────────────────────────────

/**
 * CharSequence가 비어있는지 검증한다 (null 또는 empty).
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun CharSequence?.shouldBeEmpty(): CharSequence? {
    if (this != null && this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", "", this),
            "",
            this
        )
    }
    return this
}

/**
 * CharSequence가 비어있지 않은지 검증한다.
 *
 * receiver가 null이거나 empty이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
fun CharSequence?.shouldNotBeEmpty(): CharSequence {
    if (this == null || this.isEmpty()) {
        Failures.fail("Expected CharSequence to not be empty, but was ${Messages.stringify(this)}.")
    }
    return this
}

// ── shouldBeBlank / shouldNotBeBlank ─────────────────────────────────────────

/**
 * CharSequence가 공백인지 검증한다 (null, empty, 또는 whitespace만 포함).
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun CharSequence?.shouldBeBlank(): CharSequence? {
    if (this != null && this.isNotBlank()) {
        Failures.failComparison(
            Messages.expectedToBe("be blank", "<blank>", this),
            "<blank>",
            this
        )
    }
    return this
}

/**
 * CharSequence가 공백이 아닌지 검증한다.
 *
 * receiver가 null이거나 blank이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
fun CharSequence?.shouldNotBeBlank(): CharSequence {
    if (this == null || this.isBlank()) {
        Failures.fail("Expected CharSequence to not be blank, but was ${Messages.stringify(this)}.")
    }
    return this
}

// ── shouldBeNullOrEmpty / shouldNotBeNullOrEmpty ──────────────────────────────

/**
 * CharSequence가 null 또는 empty인지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 */
fun CharSequence?.shouldBeNullOrEmpty() {
    if (!this.isNullOrEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be null or empty", "<null or empty>", this),
            "<null or empty>",
            this
        )
    }
}

/**
 * CharSequence가 null이 아니고 empty가 아닌지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @return non-null receiver (체이닝 지원)
 */
fun CharSequence?.shouldNotBeNullOrEmpty(): CharSequence {
    val s = this
    if (s.isNullOrEmpty()) {
        Failures.fail("Expected CharSequence to not be null or empty, but was ${Messages.stringify(s)}.")
    }
    return s
}

// ── shouldMatch / shouldNotMatch ─────────────────────────────────────────────

/**
 * CharSequence 전체가 [regex]와 매치되는지 검증한다.
 *
 * `Regex.matches()`를 사용하므로 전체 문자열이 패턴과 일치해야 한다.
 * 부분 매치가 필요하면 [shouldContainRegex]를 사용하라.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param regex 전체 매치에 사용할 정규식
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldMatch(regex: Regex): CharSequence {
    if (this == null || !regex.matches(this)) {
        Failures.failComparison(
            Messages.expectedToBe("match regex", regex.pattern, this),
            regex.pattern,
            this
        )
    }
    return this
}

/**
 * CharSequence 전체가 [pattern] 문자열로 만든 Regex와 매치되는지 검증한다.
 *
 * `Regex.matches()`를 사용하므로 전체 문자열이 패턴과 일치해야 한다.
 * 부분 매치가 필요하면 [shouldContainRegex]를 사용하라.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param pattern 전체 매치에 사용할 정규식 패턴 문자열
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldMatch(pattern: String): CharSequence =
    this shouldMatch Regex(pattern)

/**
 * CharSequence 전체가 [regex]와 매치되지 않는지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param regex 전체 매치에 사용할 정규식
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldNotMatch(regex: Regex): CharSequence? {
    if (this != null && regex.matches(this)) {
        Failures.failComparison(
            Messages.expectedNotToBe("match regex", regex.pattern, this),
            regex.pattern,
            this
        )
    }
    return this
}

// ── shouldContainAll / shouldContainNone ─────────────────────────────────────

/**
 * CharSequence가 [substrings] 모두를 포함하는지 검증한다.
 *
 * receiver가 null이거나 하나라도 포함하지 않으면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substrings 모두 포함되어야 하는 부분 문자열 목록
 * @return non-null receiver (체이닝 지원)
 */
fun CharSequence?.shouldContainAll(vararg substrings: CharSequence): CharSequence {
    if (this == null) {
        Failures.fail("Expected CharSequence to contain all substrings, but was <null>.")
    }
    val missing = substrings.filter { !this.contains(it) }
    if (missing.isNotEmpty()) {
        Failures.fail(
            "Expected ${Messages.stringify(this)} to contain all of ${
                substrings.joinToString(prefix = "[", postfix = "]") {
                    Messages.stringify(it)
                }
            }, but was missing: ${
                missing.joinToString(prefix = "[", postfix = "]") {
                    Messages.stringify(it)
                }
            }."
        )
    }
    return this
}

/**
 * CharSequence가 [substrings] Iterable의 모든 요소를 포함하는지 검증한다.
 *
 * receiver가 null이거나 하나라도 포함하지 않으면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substrings 모두 포함되어야 하는 부분 문자열 목록
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldContainAll(substrings: Iterable<CharSequence>): CharSequence {
    val list = substrings.toList()
    return this.shouldContainAll(*list.toTypedArray())
}

/**
 * CharSequence가 [substrings] 중 어느 것도 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param substrings 포함되지 않아야 하는 부분 문자열 목록
 * @return receiver (체이닝 지원)
 */
fun CharSequence?.shouldContainNone(vararg substrings: CharSequence): CharSequence? {
    if (this != null) {
        val found = substrings.filter { this.contains(it) }
        if (found.isNotEmpty()) {
            Failures.fail(
                "Expected ${Messages.stringify(this)} to contain none of ${
                    substrings.joinToString(prefix = "[", postfix = "]") {
                        Messages.stringify(it)
                    }
                }, but found: ${
                    found.joinToString(prefix = "[", postfix = "]") {
                        Messages.stringify(it)
                    }
                }."
            )
        }
    }
    return this
}

// ── shouldBeEqualToIgnoringCase ───────────────────────────────────────────────

/**
 * CharSequence가 대소문자를 무시하고 [expected]와 같은지 검증한다.
 *
 * receiver가 null이면 AssertionFailedError를 던진다.
 *
 * @receiver 검증할 CharSequence (nullable 허용)
 * @param expected 기대하는 문자열 (대소문자 무시)
 * @return non-null receiver (체이닝 지원)
 */
infix fun CharSequence?.shouldBeEqualToIgnoringCase(expected: String): CharSequence {
    if (this == null || !this.toString().equals(expected, ignoreCase = true)) {
        Failures.failComparison(
            Messages.expectedToBe("be equal to (ignoring case)", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── shouldContainRegex / shouldNotContainRegex (부분 매치) ────────────────────

/**
 * CharSequence의 일부가 [regex]와 매치되는지 검증한다 (부분 포함).
 *
 * `Regex.containsMatchIn()`을 사용하므로 전체 문자열이 아닌 일부만 매치되어도 통과한다.
 * 전체 매치가 필요하면 [shouldMatch]를 사용하라.
 *
 * @receiver 검증할 CharSequence (non-null)
 * @param regex 부분 매치에 사용할 정규식
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence.shouldContainRegex(regex: Regex): CharSequence {
    if (!regex.containsMatchIn(this)) {
        Failures.failComparison(
            Messages.expectedToBe("contain regex", regex.pattern, this),
            regex.pattern,
            this
        )
    }
    return this
}

/**
 * CharSequence의 일부가 [regex]와 매치되지 않는지 검증한다 (부분 포함).
 *
 * @receiver 검증할 CharSequence (non-null)
 * @param regex 부분 매치에 사용할 정규식
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence.shouldNotContainRegex(regex: Regex): CharSequence {
    if (regex.containsMatchIn(this)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain regex", regex.pattern, this),
            regex.pattern,
            this
        )
    }
    return this
}

/**
 * CharSequence의 일부가 [pattern] 문자열로 만든 Regex와 매치되는지 검증한다 (부분 포함).
 *
 * `Regex.containsMatchIn()`을 사용하므로 전체 문자열이 아닌 일부만 매치되어도 통과한다.
 * 전체 매치가 필요하면 [shouldMatch]를 사용하라.
 *
 * @receiver 검증할 CharSequence (non-null)
 * @param pattern 부분 매치에 사용할 정규식 패턴 문자열
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence.shouldContainRegex(pattern: String): CharSequence =
    this shouldContainRegex Regex(pattern)

/**
 * CharSequence의 일부가 [pattern] 문자열로 만든 Regex와 매치되지 않는지 검증한다 (부분 포함).
 *
 * @receiver 검증할 CharSequence (non-null)
 * @param pattern 부분 매치에 사용할 정규식 패턴 문자열
 * @return receiver (체이닝 지원)
 */
infix fun CharSequence.shouldNotContainRegex(pattern: String): CharSequence =
    this shouldNotContainRegex Regex(pattern)
