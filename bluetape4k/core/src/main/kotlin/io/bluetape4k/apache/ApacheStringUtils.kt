package io.bluetape4k.apache

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Strings

/**
 * 지정한 대체 마커로 문자열을 축약합니다. 예를 들어
 * "Now is the time for all good men" into "...is the time for..." if "..." was defined
 * as the replacement marker.
 *
 * `String.abbr(String, int)`처럼 동작하지만
 * "왼쪽 경계" 오프셋을 지정할 수 있습니다. 이 경계가 반드시
 * 결과의 가장 왼쪽 문자나 대체 마커 바로 뒤의 첫 문자가 되는 것은 아니지만,
 * 결과 어딘가에는 포함됩니다.
 *
 * 어떤 경우에도 {@code maxWidth\}보다 긴 문자열은 반환하지 않습니다.
 *
 * ```
 * StringUtils.abbreviate(null, null, *, *)                 = null
 * StringUtils.abbreviate("abcdefghijklmno", null, *, *)    = "abcdefghijklmno"
 * StringUtils.abbreviate("", "...", 0, 4)                  = ""
 * StringUtils.abbreviate("abcdefghijklmno", "---", -1, 10) = "abcdefg---"
 * StringUtils.abbreviate("abcdefghijklmno", ",", 0, 10)    = "abcdefghi,"
 * StringUtils.abbreviate("abcdefghijklmno", ",", 1, 10)    = "abcdefghi,"
 * StringUtils.abbreviate("abcdefghijklmno", ",", 2, 10)    = "abcdefghi,"
 * StringUtils.abbreviate("abcdefghijklmno", "::", 4, 10)   = "::efghij::"
 * StringUtils.abbreviate("abcdefghijklmno", "...", 6, 10)  = "...ghij..."
 * StringUtils.abbreviate("abcdefghijklmno", "*", 9, 10)    = "*ghijklmno"
 * StringUtils.abbreviate("abcdefghijklmno", "'", 10, 10)   = "'ghijklmno"
 * StringUtils.abbreviate("abcdefghijklmno", "!", 12, 10)   = "!ghijklmno"
 * StringUtils.abbreviate("abcdefghij", "abra", 0, 4)       = IllegalArgumentException
 * StringUtils.abbreviate("abcdefghij", "...", 5, 6)        = IllegalArgumentException
 * ```
 *
 * @receiver 검사할 문자열입니다. `null`일 수 있습니다.
 * @param abbrMarker 대체 마커로 사용할 문자열입니다.
 * @param offset 원본 문자열의 왼쪽 경계입니다.
 * @param maxWidth 결과 문자열의 최대 길이입니다. 4 이상이어야 합니다.
 * @return 축약된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.abbr(maxWidth: Int, abbrMarker: String = "...", offset: Int = 0): String =
    StringUtils.abbreviate(this, abbrMarker, offset, maxWidth)

/**
 * 문자열을 지정한 길이로 축약하고 가운데 문자를 제공된
 * 대체 문자열로 바꿉니다.
 *
 * 이 축약은 다음 조건을 모두 만족할 때만 수행됩니다:
 * - 축약 대상 문자열과 대체 문자열이 모두 `null` 또는 빈 값이 아닙니다
 * - 축약할 길이가 제공된 문자열 길이보다 작습니다
 * - 축약할 길이가 0보다 큽니다
 * - 축약 결과가 제공된 대체 문자열 길이와
 * 축약 대상 문자열의 첫 문자와 마지막 문자를 담을 만큼 충분한 공간을 가집니다
 *
 * 그 외의 경우에는 축약 대상 문자열을 그대로 반환합니다.
 *
 *
 * ```
 * StringUtils.abbreviateMiddle(null, null, 0)      = null
 * StringUtils.abbreviateMiddle("abc", null, 0)      = "abc"
 * StringUtils.abbreviateMiddle("abc", ".", 0)      = "abc"
 * StringUtils.abbreviateMiddle("abc", ".", 3)      = "abc"
 * StringUtils.abbreviateMiddle("abcdef", ".", 4)     = "ab.f"
 * ```
 *
 * @receiver 축약할 문자열입니다. `null`일 수 있습니다.
 * @param middle 가운데 문자를 대체할 문자열입니다. `null`일 수 있습니다.
 * @param length {@code str\}를 축약할 길이입니다.
 * @return 위 조건을 만족하면 축약된 문자열, 그렇지 않으면 축약 대상 원본 문자열입니다.
 */
fun String.abbrMiddle(length: Int, middle: String = "..."): String =
    StringUtils.abbreviateMiddle(this, middle, length)

/**
 * 문자열이 지정한 접미사 중 하나로 끝나지 않으면
 * 문자열 끝에 접미사를 추가합니다.
 *
 * ```
 * StringUtils.appendIfMissing(null, null) = null
 * StringUtils.appendIfMissing("abc", null) = "abc"
 * StringUtils.appendIfMissing("", "xyz") = "xyz"
 * StringUtils.appendIfMissing("abc", "xyz") = "abcxyz"
 * StringUtils.appendIfMissing("abcxyz", "xyz") = "abcxyz"
 * StringUtils.appendIfMissing("abcXYZ", "xyz") = "abcXYZxyz"
 * ```
 * 추가 접미사를 지정한 경우,
 * ```
 * StringUtils.appendIfMissing(null, null, null) = null
 * StringUtils.appendIfMissing("abc", null, null) = "abc"
 * StringUtils.appendIfMissing("", "xyz", null) = "xyz"
 * StringUtils.appendIfMissing("abc", "xyz", new CharSequence[]{null}) = "abcxyz"
 * StringUtils.appendIfMissing("abc", "xyz", "") = "abc"
 * StringUtils.appendIfMissing("abc", "xyz", "mno") = "abcxyz"
 * StringUtils.appendIfMissing("abcxyz", "xyz", "mno") = "abcxyz"
 * StringUtils.appendIfMissing("abcmno", "xyz", "mno") = "abcmno"
 * StringUtils.appendIfMissing("abcXYZ", "xyz", "mno") = "abcXYZxyz"
 * StringUtils.appendIfMissing("abcMNO", "xyz", "mno") = "abcMNOxyz"
 * ```
 *
 * @receiver 대상 문자열입니다.
 * @param suffix 문자열 끝에 추가할 접미사입니다.
 * @param suffixes 이미 유효한 끝으로 인정할 추가 접미사입니다.
 *
 * @return 접미사가 추가되면 새 문자열, 그렇지 않으면 기존 문자열입니다.
 */
fun String.appendIfMissing(suffix: CharSequence, vararg suffixes: CharSequence): String =
    Strings.CS.appendIfMissing(this, suffix, *suffixes)

/**
 * 대소문자를 무시했을 때 문자열이 지정한 접미사 중 하나로 끝나지 않으면
 * 문자열 끝에 접미사를 추가합니다.
 *
 * ```
 * StringUtils.appendIfMissingIgnoreCase(null, null) = null
 * StringUtils.appendIfMissingIgnoreCase("abc", null) = "abc"
 * StringUtils.appendIfMissingIgnoreCase("", "xyz") = "xyz"
 * StringUtils.appendIfMissingIgnoreCase("abc", "xyz") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcxyz", "xyz") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcXYZ", "xyz") = "abcXYZ"
 * ```
 * 추가 접미사를 지정한 경우,
 * ```
 * StringUtils.appendIfMissingIgnoreCase(null, null, null) = null
 * StringUtils.appendIfMissingIgnoreCase("abc", null, null) = "abc"
 * StringUtils.appendIfMissingIgnoreCase("", "xyz", null) = "xyz"
 * StringUtils.appendIfMissingIgnoreCase("abc", "xyz", new CharSequence[]{null}) = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abc", "xyz", "") = "abc"
 * StringUtils.appendIfMissingIgnoreCase("abc", "xyz", "mno") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcxyz", "xyz", "mno") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcmno", "xyz", "mno") = "abcmno"
 * StringUtils.appendIfMissingIgnoreCase("abcXYZ", "xyz", "mno") = "abcXYZ"
 * StringUtils.appendIfMissingIgnoreCase("abcMNO", "xyz", "mno") = "abcMNO"
 * ```
 *
 * @receiver 대상 문자열입니다.
 * @param suffix 문자열 끝에 추가할 접미사입니다.
 * @param suffixes 이미 유효한 끝으로 인정할 추가 접미사입니다.
 *
 * @return 접미사가 추가되면 새 문자열, 그렇지 않으면 기존 문자열입니다.
 */
