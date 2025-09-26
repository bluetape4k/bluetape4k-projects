package io.bluetape4k.apache

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Strings


/**
 * Abbreviates a String using a given replacement marker. This will turn
 * "Now is the time for all good men" into "...is the time for..." if "..." was defined
 * as the replacement marker.
 *
 * Works like `String.abbr(String, int)`, but allows you to specify
 * a "left edge" offset.  Note that this left edge is not necessarily going to
 * be the leftmost character in the result, or the first character following the
 * replacement marker, but it will appear somewhere in the result.
 *
 * In no case will it return a String of length greater than {@code maxWidth}.
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
 * @receiver  the String to check, may be null
 * @param abbrMarker  the String used as replacement marker
 * @param offset  left edge of source String
 * @param maxWidth  maximum length of result String, must be at least 4
 * @return abbreviated String, `null` if null String input
 */
fun String.abbr(maxWidth: Int, abbrMarker: String = "...", offset: Int = 0): String =
    StringUtils.abbreviate(this, abbrMarker, offset, maxWidth)

/**
 * Abbreviates a String to the length passed, replacing the middle characters with the supplied
 * replacement String.
 *
 * This abbreviation only occurs if the following criteria is met:
 * - Neither the String for abbreviation nor the replacement String are null or empty
 * - The length to truncate to is less than the length of the supplied String
 * - The length to truncate to is greater than 0
 * - The abbreviated String will have enough room for the length supplied replacement String
 * and the first and last characters of the supplied String for abbreviation
 *
 * Otherwise, the returned String will be the same as the supplied String for abbreviation.
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
 * @receiver  the String to abbreviate, may be null
 * @param middle the String to replace the middle characters with, may be null
 * @param length the length to abbreviate {@code str} to.
 * @return the abbreviated String if the above criteria is met, or the original String supplied for abbreviation.
 */
fun String.abbrMiddle(length: Int, middle: String = "..."): String =
    StringUtils.abbreviateMiddle(this, middle, length)

/**
 * Appends the suffix to the end of the string if the string does not
 * already end with any of the suffixes.
 *
 * ```
 * StringUtils.appendIfMissing(null, null) = null
 * StringUtils.appendIfMissing("abc", null) = "abc"
 * StringUtils.appendIfMissing("", "xyz") = "xyz"
 * StringUtils.appendIfMissing("abc", "xyz") = "abcxyz"
 * StringUtils.appendIfMissing("abcxyz", "xyz") = "abcxyz"
 * StringUtils.appendIfMissing("abcXYZ", "xyz") = "abcXYZxyz"
 * ```
 * With additional suffixes,
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
 * @receiver The string.
 * @param suffix The suffix to append to the end of the string.
 * @param suffixes Additional suffixes that are valid terminators.
 *
 * @return A new String if suffix was appended, the same string otherwise.
 */
fun String.appendIfMissing(suffix: CharSequence, vararg suffixes: CharSequence): String =
    Strings.CS.appendIfMissing(this, suffix, *suffixes)

/**
 * Appends the suffix to the end of the string if the string does not
 * already end, case-insensitive, with any of the suffixes.
 *
 * ```
 * StringUtils.appendIfMissingIgnoreCase(null, null) = null
 * StringUtils.appendIfMissingIgnoreCase("abc", null) = "abc"
 * StringUtils.appendIfMissingIgnoreCase("", "xyz") = "xyz"
 * StringUtils.appendIfMissingIgnoreCase("abc", "xyz") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcxyz", "xyz") = "abcxyz"
 * StringUtils.appendIfMissingIgnoreCase("abcXYZ", "xyz") = "abcXYZ"
 * ```
 * With additional suffixes,
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
 * @receiver The string.
 * @param suffix The suffix to append to the end of the string.
 * @param suffixes Additional suffixes that are valid terminators.
 *
 * @return A new String if suffix was appended, the same string otherwise.
 */