fun String.appendIfMissingIgnoreCase(suffix: CharSequence, vararg suffixes: CharSequence): String =
    Strings.CI.appendIfMissing(this, suffix, *suffixes)

/**
 * {@code size\} 크기의 더 큰 문자열 안에서 문자열을 가운데 정렬합니다
 * 공백 문자(' ')로 채웁니다.
 *
 * 지정 크기가 문자열 길이보다 작으면 원래 문자열을 반환합니다.
 * `null` 문자열은 `null`을 반환합니다.
 * 음수 크기는 0으로 처리합니다.</p>
 *
 * `center(str, size, " ")`와 동일합니다.
 *
 * ```
 * StringUtils.center(null, *)   = null
 * StringUtils.center("", 4)     = "    "
 * StringUtils.center("ab", -1)  = "ab"
 * StringUtils.center("ab", 4)   = " ab "
 * StringUtils.center("abcd", 2) = "abcd"
 * StringUtils.center("a", 4)    = " a  "
 * ```
 *
 * @receiver 가운데 정렬할 문자열입니다. `null`일 수 있습니다.
 * @param size 새 문자열 크기입니다. 음수는 0으로 처리합니다.
 * @return 가운데 정렬된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.center(size: Int, padChar: Char = ' '): String =
    StringUtils.center(this, size, padChar)

/**
 * {@code size\} 크기의 더 큰 문자열 안에서 문자열을 가운데 정렬합니다.
 * 제공된 문자열을 채움 값으로 사용합니다.
 *
 * 지정 크기가 문자열 길이보다 작으면 문자열을 그대로 반환합니다.
 * `null` 문자열은 `null`을 반환합니다.
 * 음수 크기는 0으로 처리합니다.
 *
 * ```
 * StringUtils.center(null, *, *)     = null
 * StringUtils.center("", 4, " ")     = "    "
 * StringUtils.center("ab", -1, " ")  = "ab"
 * StringUtils.center("ab", 4, " ")   = " ab "
 * StringUtils.center("abcd", 2, " ") = "abcd"
 * StringUtils.center("a", 4, " ")    = " a  "
 * StringUtils.center("a", 4, "yz")   = "yayz"
 * StringUtils.center("abc", 7, null) = "  abc  "
 * StringUtils.center("abc", 7, "")   = "  abc  "
 * ```
 *
 * @receiver 가운데 정렬할 문자열입니다. `null`일 수 있습니다.
 * @param size 새 문자열 크기입니다. 음수는 0으로 처리합니다.
 * @param padStr 새 문자열을 채울 문자열입니다. `null`이거나 비어 있으면 안 됩니다.
 * @return 가운데 정렬된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 * @throws IllegalArgumentException `padStr`가 `null`이거나 비어 있으면 발생합니다.
 */
fun String.center(size: Int, padStr: String): String =
    StringUtils.center(this, size, padStr)

/**
 * 문자열의 마지막 문자를 제거합니다.
 *
 * 문자열이 {@code \r\n}로 끝나면 두 문자를 모두 제거합니다.
 *
 * ```
 * StringUtils.chop(null)          = null
 * StringUtils.chop("")            = ""
 * StringUtils.chop("abc \r")      = "abc "
 * StringUtils.chop("abc\n")       = "abc"
 * StringUtils.chop("abc\r\n")     = "abc"
 * StringUtils.chop("abc")         = "ab"
 * StringUtils.chop("abc\nabc")    = "abc\nab"
 * StringUtils.chop("a")           = ""
 * StringUtils.chop("\r")          = ""
 * StringUtils.chop("\n")          = ""
 * StringUtils.chop("\r\n")        = ""
 * ```
 *
 * @receiver 마지막 문자를 제거할 문자열입니다. `null`일 수 있습니다.
 * @return 마지막 문자가 제거된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.chop(): String =
    StringUtils.chop(this)


/**
 * CharSequence가 지정한 문자 집합의 문자 중 하나라도 포함하는지
 * 확인합니다.
 *
 * <p>A `null` CharSequence will return {@code false}.
 * A `null` or zero length search array will return {@code false}.</p>
 *
 * ```
 * StringUtils.containsAny(null, *)                  = false
 * StringUtils.containsAny("", *)                    = false
 * StringUtils.containsAny(*, null)                  = false
 * StringUtils.containsAny(*, [])                    = false
 * StringUtils.containsAny("zzabyycdxx", ['z', 'a']) = true
 * StringUtils.containsAny("zzabyycdxx", ['b', 'y']) = true
 * StringUtils.containsAny("zzabyycdxx", ['z', 'y']) = true
 * StringUtils.containsAny("aba", ['z'])             = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchChars 검색할 문자입니다. `null`일 수 있습니다.
 * @return 문자를 하나라도 찾으면 {@code true\}, 매치가 없거나 입력이 `null`이면 {@code false\}입니다.
 */
fun CharSequence.containsAny(vararg searchChars: Char): Boolean =
    StringUtils.containsAny(this, *searchChars)

/**
 * CharSequence가 지정한 배열의 CharSequence 중 하나라도 포함하는지 확인합니다.
 *
 * A `null` or zero length search array will return `false`.
 *
 * ```
 * StringUtils.containsAny(null, *)            = false
 * StringUtils.containsAny("", *)              = false
 * StringUtils.containsAny(*, null)            = false
 * StringUtils.containsAny(*, [])              = false
 * StringUtils.containsAny("abcd", "ab", null) = true
 * StringUtils.containsAny("abcd", "ab", "cd") = true
 * StringUtils.containsAny("abc", "d", "abc")  = true
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchCharSequences The array of CharSequences to search for, may be null. Individual CharSequences may be
 *        null as well.
 * @return 검색 CharSequence를 하나라도 찾으면 `true`, 그렇지 않으면 `false`입니다.
 */
fun CharSequence.containsAny(vararg searchCharSequences: CharSequence): Boolean =
    Strings.CS.containsAny(this, *searchCharSequences)


/**
 * 대소문자를 무시하고 CharSequence가 지정한 배열의 CharSequence 중 하나라도 포함하는지 확인합니다.
 *
 * A `null` {@code cs} CharSequence will return {@code false}. A `null` or zero length search array will
 * return {@code false}.
 *
 *
 * ```
 * StringUtils.containsAny(null, *)            = false
 * StringUtils.containsAny("", *)              = false
 * StringUtils.containsAny(*, null)            = false
 * StringUtils.containsAny(*, [])              = false
 * StringUtils.containsAny("abcd", "ab", null) = true
 * StringUtils.containsAny("abcd", "ab", "cd") = true
 * StringUtils.containsAny("abc", "d", "abc")  = true
 * StringUtils.containsAny("abc", "D", "ABC")  = true
 * StringUtils.containsAny("ABC", "d", "abc")  = true
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchCharSequences The array of CharSequences to search for, may be null. Individual CharSequences may be
 *        null as well.
 * @return 검색 CharSequence를 하나라도 찾으면 `true`, 그렇지 않으면 `false`입니다.
 */
fun CharSequence.containsAnyIgnoreCase(vararg searchCharSequences: CharSequence): Boolean =
    Strings.CI.containsAny(this, *searchCharSequences)

/**
 * 대소문자와 관계없이 CharSequence가 검색 CharSequence를 포함하는지 확인합니다,
 * handling `null`. Case-insensitivity is defined as by
 * `String#equalsIgnoreCase(String)`
 *
 * A `null` CharSequence will return `false`.
 *
 * ```
 * StringUtils.containsIgnoreCase(null, *) = false
 * StringUtils.containsIgnoreCase(*, null) = false
 * StringUtils.containsIgnoreCase("", "") = true
 * StringUtils.containsIgnoreCase("abc", "") = true
 * StringUtils.containsIgnoreCase("abc", "a") = true
 * StringUtils.containsIgnoreCase("abc", "z") = false
 * StringUtils.containsIgnoreCase("abc", "A") = true
 * StringUtils.containsIgnoreCase("abc", "Z") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchStr  the CharSequence to find, may be null
 * @return 대소문자와 관계없이 검색 CharSequence를 포함하면 true, 그렇지 않거나 입력이 `null`이면 false입니다.
 */
fun CharSequence.containsIgnoreCase(searchStr: CharSequence): Boolean =
    Strings.CI.contains(this, searchStr)

/**
 * CharSequence가 특정 문자를 포함하지 않는지 확인합니다.
 *
 * <p>A `null` CharSequence will return {@code true}.
 * A `null` invalid character array will return {@code true}.
 * An empty CharSequence (length()=0) always returns true.</p>
 *
 * ```
 * StringUtils.containsNone(*, null)       = true
 * StringUtils.containsNone("", *)         = true
 * StringUtils.containsNone("ab", '')      = true
 * StringUtils.containsNone("abab", 'xyz') = true
 * StringUtils.containsNone("ab1", 'xyz')  = true
 * StringUtils.containsNone("abz", 'xyz')  = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchChars 허용하지 않는 문자 배열입니다. `null`일 수 있습니다.
 * @return 허용하지 않는 문자를 포함하지 않거나 `null`이면 true입니다.
 */
fun CharSequence.containsNone(vararg searchChars: Char): Boolean =
    StringUtils.containsNone(this, *searchChars)

/**
 * CharSequence가 특정 문자를 포함하지 않는지 확인합니다.
 *
 * A `null` CharSequence will return {@code true}.
 * A `null` invalid character array will return `true`.
 * 빈 문자열("")은 항상 true를 반환합니다.
 *
 * ```
 * StringUtils.containsNone(*, null)       = true
 * StringUtils.containsNone("", *)         = true
 * StringUtils.containsNone("ab", "")      = true
 * StringUtils.containsNone("abab", "xyz") = true
 * StringUtils.containsNone("ab1", "xyz")  = true
 * StringUtils.containsNone("abz", "xyz")  = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @param invalidStr 허용하지 않는 문자 문자열입니다. `null`일 수 있습니다.
 * @return 허용하지 않는 문자를 포함하지 않거나 `null`이면 true입니다.
 * @since 2.0
 * @since 3.0 Changed signature from containsNone(String, String) to containsNone(CharSequence, String)
 */
fun CharSequence.containsNone(invalidStr: String?): Boolean =
    StringUtils.containsNone(this, invalidStr)


/**
 * CharSequence가 특정 문자만 포함하는지 확인합니다.
 *
 * A `null` CharSequence will return {@code false}.
 * A `null` valid character array will return `false`.
 * 빈 CharSequence(length()=0)는 항상 `true`를 반환합니다.
 *
 * ```
 * StringUtils.containsOnly("", *)         = true
 * StringUtils.containsOnly("ab", '')      = false
 * StringUtils.containsOnly("abab", 'abc') = true
 * StringUtils.containsOnly("ab1", 'abc')  = false
 * StringUtils.containsOnly("abz", 'abc')  = false
 * ```
 *
 * @receiver  the String to check
 * @param validChars 허용할 문자 배열입니다.
 * @return `null`이 아니고 허용 문자만 포함하면 true입니다.
 */
fun CharSequence.containsOnly(vararg validChars: Char): Boolean =
    StringUtils.containsOnly(this, *validChars)

/**
 * CharSequence가 특정 문자만 포함하는지 확인합니다.
 *
 * A `null` valid character String will return `false`.
 * An empty String (length()=0) always returns `true`.
 *
 * ```
 * StringUtils.containsOnly(*, null)       = false
 * StringUtils.containsOnly("", *)         = true
 * StringUtils.containsOnly("ab", "")      = false
 * StringUtils.containsOnly("abab", "abc") = true
 * StringUtils.containsOnly("ab1", "abc")  = false
 * StringUtils.containsOnly("abz", "abc")  = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param validStr 허용할 문자 문자열입니다. `null`일 수 있습니다.
 * @return `null`이 아니고 허용 문자만 포함하면 true입니다.
 * @since 2.0
 * @since 3.0 Changed signature from containsOnly(String, String) to containsOnly(CharSequence, String)
 */
fun CharSequence.containsOnly(validStr: String?): Boolean =
    StringUtils.containsOnly(this, validStr)

/**
 * 주어진 CharSequence가 공백 문자를 포함하는지 확인합니다.
 *
 * 공백은 [Character#isWhitespace(char)] 기준으로 정의합니다.
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @return `true` if the CharSequence is not empty and
 * contains at least 1 (breaking) whitespace character
 */
fun CharSequence.containsWhitespace(): Boolean =
    StringUtils.containsWhitespace(this)

/**
 * 지정한 문자열에 문자가 몇 번 등장하는지 셉니다.
 *
 * A `null` or empty ("") String input returns `0`.
 *
 * ```
 * StringUtils.countMatches(null, *)       = 0
 * StringUtils.countMatches("", *)         = 0
 * StringUtils.countMatches("abba", 0)  = 0
 * StringUtils.countMatches("abba", 'a')   = 2
 * StringUtils.countMatches("abba", 'b')  = 2
 * StringUtils.countMatches("abba", 'x') = 0
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param ch 셀 문자입니다.
 * @return 등장 횟수입니다. CharSequence가 `null`이면 0입니다.
 */
fun CharSequence.countMatches(ch: Char): Int =
    StringUtils.countMatches(this, ch)

/**
 * 큰 문자열 안에 부분 문자열이 몇 번 등장하는지 셉니다.
 * 겹치지 않는 매치만 셉니다.
 *
 * A `null` or empty ("") String input returns {@code 0}.
 *
 * ```
 * StringUtils.countMatches(null, *)       = 0
 * StringUtils.countMatches("", *)         = 0
 * StringUtils.countMatches("abba", null)  = 0
 * StringUtils.countMatches("abba", "")    = 0
 * StringUtils.countMatches("abba", "a")   = 2
 * StringUtils.countMatches("abba", "ab")  = 1
 * StringUtils.countMatches("abba", "xxx") = 0
 * StringUtils.countMatches("ababa", "aba") = 1
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param sub 셀 부분 문자열입니다. `null`일 수 있습니다.
 * @return 등장 횟수입니다. 어느 CharSequence라도 `null`이면 0입니다.
 */
fun CharSequence.countMatches(sub: CharSequence): Int =
    StringUtils.countMatches(this, sub)


/**
 * defaultIfWhitespace 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = "   ".defaultIfWhitespace("fallback")
 * // result == "fallback"
 * ```
 */
fun <T: CharSequence> T.defaultIfWhitespace(defaultValue: T): T = this.ifBlank { defaultValue }

/**
 * defaultIfEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = "".defaultIfEmpty("fallback")
 * // result == "fallback"
 * ```
 */
fun <T: CharSequence> T.defaultIfEmpty(defaultValue: T): T = this.ifEmpty { defaultValue }

/**
 * Deletes all whitespaces from a String as defined by
 * `Char.isWhitespace()`
 *
 * ```
 * StringUtils.deleteWhitespace("")           = ""
 * StringUtils.deleteWhitespace("abc")        = "abc"
 * StringUtils.deleteWhitespace("   ab  c  ") = "abc"
 * ```
 *
 * @receiver 공백을 제거할 문자열입니다. `null`일 수 있습니다.
 * @return the String without whitespaces, `null` if null String input
 */
fun String.deleteWhitespace(): String = StringUtils.deleteWhitespace(this)

/**
 * 두 문자열을 비교하고 서로 달라지는 부분을 반환합니다.
 * More precisely, return the remainder of the second String,
 * starting from where it's different from the first. This means that
 * the difference between "abc" and "ab" is the empty String and not "c".
 *
 * For example,
 * ```
 * difference("i am a machine", "i am a robot") -> "robot"}
 * ```
 *
 * ```
 * StringUtils.difference(null, null)       = null
 * StringUtils.difference("", "")           = ""
 * StringUtils.difference("", "abc")        = "abc"
 * StringUtils.difference("abc", "")        = ""
 * StringUtils.difference("abc", "abc")     = ""
 * StringUtils.difference("abc", "ab")      = ""
 * StringUtils.difference("ab", "abxyz")    = "xyz"
 * StringUtils.difference("abcde", "abxyz") = "xyz"
 * StringUtils.difference("abcde", "xyz")   = "xyz"
 * ```
 *
 * @receiver 첫 번째 문자열입니다.
 * @param other 두 번째 문자열입니다. `null`일 수 있습니다.
 * @return `str1`과 다른 `str2`의 부분입니다. 같으면 빈 문자열을 반환합니다.
 */
fun String.deference(other: String): String = StringUtils.difference(this, other)


/**
 * endsWithAny 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = "readme.md".endsWithAny(".txt", ".md")
 * // result == true
 * ```
 */
fun String.endsWithAny(vararg searchStrings: String): Boolean = Strings.CS.endsWithAny(this, *searchStrings)

/**
 * endsWithIgnoreCase 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = "README.MD".endsWithIgnoreCase("md")
 * // result == true
 * ```
 */
fun String.endsWithIgnoreCase(suffix: CharSequence): Boolean = Strings.CI.endsWith(this, suffix)