fun String.appendIfMissingIgnoreCase(suffix: CharSequence, vararg suffixes: CharSequence): String =
    Strings.CI.appendIfMissing(this, suffix, *suffixes)

/**
 * Centers a String in a larger String of size {@code size}
 * using the space character (' ').
 *
 * If the size is less than the String length, the original String is returned.
 * A `null` String returns `null`.
 * A negative size is treated as zero.</p>
 *
 * Equivalent to `center(str, size, " ")`.
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
 * @receiver  the String to center, may be null
 * @param size  the int size of new String, negative treated as zero
 * @return centered String, `null` if null String input
 */
fun String.center(size: Int, padChar: Char = ' '): String =
    StringUtils.center(this, size, padChar)

/**
 * Centers a String in a larger String of size {@code size}.
 * Uses a supplied String as the value to pad the String with.
 *
 * If the size is less than the String length, the String is returned.
 * A `null` String returns `null`.
 * A negative size is treated as zero.
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
 * @receiver  the String to center, may be null
 * @param size  the int size of new String, negative treated as zero
 * @param padStr  the String to pad the new String with, must not be null or empty
 * @return centered String, `null` if null String input
 * @throws IllegalArgumentException if padStr is `null` or empty
 */
fun String.center(size: Int, padStr: String): String =
    StringUtils.center(this, size, padStr)

/**
 * Remove the last character from a String.
 *
 * If the String ends in {@code \r\n}, then remove both of them.
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
 * @receiver  the String to chop last character from, may be null
 * @return String without last character, `null` if null String input
 */
fun String.chop(): String =
    StringUtils.chop(this)


/**
 * Checks if the CharSequence contains any character in the given
 * set of characters.
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
 * @receiver  the CharSequence to check, may be null
 * @param searchChars  the chars to search for, may be null
 * @return the {@code true} if any of the chars are found, {@code false} if no match or null input
 */
fun CharSequence.containsAny(vararg searchChars: Char): Boolean =
    StringUtils.containsAny(this, *searchChars)

/**
 * Checks if the CharSequence contains any of the CharSequences in the given array.
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
 * @receiver The CharSequence to check, may be null
 * @param searchCharSequences The array of CharSequences to search for, may be null. Individual CharSequences may be
 *        null as well.
 * @return `true` if any of the search CharSequences are found, `false` otherwise
 */
fun CharSequence.containsAny(vararg searchCharSequences: CharSequence): Boolean =
    Strings.CS.containsAny(this, *searchCharSequences)


/**
 * Checks if the CharSequence contains any of the CharSequences in the given array, ignoring case.
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
 * @receiver The CharSequence to check, may be null
 * @param searchCharSequences The array of CharSequences to search for, may be null. Individual CharSequences may be
 *        null as well.
 * @return `true` if any of the search CharSequences are found, `false` otherwise
 */
fun CharSequence.containsAnyIgnoreCase(vararg searchCharSequences: CharSequence): Boolean =
    Strings.CI.containsAny(this, *searchCharSequences)

/**
 * Checks if CharSequence contains a search CharSequence irrespective of case,
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
 * @receiver the CharSequence to check, may be null
 * @param searchStr  the CharSequence to find, may be null
 * @return true if the CharSequence contains the search CharSequence irrespective of case or false if not or `null` string input
 */
fun CharSequence.containsIgnoreCase(searchStr: CharSequence): Boolean =
    Strings.CI.contains(this, searchStr)

/**
 * Checks that the CharSequence does not contain certain characters.
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
 * @receiver  the CharSequence to check, may be null
 * @param searchChars  an array of invalid chars, may be null
 * @return true if it contains none of the invalid chars, or is null
 */
fun CharSequence.containsNone(vararg searchChars: Char): Boolean =
    StringUtils.containsNone(this, *searchChars)

/**
 * Checks that the CharSequence does not contain certain characters.
 *
 * A `null` CharSequence will return {@code true}.
 * A `null` invalid character array will return `true`.
 * An empty String ("") always returns true.
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
 * @receiver  the CharSequence to check
 * @param invalidStr  a String of invalid chars, may be null
 * @return true if it contains none of the invalid chars, or is null
 * @since 2.0
 * @since 3.0 Changed signature from containsNone(String, String) to containsNone(CharSequence, String)
 */