/**
 * 문자열이 유니코드 숫자를 포함하는지 확인하고,
 * if yes then concatenate all the digits in String and return it as a String.
 *
 * {@code str\}에서 숫자를 찾지 못하면 빈 문자열("")을 반환합니다.
 *
 * ```
 * StringUtils.getDigits("")                   = ""
 * StringUtils.getDigits("abc")                = ""
 * StringUtils.getDigits("1000$")              = "1000"
 * StringUtils.getDigits("1123~45")            = "112345"
 * StringUtils.getDigits("(541) 754-3010")     = "5417543010"
 * StringUtils.getDigits("\u0967\u0968\u0969") = "\u0967\u0968\u0969"
 * ```
 *
 * @receiver 숫자만 추출할 문자열입니다.
 * @return 숫자만 포함한 문자열입니다,
 *           or an empty ("") String if no digits found,
 *           or `null` String if {@code str} is null
 */
fun String.getDigits(): String = StringUtils.getDigits(this)


/**
 * 후보 부분 문자열 집합 중 하나가 처음 나타나는 인덱스를 찾습니다.
 *
 * A `null` CharSequence will return `-1`.
 * A `null` or zero length search array will return `-1`.
 * A `null` search array entry will be ignored, but a search
 * array containing "" will return `0` if `receiver` is not
 * null. This method uses `String#indexOf(String)` if possible.
 *
 * ```
 * StringUtils.indexOfAny(*, null)                      = -1
 * StringUtils.indexOfAny(*, [])                        = -1
 * StringUtils.indexOfAny("zzabyycdxx", ["ab", "cd"])   = 2
 * StringUtils.indexOfAny("zzabyycdxx", ["cd", "ab"])   = 2
 * StringUtils.indexOfAny("zzabyycdxx", ["mn", "op"])   = -1
 * StringUtils.indexOfAny("zzabyycdxx", ["zab", "aby"]) = 1
 * StringUtils.indexOfAny("zzabyycdxx", [""])           = 0
 * StringUtils.indexOfAny("", [""])                     = 0
 * StringUtils.indexOfAny("", ["a"])                    = -1
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchStrs 검색할 CharSequence입니다. `null`일 수 있습니다.
 * @return `str`에서 `searchStrs` 중 하나가 처음 나타나는 인덱스입니다. 매치가 없으면 -1입니다.
 */
fun CharSequence.indexOfAny(vararg searchStrs: CharSequence): Int = StringUtils.indexOfAny(this, *searchStrs)

/**
 * Search a CharSequence to find the first index of any
 * character in the given 확인합니다.
 *
 * A `null` String will return `-1`.
 * A `null` search string will return `-1`.
 *
 * ```
 * StringUtils.indexOfAny("", *)              = -1
 * StringUtils.indexOfAny(*, null)            = -1
 * StringUtils.indexOfAny(*, "")              = -1
 * StringUtils.indexOfAny("zzabyycdxx", "za") = 0
 * StringUtils.indexOfAny("zzabyycdxx", "by") = 3
 * StringUtils.indexOfAny("aba", "z")         = -1
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param searchStr 검색할 문자입니다. `null`일 수 있습니다.
 * @return 문자 중 하나의 인덱스입니다. 매치가 없거나 입력이 `null`이면 -1입니다.
 */
fun CharSequence.indexOfAny(searchStr: String?): Int = StringUtils.indexOfAny(this, searchStr)


/**
 * 배열의 모든 CharSequence를 비교하고
 * CharSequences begin to differ.
 *
 * For example,
 * ```
 * indexOfDifference(new String[] {"i am a machine", "i am a robot"}) -> 7
 * ```
 *
 * ```
 * StringUtils.indexOfDifference(null)                             = -1
 * StringUtils.indexOfDifference(new String[] {})                  = -1
 * StringUtils.indexOfDifference(new String[] {"abc"})             = -1
 * StringUtils.indexOfDifference(new String[] {null, null})        = -1
 * StringUtils.indexOfDifference(new String[] {"", ""})            = -1
 * StringUtils.indexOfDifference(new String[] {"", null})          = 0
 * StringUtils.indexOfDifference(new String[] {"abc", null, null}) = 0
 * StringUtils.indexOfDifference(new String[] {null, null, "abc"}) = 0
 * StringUtils.indexOfDifference(new String[] {"", "abc"})         = 0
 * StringUtils.indexOfDifference(new String[] {"abc", ""})         = 0
 * StringUtils.indexOfDifference(new String[] {"abc", "abc"})      = -1
 * StringUtils.indexOfDifference(new String[] {"abc", "a"})        = 1
 * StringUtils.indexOfDifference(new String[] {"ab", "abxyz"})     = 2
 * StringUtils.indexOfDifference(new String[] {"abcde", "abxyz"})  = 2
 * StringUtils.indexOfDifference(new String[] {"abcde", "xyz"})    = 0
 * StringUtils.indexOfDifference(new String[] {"xyz", "abcde"})    = 0
 * StringUtils.indexOfDifference(new String[] {"i am a machine", "i am a robot"}) = 7
 * ```
 *
 * @param css CharSequence 배열입니다. 각 항목은 `null`일 수 있습니다.
 * @return 문자열들이 달라지기 시작하는 인덱스입니다. 모두 같으면 -1입니다.
 */
fun indexOfDifference(vararg css: CharSequence): Int = StringUtils.indexOfDifference(*css)


/**
 * 두 CharSequence를 비교하고
 * CharSequences begin to differ.
 *
 * For example,
 * ```
 * indexOfDifference("i am a machine", "i am a robot") -> 7
 * ```
 *
 * ```
 * StringUtils.indexOfDifference("", "")           = -1
 * StringUtils.indexOfDifference("", "abc")        = 0
 * StringUtils.indexOfDifference("abc", "")        = 0
 * StringUtils.indexOfDifference("abc", "abc")     = -1
 * StringUtils.indexOfDifference("ab", "abxyz")    = 2
 * StringUtils.indexOfDifference("abcde", "abxyz") = 2
 * StringUtils.indexOfDifference("abcde", "xyz")   = 0
 * ```
 *
 * @receiver 첫 번째 CharSequence입니다.
 * @param other 두 번째 CharSequence입니다. `null`일 수 있습니다.
 * @return `cs1`과 `cs2`가 달라지기 시작하는 인덱스입니다. 같으면 -1입니다.
 */
fun CharSequence.indexOfDifference(other: CharSequence?): Int = StringUtils.indexOfDifference(this, other)

/**
 * indexOfIgnoreCase 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = "Hello".indexOfIgnoreCase("he")
 * // result == 0
 * ```
 */
fun CharSequence.indexOfIgnoreCase(searchStr: CharSequence, startPos: Int = 0): Int =
    Strings.CI.indexOf(this, searchStr, startPos)

/**
 * CharSequence가 유니코드 문자만 포함하는지 확인합니다.
 *
 * 빈 CharSequence(length()=0)는 `false`를 반환합니다.
 *
 * ```
 * StringUtils.isAlpha("")     = false
 * StringUtils.isAlpha("  ")   = false
 * StringUtils.isAlpha("abc")  = true
 * StringUtils.isAlpha("ab2c") = false
 * StringUtils.isAlpha("ab-c") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @return `null`이 아니고 문자만 포함하면 `true`입니다.
 */
fun CharSequence.isAlpha(): Boolean = StringUtils.isAlpha(this)

/**
 * CharSequence가 유니코드 문자 또는 숫자만 포함하는지 확인합니다.
 *
 * 빈 CharSequence(length()=0)는 `false`를 반환합니다.
 *
 * ```
 * StringUtils.isAlphanumeric("")     = false
 * StringUtils.isAlphanumeric("  ")   = false
 * StringUtils.isAlphanumeric("abc")  = true
 * StringUtils.isAlphanumeric("ab c") = false
 * StringUtils.isAlphanumeric("ab2c") = true
 * StringUtils.isAlphanumeric("ab-c") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 문자 또는 숫자만 포함하면 {@code true\}입니다.
 */
fun CharSequence.isAlphaNumeric(): Boolean = StringUtils.isAlphanumeric(this)

/**
 * CharSequence가 유니코드 문자, 숫자
 * or space ({@code ' '}).
 *
 * 빈 CharSequence(length()=0)는 `true`를 반환합니다.
 *
 * ```
 * StringUtils.isAlphanumericSpace("")     = true
 * StringUtils.isAlphanumericSpace("  ")   = true
 * StringUtils.isAlphanumericSpace("abc")  = true
 * StringUtils.isAlphanumericSpace("ab c") = true
 * StringUtils.isAlphanumericSpace("ab2c") = true
 * StringUtils.isAlphanumericSpace("ab-c") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 문자, 숫자 또는 공백만 포함하면 `true`입니다.
 */