fun CharSequence.containsNone(invalidStr: String?): Boolean =
    StringUtils.containsNone(this, invalidStr)


/**
 * Checks if the CharSequence contains only certain characters.
 *
 * A `null` CharSequence will return {@code false}.
 * A `null` valid character array will return `false`.
 * An empty CharSequence (length()=0) always returns `true`.
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
 * @param validChars  an array of valid chars
 * @return true if it only contains valid chars and is non-null
 */
fun CharSequence.containsOnly(vararg validChars: Char): Boolean =
    StringUtils.containsOnly(this, *validChars)

/**
 * Checks if the CharSequence contains only certain characters.
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
 * @receiver  the CharSequence to check, may be null
 * @param validStr  a String of valid chars, may be null
 * @return true if it only contains valid chars and is non-null
 * @since 2.0
 * @since 3.0 Changed signature from containsOnly(String, String) to containsOnly(CharSequence, String)
 */
fun CharSequence.containsOnly(validStr: String?): Boolean =
    StringUtils.containsOnly(this, validStr)

/**
 * Check whether the given CharSequence contains any whitespace characters.
 *
 * Whitespace is defined by [Character#isWhitespace(char)].
 *
 * @receiver the CharSequence to check (may be `null`)
 * @return `true` if the CharSequence is not empty and
 * contains at least 1 (breaking) whitespace character
 */
fun CharSequence.containsWhitespace(): Boolean =
    StringUtils.containsWhitespace(this)

/**
 * Counts how many times the char appears in the given string.
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
 * @receiver  the CharSequence to check, may be null
 * @param ch  the char to count
 * @return the number of occurrences, 0 if the CharSequence is `null`
 */
fun CharSequence.countMatches(ch: Char): Int =
    StringUtils.countMatches(this, ch)

/**
 * Counts how many times the substring appears in the larger string.
 * Note that the code only counts non-overlapping matches.
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
 * @receiver the CharSequence to check, may be null
 * @param sub  the substring to count, may be null
 * @return the number of occurrences, 0 if either CharSequence is `null`
 */
fun CharSequence.countMatches(sub: CharSequence): Int =
    StringUtils.countMatches(this, sub)


fun <T: CharSequence> T.defaultIfWhitespace(defaultValue: T): T = this.ifBlank { defaultValue }

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
 * @receiver  the String to delete whitespace from, may be null
 * @return the String without whitespaces, `null` if null String input
 */
fun String.deleteWhitespace(): String = StringUtils.deleteWhitespace(this)

/**
 * Compares two Strings, and returns the portion where they differ.
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
 * @receiver  the first String
 * @param other  the second String, may be null
 * @return the portion of str2 where it differs from str1; returns the empty String if they are equal
 */
fun String.deference(other: String): String = StringUtils.difference(this, other)


fun String.endsWithAny(vararg searchStrings: String): Boolean = Strings.CS.endsWithAny(this, *searchStrings)

fun String.endsWithIgnoreCase(suffix: CharSequence): Boolean = Strings.CI.endsWith(this, suffix)

/**
 * Checks if a String contains Unicode digits,
 * if yes then concatenate all the digits in String and return it as a String.
 *
 * An empty ("") String will be returned if no digits found in {@code str}.
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
 * @receiver the String to extract digits from
 * @return String with only digits,
 *           or an empty ("") String if no digits found,
 *           or `null` String if {@code str} is null
 */
fun String.getDigits(): String = StringUtils.getDigits(this)


/**
 * Find the first index of any of a set of potential substrings.
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
 * @receiver  the CharSequence to check, may be null
 * @param searchStrs  the CharSequences to search for, may be null
 * @return the first index of any of the searchStrs in str, -1 if no match
 */
fun CharSequence.indexOfAny(vararg searchStrs: CharSequence): Int = StringUtils.indexOfAny(this, *searchStrs)

/**
 * Search a CharSequence to find the first index of any
 * character in the given set of characters.
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
 * @receiver  the CharSequence to check, may be null
 * @param searchStr  the chars to search for, may be null
 * @return the index of any of the chars, -1 if no match or null input
 */
fun CharSequence.indexOfAny(searchStr: String?): Int = StringUtils.indexOfAny(this, searchStr)


/**
 * Compares all CharSequences in an array and returns the index at which the
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
 * @param css  array of CharSequences, entries may be null
 * @return the index where the strings begin to differ; -1 if they are all equal
 */
fun indexOfDifference(vararg css: CharSequence): Int = StringUtils.indexOfDifference(*css)


/**
 * Compares two CharSequences, and returns the index at which the
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
 * @receiver  the first CharSequence
 * @param other  the second CharSequence, may be null
 * @return the index where cs1 and cs2 begin to differ; -1 if they are equal
 */
fun CharSequence.indexOfDifference(other: CharSequence?): Int = StringUtils.indexOfDifference(this, other)

fun CharSequence.indexOfIgnoreCase(searchStr: CharSequence, startPos: Int = 0): Int =
    Strings.CI.indexOf(this, searchStr, startPos)

/**
 * Checks if the CharSequence contains only Unicode letters.
 *
 * An empty CharSequence (length()=0) will return `false`.
 *
 * ```
 * StringUtils.isAlpha("")     = false
 * StringUtils.isAlpha("  ")   = false
 * StringUtils.isAlpha("abc")  = true
 * StringUtils.isAlpha("ab2c") = false
 * StringUtils.isAlpha("ab-c") = false
 * ```
 *
 * @receiver  the CharSequence to check, may be null
 * @return `true` if only contains letters, and is non-null
 */
fun CharSequence.isAlpha(): Boolean = StringUtils.isAlpha(this)

/**
 * Checks if the CharSequence contains only Unicode letters or digits.
 *
 * An empty CharSequence (length()=0) will return `false`.
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
 * @receiver  the CharSequence to check
 * @return {@code true} if only contains letters or digits, and is non-null
 */
fun CharSequence.isAlphaNumeric(): Boolean = StringUtils.isAlphanumeric(this)

/**
 * Checks if the CharSequence contains only Unicode letters, digits
 * or space ({@code ' '}).
 *
 * An empty CharSequence (length()=0) will return `true`.
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
 * @receiver  the CharSequence to check
 * @return `true` if only contains letters, digits or space, and is non-null
 */
fun CharSequence.isAlphaNumericSpace(): Boolean = StringUtils.isAlphanumericSpace(this)

/**
 * Checks if the CharSequence contains only Unicode letters or space
 * ({@code ' '}).
 *
 * An empty CharSequence (length()=0) will return `true`.
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
 * @receiver  the CharSequence to check
 * @return `true` if only contains letters or space, and is non-null
 */
fun CharSequence.isAlphaSpace(): Boolean = StringUtils.isAlphaSpace(this)


/**
 * Checks if the CharSequence contains only ASCII printable characters.
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
 * @receiver the CharSequence to check
 * @return `true` if every character is in the range 32 through 126
 */
fun CharSequence.isAsciiPrintable(): Boolean = StringUtils.isAsciiPrintable(this)

/**
 * Checks if the CharSequence contains mixed casing of both uppercase and lowercase characters.
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
 * @receiver the CharSequence to check
 * @return `true` if the CharSequence contains both uppercase and lowercase characters
 */
fun CharSequence.isMixedCase(): Boolean = StringUtils.isMixedCase(this)

/**
 * Checks if the CharSequence contains only Unicode digits.
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
 * @receiver  the CharSequence to check
 * @return `true` if only contains digits, and is non-null
 */
fun CharSequence.isNumeric(): Boolean = StringUtils.isNumeric(this)