fun CharSequence.isAlphaNumericSpace(): Boolean = StringUtils.isAlphanumericSpace(this)

/**
 * CharSequence가 유니코드 문자 또는 공백만 포함하는지 확인합니다
 * ({@code ' '}).
 *
 * 빈 CharSequence(length()=0)는 `true`를 반환합니다.
 *
 * ```
 * StringUtils.isAlphaSpace("")     = true
 * StringUtils.isAlphaSpace("  ")   = true
 * StringUtils.isAlphaSpace("abc")  = true
 * StringUtils.isAlphaSpace("ab c") = true
 * StringUtils.isAlphaSpace("ab2c") = false
 * StringUtils.isAlphaSpace("ab-c") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 문자 또는 공백만 포함하면 `true`입니다.
 */
fun CharSequence.isAlphaSpace(): Boolean = StringUtils.isAlphaSpace(this)


/**
 * CharSequence가 출력 가능한 ASCII 문자만 포함하는지 확인합니다.
 *
 * An empty CharSequence (length()=0) will return `true`
 *
 * ```
 * StringUtils.isAsciiPrintable("")       = true
 * StringUtils.isAsciiPrintable(" ")      = true
 * StringUtils.isAsciiPrintable("Ceki")   = true
 * StringUtils.isAsciiPrintable("ab2c")   = true
 * StringUtils.isAsciiPrintable("!ab-c~") = true
 * StringUtils.isAsciiPrintable("\u0020") = true
 * StringUtils.isAsciiPrintable("\u0021") = true
 * StringUtils.isAsciiPrintable("\u007e") = true
 * StringUtils.isAsciiPrintable("\u007f") = false
 * StringUtils.isAsciiPrintable("Ceki G\u00fclc\u00fc") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return 모든 문자가 32부터 126 범위에 있으면 `true`입니다.
 */
fun CharSequence.isAsciiPrintable(): Boolean = StringUtils.isAsciiPrintable(this)

/**
 * CharSequence가 대문자와 소문자를 모두 포함하는지 확인합니다.
 *
 * An empty CharSequence ({@code length()=0}) will return `false`
 *
 * ```
 * StringUtils.isMixedCase("")      = false
 * StringUtils.isMixedCase(" ")     = false
 * StringUtils.isMixedCase("ABC")   = false
 * StringUtils.isMixedCase("abc")   = false
 * StringUtils.isMixedCase("aBc")   = true
 * StringUtils.isMixedCase("A c")   = true
 * StringUtils.isMixedCase("A1c")   = true
 * StringUtils.isMixedCase("a/C")   = true
 * StringUtils.isMixedCase("aC\t")  = true
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return CharSequence가 대문자와 소문자를 모두 포함하면 `true`입니다.
 */
fun CharSequence.isMixedCase(): Boolean = StringUtils.isMixedCase(this)

/**
 * CharSequence가 유니코드 숫자만 포함하는지 확인합니다.
 * A decimal point is not a Unicode digit and returns false.
 *
 * <p>`null` will return {@code false}.
 * An empty CharSequence (length()=0) will return {@code false}.</p>
 *
 * <p>Note that the method does not allow for a leading sign, either positive or negative.
 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
 * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
 * for int or long respectively.</p>
 *
 * ```
 * StringUtils.isNumeric("")     = false
 * StringUtils.isNumeric("  ")   = false
 * StringUtils.isNumeric("123")  = true
 * StringUtils.isNumeric("\u0967\u0968\u0969")  = true
 * StringUtils.isNumeric("12 3") = false
 * StringUtils.isNumeric("ab2c") = false
 * StringUtils.isNumeric("12-3") = false
 * StringUtils.isNumeric("12.3") = false
 * StringUtils.isNumeric("-123") = false
 * StringUtils.isNumeric("+123") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 숫자만 포함하면 `true`입니다.
 */
fun CharSequence.isNumeric(): Boolean = StringUtils.isNumeric(this)

/**
 * CharSequence가 유니코드 숫자 또는 공백(`' '`)만 포함하는지 확인합니다.
 * A decimal point is not a Unicode digit and returns false.
 *
 * 빈 CharSequence(length()=0)는 `true`를 반환합니다.
 *
 * ```
 * StringUtils.isNumericSpace(null)   = false
 * StringUtils.isNumericSpace("")     = true
 * StringUtils.isNumericSpace("  ")   = true
 * StringUtils.isNumericSpace("123")  = true
 * StringUtils.isNumericSpace("12 3") = true
 * StringUtils.isNumericSpace("\u0967\u0968\u0969")   = true
 * StringUtils.isNumericSpace("\u0967\u0968 \u0969")  = true
 * StringUtils.isNumericSpace("ab2c") = false
 * StringUtils.isNumericSpace("12-3") = false
 * StringUtils.isNumericSpace("12.3") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 숫자 또는 공백만 포함하면 `true`입니다.
 */
fun CharSequence.isNumericSpace(): Boolean = StringUtils.isNumericSpace(this)

/**
 * CharSequence가 공백만 포함하는지 확인합니다.
 *
 * 공백은 \{@link Character#isWhitespace(char)\} 기준으로 정의합니다.
 *
 * An empty CharSequence (length()=0) will return `true`
 *
 * ```
 * StringUtils.isWhitespace("")     = true
 * StringUtils.isWhitespace("  ")   = true
 * StringUtils.isWhitespace("abc")  = false
 * StringUtils.isWhitespace("ab2c") = false
 * StringUtils.isWhitespace("ab-c") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @return `null`이 아니고 공백만 포함하면 `true`입니다.
 */
fun CharSequence.isWhiteSpace(): Boolean = StringUtils.isWhitespace(this)

/**
 * 후보 부분 문자열 집합 중 하나가 마지막으로 나타나는 인덱스를 찾습니다.
 *
 * <p>A `null` CharSequence will return {@code -1}.
 * A `null` search array will return {@code -1}.
 * A `null` or zero length search array entry will be ignored,
 * but a search array containing "" will return the length of {@code str}
 * if {@code str} is not null. This method uses {@link String#indexOf(String)} if possible</p>
 *
 * ```
 * StringUtils.lastIndexOfAny(*, [])                      = -1
 * StringUtils.lastIndexOfAny("zzabyycdxx", ["ab", "cd"]) = 6
 * StringUtils.lastIndexOfAny("zzabyycdxx", ["cd", "ab"]) = 6
 * StringUtils.lastIndexOfAny("zzabyycdxx", ["mn", "op"]) = -1
 * StringUtils.lastIndexOfAny("zzabyycdxx", ["mn", "op"]) = -1
 * StringUtils.lastIndexOfAny("zzabyycdxx", ["mn", ""])   = 10
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @param searchStrs 검색할 CharSequence입니다. `null`일 수 있습니다.
 * @return CharSequence 중 하나가 마지막으로 나타나는 인덱스입니다. 매치가 없으면 -1입니다.
 */
fun CharSequence.lastIndexOfAny(vararg searchStrs: CharSequence): Int =
    StringUtils.lastIndexOfAny(this, *searchStrs)

/**
 * Case in-sensitive find of the last index within a CharSequence
 * from the specified position.
 *
 * A negative start position returns {@code -1}.
 * 빈 검색 CharSequence("")는 시작 위치가 음수가 아니면 항상 일치합니다.
 * A start position greater than the string length searches the whole string.
 * The search starts at the startPos and works backwards; matches starting after the start
 * position are ignored.
 *
 * ```
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "A", 8)  = 7
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "B", 8)  = 5
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "AB", 8) = 4
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "B", 9)  = 5
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "B", -1) = -1
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "A", 0)  = 0
 * StringUtils.lastIndexOfIgnoreCase("aabaabaa", "B", 0)  = -1
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @param searchStr  the CharSequence to find, may be null
 * @param startPos  the start position
 * @return 검색 CharSequence의 마지막 인덱스입니다(항상 startPos 이하). 매치가 없으면 -1입니다.
 */
fun CharSequence.lastIndexOfIgnoreCase(searchStr: CharSequence?, startPos: Int = length): Int =
    Strings.CI.lastIndexOf(this, searchStr, startPos)