/**
 * Checks if the CharSequence contains only Unicode digits or space (`' '`).
 * A decimal point is not a Unicode digit and returns false.
 *
 * An empty CharSequence (length()=0) will return `true`.
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
 * @receiver  the CharSequence to check
 * @return `true` if only contains digits or space, and is non-null
 */
fun CharSequence.isNumericSpace(): Boolean = StringUtils.isNumericSpace(this)

/**
 * Checks if the CharSequence contains only whitespace.
 *
 * Whitespace is defined by {@link Character#isWhitespace(char)}.
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
 * @receiver  the CharSequence to check
 * @return `true` if only contains whitespace, and is non-null
 */
fun CharSequence.isWhiteSpace(): Boolean = StringUtils.isWhitespace(this)

/**
 * Find the latest index of any substring in a set of potential substrings.
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
 * @receiver  the CharSequence to check
 * @param searchStrs  the CharSequences to search for, may be null
 * @return the last index of any of the CharSequences, -1 if no match
 */
fun CharSequence.lastIndexOfAny(vararg searchStrs: CharSequence): Int =
    StringUtils.lastIndexOfAny(this, *searchStrs)

/**
 * Case in-sensitive find of the last index within a CharSequence
 * from the specified position.
 *
 * A negative start position returns {@code -1}.
 * An empty ("") search CharSequence always matches unless the start position is negative.
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
 * @receiver  the CharSequence to check
 * @param searchStr  the CharSequence to find, may be null
 * @param startPos  the start position
 * @return the last index of the search CharSequence (always <= startPos), -1 if no match
 */
fun CharSequence.lastIndexOfIgnoreCase(searchStr: CharSequence?, startPos: Int = length): Int =
    Strings.CI.lastIndexOf(this, searchStr, startPos)

/**
 * Left pad a String with a specified character.
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
 * @receiver  the String to pad out
 * @param size  the size to pad to
 * @param padChar  the character to pad with
 * @return left padded String or original String if no padding is necessary,
 *  `null` if null String input
 * @since 2.0
 */
fun String.leftPad(size: Int, padChar: Char = ' '): String =
    StringUtils.leftPad(this, size, padChar)

/**
 * Left pad a String with a specified String.
 *
 * Pad to a size of `size`.
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
 * @receiver the String to pad out
 * @param size  the size to pad to
 * @param padStr  the String to pad with, null or empty treated as single space
 * @return left padded String or original String if no padding is necessary,
 *  `null` if null String input
 */
fun String.leftPad(size: Int, padStr: String): String =
    StringUtils.leftPad(this, size, padStr)


/**
 * Removes all occurrences of a substring from within the source string.
 *
 * An empty ("") source string will return the empty string.
 * A `null` remove string will return the source string.
 * An empty ("") remove string will return the source string.
 *
 * ```
 * StringUtils.remove("", *)          = ""
 * StringUtils.remove(*, null)        = *
 * StringUtils.remove(*, "")          = *
 * StringUtils.remove("queued", "ue") = "qd"
 * StringUtils.remove("queued", "zz") = "queued"
 * ```
 *
 * @receiver  the source String to search
 * @param remove  the String to search for and remove, may be null
 * @return the substring with the string removed if found, `null` if null String input
 */
fun String.remove(remove: String?): String = Strings.CS.remove(this, remove)

/**
 * Removes a substring only if it is at the end of a source string,
 * otherwise returns the source string.
 *
 * An empty ("") source string will return the empty string.
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
 * @receiver  the source String to search, may be null
 * @param remove  the String to search for and remove, may be null
 * @return the substring with the string removed if found, `null` if null String input
 */
fun String.removeEnd(remove: String): String = Strings.CS.removeEnd(this, remove)

/**
 * Case insensitive removal of a substring if it is at the end of a source string,
 * otherwise returns the source string.
 *
 * An empty ("") source string will return the empty string.
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
 * @receiver  the source String to search, may be null
 * @param remove  the String to search for (case-insensitive) and remove, may be null
 * @return the substring with the string removed if found, `null` if null String input
 */
fun String.removeEndIgnoreCase(remove: String): String = Strings.CI.removeEnd(this, remove)

/**
 * Removes a substring only if it is at the beginning of a source string,
 * otherwise returns the source string.
 *
 * An empty ("") source string will return the empty string.
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
 * @receiver  the source String to search
 * @param remove  the String to search for and remove, may be null
 * @return the substring with the string removed if found, `null` if null String input
 */
fun String.removeStart(remove: String): String = Strings.CS.removeStart(this, remove)

/**
 * Case insensitive removal of a substring if it is at the beginning of a source string,
 * otherwise returns the source string.
 *
 * An empty ("") source string will return the empty string.
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
 * @receiver  the source String to search, may be null
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
 * Case insensitively replaces a String with another String inside a larger String,
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
 * @receiver  text to search and replace in
 * @param searchStr  the String to search for (case-insensitive), may be null
 * @param replacement  the String to replace it with, may be null
 * @param max  maximum number of values to replace, or {@code -1} if no maximum
 * @return the text with any replacements processed, `null` if null String input
 */
fun String.replaceIgnoreCase(searchStr: String?, replacement: String?, max: Int = -1): String =
    Strings.CI.replace(this, searchStr, replacement, max)

/**
 * Replaces a String with another String inside a larger String, once.
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
 * @receiver text to search and replace in
 * @param searchString  the String to search for, may be null
 * @param replacement  the String to replace with, may be null
 * @return the text with any replacements processed, `null` if null String input
 */
fun String.replaceOnce(searchString: String?, replacement: String?): String {
    return Strings.CS.replace(this, searchString, replacement, 1)
}

/**
 * Case insensitively replaces a String with another String inside a larger String, once.
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
 * @receiver  text to search and replace in
 * @param searchString  the String to search for (case-insensitive), may be null
 * @param replacement  the String to replace with, may be null
 * @return the text with any replacements processed, `null` if null String input
 */
fun String.replaceOnceIgnoreCase(searchString: String?, replacement: String?): String {
    return Strings.CI.replace(this, searchString, replacement, 1)
}

/**
 * Gets the rightmost {@code len} characters of a String.
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
 * @receiver  the String to get the rightmost characters from, may be null
 * @param len  the length of the required String
 * @return the rightmost characters, `null` if null String input
 */
fun String.right(len: Int): String = StringUtils.right(this, len)

/**
 * Right pad a String with a specified character.
 *
 * The String is padded to the size of `size`.
 *
 * ```
 * StringUtils.rightPad("", 3, 'z')     = "zzz"
 * StringUtils.rightPad("bat", 3, 'z')  = "bat"
 * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
 * StringUtils.rightPad("bat", 1, 'z')  = "bat"
 * StringUtils.rightPad("bat", -1, 'z') = "bat"
 * ```
 *
 * @receiver  the String to pad out
 * @param size  the size to pad to
 * @param padChar  the character to pad with
 * @return right padded String or original String if no padding is necessary, `null` if null String input
 */
fun String.rightPad(size: Int, padChar: Char = ' '): String = StringUtils.rightPad(this, size, padChar)

/**
 * Right pad a String with a specified String.
 *
 * The String is padded to the size of `size`.
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
 * @receiver the String to pad out
 * @param size  the size to pad to
 * @param padStr  the String to pad with, null or empty treated as single space
 * @return right padded String or original String if no padding is necessary, `null` if null String input
 */
fun String.rightPad(size: Int, padStr: String): String = StringUtils.rightPad(this, size, padStr)

/**
 * Check if a CharSequence starts with any of the provided case-sensitive prefixes.
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
 * @receiver the CharSequence to check
 * @param searchStrs the case-sensitive CharSequence prefixes, may be empty or `null`
 * @see StringUtils#startsWith(CharSequence, CharSequence)
 * @return {@code true} if the input {@code sequence} is `null` AND no {@code searchStrings} are provided, or
 *   the input {@code sequence} begins with any of the provided case-sensitive {@code searchStrings}.
 */