/**
 * 지정한 문자로 문자열 왼쪽을 채웁니다.
 *
 * <p>Pad to a size of {@code size}.</p>
 *
 * ```
 * StringUtils.leftPad("", 3, 'z')     = "zzz"
 * StringUtils.leftPad("bat", 3, 'z')  = "bat"
 * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
 * StringUtils.leftPad("bat", 1, 'z')  = "bat"
 * StringUtils.leftPad("bat", -1, 'z') = "bat"
 * ```
 *
 * @receiver 채울 대상 문자열입니다.
 * @param size 채운 뒤의 크기입니다.
 * @param padChar 채움에 사용할 문자입니다.
 * @return 왼쪽이 채워진 문자열입니다. 채움이 필요 없으면 원래 문자열입니다,
 *  `null` if null String input
 * @since 2.0
 */
fun String.leftPad(size: Int, padChar: Char = ' '): String =
    StringUtils.leftPad(this, size, padChar)

/**
 * 지정한 문자열로 문자열 왼쪽을 채웁니다.
 *
 * `size` 크기가 되도록 채웁니다.
 *
 * ```
 * StringUtils.leftPad("", 3, "z")      = "zzz"
 * StringUtils.leftPad("bat", 3, "yz")  = "bat"
 * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
 * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
 * StringUtils.leftPad("bat", 1, "yz")  = "bat"
 * StringUtils.leftPad("bat", -1, "yz") = "bat"
 * StringUtils.leftPad("bat", 5, "")    = "  bat"
 * ```
 *
 * @receiver 채울 대상 문자열입니다.
 * @param size 채운 뒤의 크기입니다.
 * @param padStr 채움에 사용할 문자열입니다. `null` 또는 빈 값은 단일 공백으로 처리합니다.
 * @return 왼쪽이 채워진 문자열입니다. 채움이 필요 없으면 원래 문자열입니다,
 *  `null` if null String input
 */
fun String.leftPad(size: Int, padStr: String): String =
    StringUtils.leftPad(this, size, padStr)


/**
 * 원본 문자열 안의 모든 부분 문자열 등장 위치를 제거합니다.
 *
 * 빈 원본 문자열("")은 빈 문자열을 반환합니다.
 * A `null` remove string will return the source string.
 * 빈 제거 문자열("")은 원본 문자열을 반환합니다.
 *
 * ```
 * StringUtils.remove("", *)          = ""
 * StringUtils.remove(*, null)        = *
 * StringUtils.remove(*, "")          = *
 * StringUtils.remove("queued", "ue") = "qd"
 * StringUtils.remove("queued", "zz") = "queued"
 * ```
 *
 * @receiver 검색할 원본 문자열입니다.
 * @param remove 검색 후 제거할 문자열입니다. `null`일 수 있습니다.
 * @return 문자열을 찾으면 제거한 부분 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.remove(remove: String?): String = Strings.CS.remove(this, remove)

/**
 * Removes a substring only if it is at the end of a source string,
 * otherwise returns the source string.
 *
 * 빈 원본 문자열("")은 빈 문자열을 반환합니다.
 * A `null` search string will return the source string.</p>
 *
 * ```
 * StringUtils.removeEnd("", *)        = ""
 * StringUtils.removeEnd(*, null)      = *
 * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
 * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
 * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
 * StringUtils.removeEnd("abc", "")    = "abc"
 * ```
 *
 * @receiver 검색할 원본 문자열입니다. `null`일 수 있습니다.
 * @param remove 검색 후 제거할 문자열입니다. `null`일 수 있습니다.
 * @return 문자열을 찾으면 제거한 부분 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.removeEnd(remove: String): String = Strings.CS.removeEnd(this, remove)

/**
 * 원본 문자열 끝에 있는 부분 문자열을 대소문자 구분 없이 제거합니다,
 * otherwise returns the source string.
 *
 * 빈 원본 문자열("")은 빈 문자열을 반환합니다.
 * A `null` search string will return the source string.
 *
 * ```
 * StringUtils.removeEndIgnoreCase("", *)        = ""
 * StringUtils.removeEndIgnoreCase(*, null)      = *
 * StringUtils.removeEndIgnoreCase("www.domain.com", ".com.")  = "www.domain.com"
 * StringUtils.removeEndIgnoreCase("www.domain.com", ".com")   = "www.domain"
 * StringUtils.removeEndIgnoreCase("www.domain.com", "domain") = "www.domain.com"
 * StringUtils.removeEndIgnoreCase("abc", "")    = "abc"
 * StringUtils.removeEndIgnoreCase("www.domain.com", ".COM") = "www.domain")
 * StringUtils.removeEndIgnoreCase("www.domain.COM", ".com") = "www.domain")
 * ```
 *
 * @receiver 검색할 원본 문자열입니다. `null`일 수 있습니다.
 * @param remove  the String to search for (case-insensitive) and remove, may be null
 * @return 문자열을 찾으면 제거한 부분 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.removeEndIgnoreCase(remove: String): String = Strings.CI.removeEnd(this, remove)

/**
 * Removes a substring only if it is at the beginning of a source string,
 * otherwise returns the source string.
 *
 * 빈 원본 문자열("")은 빈 문자열을 반환합니다.
 * A `null` search string will return the source string.
 *
 * ```
 * StringUtils.removeStart(null, *)      = null
 * StringUtils.removeStart("", *)        = ""
 * StringUtils.removeStart(*, null)      = *
 * StringUtils.removeStart("www.domain.com", "www.")   = "domain.com"
 * StringUtils.removeStart("domain.com", "www.")       = "domain.com"
 * StringUtils.removeStart("www.domain.com", "domain") = "www.domain.com"
 * StringUtils.removeStart("abc", "")    = "abc"
 * ```
 *
 * @receiver 검색할 원본 문자열입니다.
 * @param remove 검색 후 제거할 문자열입니다. `null`일 수 있습니다.
 * @return 문자열을 찾으면 제거한 부분 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.removeStart(remove: String): String = Strings.CS.removeStart(this, remove)

/**
 * 원본 문자열 시작에 있는 부분 문자열을 대소문자 구분 없이 제거합니다,
 * otherwise returns the source string.
 *
 * 빈 원본 문자열("")은 빈 문자열을 반환합니다.
 * A `null` search string will return the source string.
 *
 * ```
 * StringUtils.removeStartIgnoreCase(null, *)      = null
 * StringUtils.removeStartIgnoreCase("", *)        = ""
 * StringUtils.removeStartIgnoreCase(*, null)      = *
 * StringUtils.removeStartIgnoreCase("www.domain.com", "www.")   = "domain.com"
 * StringUtils.removeStartIgnoreCase("www.domain.com", "WWW.")   = "domain.com"
 * StringUtils.removeStartIgnoreCase("domain.com", "www.")       = "domain.com"
 * StringUtils.removeStartIgnoreCase("www.domain.com", "domain") = "www.domain.com"
 * StringUtils.removeStartIgnoreCase("abc", "")    = "abc"
 * ```
 *
 * @receiver 검색할 원본 문자열입니다. `null`일 수 있습니다.
 * @param remove  the String to search for (case-insensitive) and remove, may be null
 * @return the substring with the string removed if found,
 *  `null` if null String input
 * @since 2.4
 */
fun String.removeStartIgnoreCase(remove: String): String = Strings.CI.removeStart(this, remove)

/**
 * Repeat a String {@code repeat} times to form a
 * new String, with a String separator injected each time.
 *
 * ```
 * StringUtils.repeat("", null, 0)   = ""
 * StringUtils.repeat("", "", 2)     = ""
 * StringUtils.repeat("", "x", 3)    = "xx"
 * StringUtils.repeat("?", ", ", 3)  = "?, ?, ?"
 * ```
 *
 * @receiver        the String to repeat
 * @param separator  the String to inject, may be null
 * @param repeat     number of times to repeat str, negative treated as zero
 * @return a new String consisting of the original String repeated, `null` if null String input
 */
fun String.repeat(separator: String?, repeat: Int): String = StringUtils.repeat(this, separator, repeat)

/**
 * 대소문자를 무시하고 큰 문자열 안의 문자열을 다른 문자열로 교체합니다,
 * for the first {@code max} values of the search String.
 *
 *
 * ```
 * StringUtils.replaceIgnoreCase("", *, *, *)           = ""
 * StringUtils.replaceIgnoreCase("any", null, *, *)     = "any"
 * StringUtils.replaceIgnoreCase("any", *, null, *)     = "any"
 * StringUtils.replaceIgnoreCase("any", "", *, *)       = "any"
 * StringUtils.replaceIgnoreCase("any", *, *, 0)        = "any"
 * StringUtils.replaceIgnoreCase("abaa", "a", null, -1) = "abaa"
 * StringUtils.replaceIgnoreCase("abaa", "a", "", -1)   = "b"
 * StringUtils.replaceIgnoreCase("abaa", "a", "z", 0)   = "abaa"
 * StringUtils.replaceIgnoreCase("abaa", "A", "z", 1)   = "zbaa"
 * StringUtils.replaceIgnoreCase("abAa", "a", "z", 2)   = "zbza"
 * StringUtils.replaceIgnoreCase("abAa", "a", "z", -1)  = "zbzz"
 * ```
 *
 * @receiver 검색하고 치환할 텍스트입니다.
 * @param searchStr 대소문자를 무시하고 검색할 문자열입니다. `null`일 수 있습니다.
 * @param replacement 치환에 사용할 문자열입니다. `null`일 수 있습니다.
 * @param max  maximum number of values to replace, or {@code -1} if no maximum
 * @return 치환을 처리한 텍스트입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.replaceIgnoreCase(searchStr: String?, replacement: String?, max: Int = -1): String =
    Strings.CI.replace(this, searchStr, replacement, max)

/**
 * 큰 문자열 안의 문자열을 다른 문자열로 한 번 교체합니다.
 *
 * ```
 * StringUtils.replaceOnce(null, *, *)        = null
 * StringUtils.replaceOnce("", *, *)          = ""
 * StringUtils.replaceOnce("any", null, *)    = "any"
 * StringUtils.replaceOnce("any", *, null)    = "any"
 * StringUtils.replaceOnce("any", "", *)      = "any"
 * StringUtils.replaceOnce("aba", "a", null)  = "aba"
 * StringUtils.replaceOnce("aba", "a", "")    = "ba"
 * StringUtils.replaceOnce("aba", "a", "z")   = "zba"
 * ```
 *
 * @receiver 검색하고 치환할 텍스트입니다.
 * @param searchString 검색할 문자열입니다. `null`일 수 있습니다.
 * @param replacement 치환에 사용할 문자열입니다. `null`일 수 있습니다.
 * @return 치환을 처리한 텍스트입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.replaceOnce(searchString: String?, replacement: String?): String {
    return Strings.CS.replace(this, searchString, replacement, 1)
}

/**
 * 대소문자를 무시하고 큰 문자열 안의 문자열을 다른 문자열로 한 번 교체합니다.
 *
 * ```
 * StringUtils.replaceOnceIgnoreCase(null, *, *)        = null
 * StringUtils.replaceOnceIgnoreCase("", *, *)          = ""
 * StringUtils.replaceOnceIgnoreCase("any", null, *)    = "any"
 * StringUtils.replaceOnceIgnoreCase("any", *, null)    = "any"
 * StringUtils.replaceOnceIgnoreCase("any", "", *)      = "any"
 * StringUtils.replaceOnceIgnoreCase("aba", "a", null)  = "aba"
 * StringUtils.replaceOnceIgnoreCase("aba", "a", "")    = "ba"
 * StringUtils.replaceOnceIgnoreCase("aba", "a", "z")   = "zba"
 * StringUtils.replaceOnceIgnoreCase("FoOFoofoo", "foo", "") = "Foofoo"
 * ```
 *
 * @see replaceIgnoreCase
 * @receiver 검색하고 치환할 텍스트입니다.
 * @param searchString 대소문자를 무시하고 검색할 문자열입니다. `null`일 수 있습니다.
 * @param replacement 치환에 사용할 문자열입니다. `null`일 수 있습니다.
 * @return 치환을 처리한 텍스트입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.replaceOnceIgnoreCase(searchString: String?, replacement: String?): String {
    return Strings.CI.replace(this, searchString, replacement, 1)
}

/**
 * 문자열의 오른쪽 끝 {@code len\}개 문자를 가져옵니다.
 *
 * If `len` characters are not available, or the String
 * is `null`, the String will be returned without an
 * an exception. An empty String is returned if len is negative.
 *
 * ```
 * StringUtils.right(*, -ve)     = ""
 * StringUtils.right("", *)      = ""
 * StringUtils.right("abc", 0)   = ""
 * StringUtils.right("abc", 2)   = "bc"
 * StringUtils.right("abc", 4)   = "abc"
 * ```
 *
 * @receiver 오른쪽 끝 문자를 가져올 문자열입니다. `null`일 수 있습니다.
 * @param len 필요한 문자열 길이입니다.
 * @return 오른쪽 끝 문자입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.right(len: Int): String = StringUtils.right(this, len)

/**
 * 지정한 문자로 문자열 오른쪽을 채웁니다.
 *
 * 문자열을 `size` 크기가 되도록 채웁니다.
 *
 * ```
 * StringUtils.rightPad("", 3, 'z')     = "zzz"
 * StringUtils.rightPad("bat", 3, 'z')  = "bat"
 * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
 * StringUtils.rightPad("bat", 1, 'z')  = "bat"
 * StringUtils.rightPad("bat", -1, 'z') = "bat"
 * ```
 *
 * @receiver 채울 대상 문자열입니다.
 * @param size 채운 뒤의 크기입니다.
 * @param padChar 채움에 사용할 문자입니다.
 * @return 오른쪽이 채워진 문자열입니다. 채움이 필요 없으면 원래 문자열, 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.rightPad(size: Int, padChar: Char = ' '): String = StringUtils.rightPad(this, size, padChar)

/**
 * 지정한 문자열로 문자열 오른쪽을 채웁니다.
 *
 * 문자열을 `size` 크기가 되도록 채웁니다.
 *
 * ```
 * StringUtils.rightPad("", 3, "z")      = "zzz"
 * StringUtils.rightPad("bat", 3, "yz")  = "bat"
 * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
 * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
 * StringUtils.rightPad("bat", 1, "yz")  = "bat"
 * StringUtils.rightPad("bat", -1, "yz") = "bat"
 * StringUtils.rightPad("bat", 5, null)  = "bat  "
 * StringUtils.rightPad("bat", 5, "")    = "bat  "
 * ```
 *
 * @receiver 채울 대상 문자열입니다.
 * @param size 채운 뒤의 크기입니다.
 * @param padStr 채움에 사용할 문자열입니다. `null` 또는 빈 값은 단일 공백으로 처리합니다.
 * @return 오른쪽이 채워진 문자열입니다. 채움이 필요 없으면 원래 문자열, 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.rightPad(size: Int, padStr: String): String = StringUtils.rightPad(this, size, padStr)

/**
 * CharSequence가 제공된 대소문자 구분 접두사 중 하나로 시작하는지 확인합니다.
 *
 * ```
 * StringUtils.startsWithAny(null, new String[] {"abc"})  = false
 * StringUtils.startsWithAny("abcxyz", null)     = false
 * StringUtils.startsWithAny("abcxyz", new String[] {""}) = true
 * StringUtils.startsWithAny("abcxyz", new String[] {"abc"}) = true
 * StringUtils.startsWithAny("abcxyz", new String[] {null, "xyz", "abc"}) = true
 * StringUtils.startsWithAny("abcxyz", null, "xyz", "ABCX") = false
 * StringUtils.startsWithAny("ABCXYZ", null, "xyz", "abc") = false
 * ```
 *
 * @receiver 검사할 CharSequence입니다.
 * @param searchStrs 대소문자를 구분하는 CharSequence 접두사입니다. 비어 있거나 `null`일 수 있습니다.
 * @see StringUtils#startsWith(CharSequence, CharSequence)
 * @return 입력 {@code sequence\}가 `null`이고 {@code searchStrings\}가 제공되지 않았거나,
 *   the input {@code sequence} begins with any of the provided case-sensitive {@code searchStrings}.
 */
fun CharSequence.startsWithAny(vararg searchStrs: CharSequence?): Boolean =
    Strings.CS.startsWithAny(this, *searchStrs)

/**
 * 대소문자를 무시하고 CharSequence가 지정한 접두사로 시작하는지 확인합니다.
 *
 * references are considered to be equal. The comparison is case insensitive.
 *
 * ```
 * StringUtils.startsWithIgnoreCase(null, "abc")     = false
 * StringUtils.startsWithIgnoreCase("abcdef", null)  = false
 * StringUtils.startsWithIgnoreCase("abcdef", "abc") = true
 * StringUtils.startsWithIgnoreCase("ABCDEF", "abc") = true
 * ```
 *
 * @see String#startsWith(String)
 * @receiver 검사할 CharSequence입니다. `null`일 수 있습니다.
 * @param prefix 찾을 접두사입니다. `null`일 수 있습니다.
 * @return 대소문자를 무시하고 CharSequence가 접두사로 시작하거나 둘 다 `null`이면 {@code true\}입니다.
 */