fun CharSequence.startsWithAny(vararg searchStrs: CharSequence?): Boolean =
    Strings.CS.startsWithAny(this, *searchStrs)

/**
 * Case insensitive check if a CharSequence starts with a specified prefix.
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
 * @receiver  the CharSequence to check, may be null
 * @param prefix the prefix to find, may be null
 * @return {@code true} if the CharSequence starts with the prefix, case-insensitive, or both `null`
 */
fun CharSequence.startsWithIgnoreCase(prefix: CharSequence?): Boolean =
    Strings.CI.startsWith(this, prefix)

/**
 * Strips any of a set of characters from the start and end of a String.
 * This is similar to {@link String#trim()} but allows the characters
 * to be stripped to be controlled.
 *
 * An empty string ("") input returns the empty string.
 *
 * If the stripChars String is `null`, whitespace is
 * stripped as defined by `Character#isWhitespace(char)`.
 * Alternatively use `String.strip(String)`.
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
 * @receiver the String to remove characters from, may be null
 * @param stripStr  the characters to remove, null treated as whitespace
 * @return the stripped String, `null` if null String input
 */
fun String.strip(stripStr: String? = null): String = StringUtils.strip(this, stripStr)

/**
 * Strips any of a set of characters from the end of a String.
 *
 * An empty string ("") input returns the empty string.
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
 * @receiver  the String to remove characters from, may be null
 * @param stripStr  the set of characters to remove, null treated as whitespace
 * @return the stripped String, `null` if null String input
 */
fun String.stripEnd(stripStr: String? = null): String = StringUtils.stripEnd(this, stripStr)

/**
 * Strips any of a set of characters from the start of a String.
 *
 * An empty string ("") input returns the empty string.
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
 * @receiver  the String to remove characters from, may be null
 * @param stripStr  the characters to remove, null treated as whitespace
 * @return the stripped String, `null` if null String input
 */
fun String.stripStart(stripStr: String? = null): String = StringUtils.stripStart(this, stripStr)

/**
 * Gets the String that is nested in between two Strings.
 * Only the first match is returned.
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
 * @receiver  the String containing the substring, may be null
 * @param open  the String before the substring, may be null
 * @param close  the String after the substring, may be null
 * @return the substring, `null` if no match
 */
fun String.substringBetween(open: String?, close: String?): String =
    StringUtils.substringBetween(this, open, close)

/**
 * Searches a String for substrings delimited by a start and end tag,
 * returning all matching substrings in an array.
 *
 * A `null` open/close returns `null` (no match).
 * An empty ("") open/close returns `null` (no match).
 *
 * ```
 * StringUtils.substringsBetween("[a][b][c]", "[", "]") = ["a","b","c"]
 * StringUtils.substringsBetween(*, null, *)            = null
 * StringUtils.substringsBetween(*, *, null)            = null
 * StringUtils.substringsBetween("", "[", "]")          = []
 * ```
 *
 * @receiver  the String containing the substrings, null returns null, empty returns empty
 * @param open  the String identifying the start of the substring, empty returns null
 * @param close  the String identifying the end of the substring, empty returns null
 * @return a String Array of substrings, or `null` if no match
 */
fun String.substringsBetween(open: String?, close: String?): Array<String> =
    StringUtils.substringsBetween(this, open, close)

/**
 * Unwraps a given string from another string.
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
 * @receiver the String to be unwrapped, can be null
 * @param wrapToken the String used to unwrap
 * @return unwrapped String or the original string if it is not quoted properly with the wrapToken
 */
fun String.unwrap(wrapToken: String): String = StringUtils.unwrap(this, wrapToken)

/**
 * Wraps a String with another String.
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
 * @receiver the String to be wrapper, may be null
 * @param wrapWith the String that will wrap str
 * @return wrapped String
 */
fun String.wrap(wrapWith: String): String = StringUtils.wrap(this, wrapWith)