fun CharSequence.startsWithIgnoreCase(prefix: CharSequence?): Boolean =
    Strings.CI.startsWith(this, prefix)

/**
 * 문자열의 시작과 끝에서 지정한 문자 집합을 제거합니다.
 * This is similar to {@link String#trim()} but allows the characters
 * to be stripped to be controlled.
 *
 * 빈 문자열("") 입력은 빈 문자열을 반환합니다.
 *
 * If the stripChars String is `null`, whitespace is
 * stripped as defined by `Character#isWhitespace(char)`.
 * 또는 `String.strip(String)`을 사용합니다.
 *
 * ```
 * StringUtils.strip("", *)            = ""
 * StringUtils.strip("abc", null)      = "abc"
 * StringUtils.strip("  abc", null)    = "abc"
 * StringUtils.strip("abc  ", null)    = "abc"
 * StringUtils.strip(" abc ", null)    = "abc"
 * StringUtils.strip("  abcyx", "xyz") = "  abc"
 * ```
 *
 * @receiver 문자를 제거할 문자열입니다. `null`일 수 있습니다.
 * @param stripStr 제거할 문자입니다. `null`은 공백으로 처리합니다.
 * @return 제거 처리된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.strip(stripStr: String? = null): String = StringUtils.strip(this, stripStr)

/**
 * 문자열 끝에서 지정한 문자 집합을 제거합니다.
 *
 * 빈 문자열("") 입력은 빈 문자열을 반환합니다.
 *
 * If the stripStr String is `null`, whitespace is
 * stripped as defined by `Character#isWhitespace(char)`.
 *
 * ```
 * StringUtils.stripEnd("", *)            = ""
 * StringUtils.stripEnd("abc", "")        = "abc"
 * StringUtils.stripEnd("abc", null)      = "abc"
 * StringUtils.stripEnd("  abc", null)    = "  abc"
 * StringUtils.stripEnd("abc  ", null)    = "abc"
 * StringUtils.stripEnd(" abc ", null)    = " abc"
 * StringUtils.stripEnd("  abcyx", "xyz") = "  abc"
 * StringUtils.stripEnd("120.00", ".0")   = "12"
 * ```
 *
 * @receiver 문자를 제거할 문자열입니다. `null`일 수 있습니다.
 * @param stripStr 제거할 문자 집합입니다. `null`은 공백으로 처리합니다.
 * @return 제거 처리된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.stripEnd(stripStr: String? = null): String = StringUtils.stripEnd(this, stripStr)

/**
 * 문자열 시작에서 지정한 문자 집합을 제거합니다.
 *
 * 빈 문자열("") 입력은 빈 문자열을 반환합니다.
 *
 * If the stripChars String is `null`, whitespace is
 * stripped as defined by `Character#isWhitespace(char)`.
 *
 * ```
 * StringUtils.stripStart("", *)            = ""
 * StringUtils.stripStart("abc", "")        = "abc"
 * StringUtils.stripStart("abc", null)      = "abc"
 * StringUtils.stripStart("  abc", null)    = "abc"
 * StringUtils.stripStart("abc  ", null)    = "abc  "
 * StringUtils.stripStart(" abc ", null)    = "abc "
 * StringUtils.stripStart("yxabc  ", "xyz") = "abc  "
 * ```
 *
 * @receiver 문자를 제거할 문자열입니다. `null`일 수 있습니다.
 * @param stripStr 제거할 문자입니다. `null`은 공백으로 처리합니다.
 * @return 제거 처리된 문자열입니다. 입력 문자열이 `null`이면 `null`입니다.
 */
fun String.stripStart(stripStr: String? = null): String = StringUtils.stripStart(this, stripStr)

/**
 * 두 문자열 사이에 중첩된 문자열을 가져옵니다.
 * 첫 번째 매치만 반환합니다.
 *
 * A `null` open/close returns `null` (no match).
 * An empty ("") open and close returns an empty string.</p>
 *
 * ```
 * StringUtils.substringBetween("wx[b]yz", "[", "]") = "b"
 * StringUtils.substringBetween(*, null, *)          = null
 * StringUtils.substringBetween(*, *, null)          = null
 * StringUtils.substringBetween("", "", "")          = ""
 * StringUtils.substringBetween("", "", "]")         = null
 * StringUtils.substringBetween("", "[", "]")        = null
 * StringUtils.substringBetween("yabcz", "", "")     = ""
 * StringUtils.substringBetween("yabcz", "y", "z")   = "abc"
 * StringUtils.substringBetween("yabczyabcz", "y", "z")   = "abc"
 * ```
 *
 * @receiver 부분 문자열을 포함하는 문자열입니다. `null`일 수 있습니다.
 * @param open 부분 문자열 앞의 문자열입니다. `null`일 수 있습니다.
 * @param close 부분 문자열 뒤의 문자열입니다. `null`일 수 있습니다.
 * @return 부분 문자열입니다. 매치가 없으면 `null`입니다.
 */
fun String.substringBetween(open: String?, close: String?): String =
    StringUtils.substringBetween(this, open, close)

/**
 * Searches a String for substrings delimited by a start and end tag,
 * returning all matching substrings in an array.
 *
 * A `null` open/close returns `null` (no match).
 * 빈 open/close 문자열("")은 `null`을 반환합니다(매치 없음).
 *
 * ```
 * StringUtils.substringsBetween("[a][b][c]", "[", "]") = ["a","b","c"]
 * StringUtils.substringsBetween(*, null, *)            = null
 * StringUtils.substringsBetween(*, *, null)            = null
 * StringUtils.substringsBetween("", "[", "]")          = []
 * ```
 *
 * @receiver 부분 문자열들을 포함하는 문자열입니다. `null`이면 `null`, 빈 값이면 빈 값을 반환합니다.
 * @param open 부분 문자열 시작을 식별하는 문자열입니다. 빈 값이면 `null`을 반환합니다.
 * @param close 부분 문자열 끝을 식별하는 문자열입니다. 빈 값이면 `null`을 반환합니다.
 * @return 부분 문자열의 String 배열입니다. 매치가 없으면 `null`입니다.
 */
fun String.substringsBetween(open: String?, close: String?): Array<String> =
    StringUtils.substringsBetween(this, open, close)

/**
 * 주어진 문자열에서 감싸는 문자열을 제거합니다.
 *
 * ```
 * StringUtils.unwrap("a", "a")           = "a"
 * StringUtils.unwrap("aa", "a")          = ""
 * StringUtils.unwrap("\'abc\'", "\'")    = "abc"
 * StringUtils.unwrap("\"abc\"", "\"")    = "abc"
 * StringUtils.unwrap("AABabcBAA", "AA")  = "BabcB"
 * StringUtils.unwrap("A", "#")           = "A"
 * StringUtils.unwrap("#A", "#")          = "#A"
 * StringUtils.unwrap("A#", "#")          = "A#"
 * ```
 *
 * @receiver 감싸는 토큰을 제거할 문자열입니다. `null`일 수 있습니다.
 * @param wrapToken 감싸는 토큰을 제거하는 데 사용할 문자열입니다.
 * @return 감싸는 토큰을 제거한 문자열입니다. `wrapToken`으로 올바르게 감싸져 있지 않으면 원래 문자열입니다.
 */
fun String.unwrap(wrapToken: String): String = StringUtils.unwrap(this, wrapToken)

/**
 * 문자열을 다른 문자열로 감쌉니다.
 *
 * ```
 * StringUtils.wrap("", *)           = ""
 * StringUtils.wrap("ab", null)      = "ab"
 * StringUtils.wrap("ab", "x")       = "xabx"
 * StringUtils.wrap("ab", "\"")      = "\"ab\""
 * StringUtils.wrap("\"ab\"", "\"")  = "\"\"ab\"\""
 * StringUtils.wrap("ab", "'")       = "'ab'"
 * StringUtils.wrap("'abcd'", "'")   = "''abcd''"
 * StringUtils.wrap("\"abcd\"", "'") = "'\"abcd\"'"
 * StringUtils.wrap("'abcd'", "\"")  = "\"'abcd'\""
 * ```
 *
 * @receiver 감쌀 문자열입니다. `null`일 수 있습니다.
 * @param wrapWith `str`를 감쌀 문자열입니다.
 * @return 감싸진 문자열입니다.
 */
fun String.wrap(wrapWith: String): String = StringUtils.wrap(this, wrapWith)
