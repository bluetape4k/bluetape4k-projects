@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import io.bluetape4k.codec.Base58
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import kotlin.contracts.ExperimentalContracts

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private typealias JChar = java.lang.Character

/**
 * 빈 문자열을 나타냅니다.
 */
const val EMPTY_STRING = ""

/**
 * 문자열 축약 시 사용되는 생략 부호 문자열입니다.
 */
const val TRIMMING = "..."

/**
 * null 값을 문자열로 표현할 때 사용합니다.
 */
const val NULL_STRING = "<null>"

/**
 * SQL에서 null 값을 문자열로 표현할 때 사용합니다.
 */
const val NULL_STRING_SQL = "null"

/**
 * 콤마(, ) 구분자 문자열입니다.
 */
const val COMMA = ","

/**
 * 탭(tab) 문자입니다.
 */
const val TAB = "\t"

/**
 * 문자열 축약 시 기본 최대 길이입니다.
 */
const val ELLIPSIS_LENGTH = 80

/**
 * 시스템의 라인 구분자 문자열입니다.
 */
@JvmField
val LINE_SEPARATOR: String = System.lineSeparator()

/**
 * 공백 문자 블록을 나타내는 정규식 패턴입니다.
 */
@JvmField
val WHITESPACE_BLOCK: Pattern = Pattern.compile("\\s+")

/**
 * UTF-8 문자셋을 나타냅니다.
 */
@JvmField
val UTF_8: Charset = Charsets.UTF_8

/**
 * 문자열이 null이거나 blank(공백, 탭 등)인 경우 true를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 blank(공백, 탭 등)일 때 true입니다.
 * - blank는 모든 공백 문자(제어문자 포함)를 의미합니다.
 * - 문자열이 비어있거나 공백만 있을 때도 true입니다.
 *
 * ```kotlin
 * "".isWhitespace() // true
 * "   ".isWhitespace() // true
 * "abc".isWhitespace() // false
 * ```
 */
inline fun CharSequence?.isWhitespace(): Boolean = isNullOrBlank()

/**
 * 문자열이 null이거나 blank(공백, 탭 등)이 아닌 경우 true를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 blank(공백, 탭 등)일 때 false입니다.
 * - 문자열에 하나라도 공백이 아닌 문자가 있으면 true입니다.
 * - 문자열이 비어있거나 공백만 있을 때 false입니다.
 *
 * ```kotlin
 * "abc".isNotWhitespace() // true
 * "   ".isNotWhitespace() // false
 * null.isNotWhitespace() // false
 * ```
 */
inline fun CharSequence?.isNotWhitespace(): Boolean = !isWhitespace()

/**
 * 문자열이 null이 아니고 empty(길이 0)가 아니면 true를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 empty("")일 때 false입니다.
 * - 공백만 있어도 길이가 0이 아니면 true입니다.
 * - 문자열의 길이만 검사합니다.
 *
 * ```kotlin
 * "abc".hasLength() // true
 * "".hasLength() // false
 * null.hasLength() // false
 * ```
 */
inline fun CharSequence?.hasLength(): Boolean = !isNullOrEmpty()

/**
 * 문자열이 null이거나 empty(길이 0)면 true를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 empty("")일 때 true입니다.
 * - 공백만 있는 문자열도 길이가 0이 아니면 false입니다.
 * - 문자열의 길이만 검사합니다.
 *
 * ```kotlin
 * "".noLength() // true
 * null.noLength() // true
 * "abc".noLength() // false
 * ```
 */
inline fun CharSequence?.noLength(): Boolean = isNullOrEmpty()

/**
 * 문자열이 null이 아니고, blank(공백, 탭 등)가 아니면서 길이가 0보다 크면 true를 반환합니다.
 *
 * ## 동작/계약
 * - null, empty, blank(공백, 탭 등)일 때 false입니다.
 * - 사람이 읽을 수 있는 문자가 하나라도 있으면 true입니다.
 * - 공백만 있으면 false입니다.
 *
 * ```kotlin
 * "abc".hasText() // true
 * "   ".hasText() // false
 * "".hasText() // false
 * ```
 */
inline fun CharSequence?.hasText(): Boolean = hasLength() && this.isNotWhitespace()

/**
 * 문자열이 null이거나 blank(공백, 탭 등)이면 true를 반환합니다.
 *
 * ## 동작/계약
 * - null, empty, blank(공백, 탭 등)일 때 true입니다.
 * - 사람이 읽을 수 있는 문자가 없으면 true입니다.
 * - 공백만 있으면 true입니다.
 *
 * ```kotlin
 * "".noText() // true
 * "   ".noText() // true
 * "abc".noText() // false
 * ```
 */
inline fun CharSequence?.noText(): Boolean = noLength() || isWhitespace()

/**
 * 문자열이 null이거나 empty(길이 0)이면 null을 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 empty("")일 때 null 반환
 * - 그 외에는 원본 문자열 반환
 * - 문자열의 길이만 검사합니다.
 *
 * ```kotlin
 * "".asNullIfEmpty() // null
 * "abc".asNullIfEmpty() // "abc"
 * null.asNullIfEmpty() // null
 * ```
 */
inline fun String?.asNullIfEmpty(): String? = if (isNullOrEmpty()) null else this

/**
 * 문자열이 null이거나 blank(공백, 탭 등)이면 null을 반환합니다.
 *
 * ## 동작/계약
 * - null, empty, blank(공백, 탭 등)일 때 null 반환
 * - 그 외에는 원본 문자열 반환
 * - 문자열의 공백 여부를 검사합니다.
 *
 * ```kotlin
 * "   ".asNullIfBlank() // null
 * "abc".asNullIfBlank() // "abc"
 * null.asNullIfBlank() // null
 * ```
 */
inline fun String?.asNullIfBlank(): String? = if (isNullOrBlank()) null else this

/**
 * 문자열을 UTF-8로 인코딩한 [ByteArray]로 변환합니다.
 *
 * ## 동작/계약
 * - 항상 새로운 ByteArray를 반환합니다.
 * - null이 아니어야 하며, 예외는 발생하지 않습니다.
 * - UTF-8 인코딩을 사용합니다.
 *
 * ```kotlin
 * "debop".toUtf8Bytes() // byteArrayOf(100, 101, 98, 111, 112)
 * ```
 */
fun String.toUtf8Bytes(): ByteArray = toByteArray(UTF_8)

/**
 * UTF-8로 인코딩된 [ByteArray]를 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - 항상 새로운 문자열을 반환합니다.
 * - ByteArray가 비어있으면 빈 문자열을 반환합니다.
 * - 예외는 발생하지 않습니다.
 *
 * ```kotlin
 * byteArrayOf(100, 101, 98, 111, 112).toUtf8String() // "debop"
 * ```
 */
fun ByteArray.toUtf8String(): String = toString(UTF_8)

/**
 * 문자열을 UTF-8 인코딩의 [ByteBuffer]로 변환합니다.
 *
 * ## 동작/계약
 * - 항상 새로운 ByteBuffer를 반환합니다.
 * - null이 아니어야 하며, 예외는 발생하지 않습니다.
 * - UTF-8 인코딩을 사용합니다.
 *
 * ```kotlin
 * "debop".toUtf8ByteBuffer() // ByteBuffer.wrap(byteArrayOf(100, 101, 98, 111, 112))
 * ```
 */
fun String.toUtf8ByteBuffer(): ByteBuffer = UTF_8.encode(this)

/**
 * UTF-8로 인코딩된 [ByteBuffer]를 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - 항상 새로운 문자열을 반환합니다.
 * - ByteBuffer가 비어있으면 빈 문자열을 반환합니다.
 * - 예외는 발생하지 않습니다.
 *
 * ```kotlin
 * ByteBuffer.wrap(byteArrayOf(100, 101, 98, 111, 112)).toUtf8String() // "debop"
 * ```
 */
fun ByteBuffer.toUtf8String(): String = UTF_8.decode(this).toString()

/**
 * 문자열이 null이거나 empty(길이 0)이면 [fallback] 결과를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 empty("")일 때 fallback 람다를 호출합니다.
 * - 그 외에는 원본 문자열을 반환합니다.
 * - 문자열의 길이만 검사합니다.
 *
 * ```kotlin
 * val name: String? = null
 * name.ifEmpty { "debop" } // "debop"
 * ```
 */
@Deprecated("use ifNullOrEmpty instead", replaceWith = ReplaceWith("ifNullOrEmpty"))
inline fun String?.ifEmpty(fallback: () -> String): String = if (isNullOrEmpty()) fallback() else this

/**
 * 문자열이 null이거나 empty(길이 0)이면 [fallback] 결과를 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 empty("")일 때 fallback 람다를 호출합니다.
 * - 그 외에는 원본 문자열을 반환합니다.
 * - 문자열의 길이만 검사합니다.
 *
 * ```kotlin
 * val name: String? = ""
 * name.ifNullOrEmpty { "debop" } // "debop"
 * ```
 */
inline fun String?.ifNullOrEmpty(fallback: () -> String): String = if (isNullOrEmpty()) fallback() else this

/**
 * 문자열이 null이거나 blank(공백, 탭 등)이면 [fallback] 결과를 반환합니다.
 *
 * ## 동작/계약
 * - null, empty, blank(공백, 탭 등)일 때 fallback 람다를 호출합니다.
 * - 그 외에는 원본 문자열을 반환합니다.
 * - 문자열의 공백 여부를 검사합니다.
 *
 * ```kotlin
 * val name: String? = "   "
 * name.ifNullOrBlank { "debop" } // "debop"
 * ```
 */
inline fun String?.ifNullOrBlank(fallback: () -> String): String = if (isNullOrBlank()) fallback() else this

/**
 * 문자열의 앞뒤에 있는 모든 공백 문자를 제거합니다.
 *
 * ## 동작/계약
 * - 문자열이 비어있으면 그대로 반환합니다.
 * - 앞뒤에 있는 모든 공백 문자(탭, 스페이스 등)를 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * " \t  debop  ".trimWhitespace() // "debop"
 * ```
 */
fun String.trimWhitespace(): String {
    if (isEmpty()) {
        return this
    }

    val sb = StringBuilder(this.trim())
    while (sb.isNotEmpty() && JChar.isWhitespace(sb[0])) {
        sb.deleteCharAt(0)
    }
    while (sb.isNotEmpty() && JChar.isWhitespace(sb.last())) {
        sb.deleteCharAt(sb.lastIndex)
    }
    return sb.toString()
}

/**
 * 문자열의 앞쪽에 있는 모든 공백 문자를 제거합니다.
 *
 * ## 동작/계약
 * - 문자열이 비어있으면 그대로 반환합니다.
 * - 앞에 있는 모든 공백 문자(탭, 스페이스 등)를 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * " \t  debop  ".trimStartWhitespace() // "debop  "
 * ```
 */
fun String.trimStartWhitespace(): String {
    if (isEmpty()) {
        return this
    }

    val sb = StringBuilder(this.trimStart())
    while (sb.isNotEmpty() && JChar.isWhitespace(sb[0])) {
        sb.deleteCharAt(0)
    }
    return sb.toString()
}

/**
 * 문자열의 뒤쪽에 있는 모든 공백 문자를 제거합니다.
 *
 * ## 동작/계약
 * - 문자열이 비어있으면 그대로 반환합니다.
 * - 뒤에 있는 모든 공백 문자(탭, 스페이스 등)를 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * " \t  debop  ".trimEndWhitespace() // " \t  debop"
 * ```
 */
fun String.trimEndWhitespace(): String {
    if (isEmpty()) return this.trimEnd()

    val sb = StringBuilder(this.trimEnd())
    while (sb.isNotEmpty() && JChar.isWhitespace(sb.last())) {
        sb.deleteCharAt(sb.lastIndex)
    }
    return sb.toString()
}

/**
 * 문자열 내 모든 위치의 공백 문자를 제거합니다.
 *
 * ## 동작/계약
 * - 문자열이 비어있으면 그대로 반환합니다.
 * - 문자열 내 모든 공백 문자(탭, 스페이스 등)를 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * " \t  de\tbop  ".trimAllWhitespace() // "debop"
 * ```
 */
fun String.trimAllWhitespace(): String {
    if (isEmpty()) return this.trim()

    return buildString(length) {
        this@trimAllWhitespace
            .filterNot { JChar.isWhitespace(it) }
            .forEach { append(it) }
    }
}

/**
 * 문자열을 SQL single quote로 감싸고 내부 quote는 이스케이프하여 반환합니다.
 *
 * ## 동작/계약
 * - null이면 "null" 문자열을 반환합니다.
 * - 빈 문자열이면 "''"를 반환합니다.
 * - 내부 single quote는 두 개로 이스케이프됩니다.
 *
 * ```kotlin
 * "debop".quoted() // "'debop'"
 * "debop's".quoted() // "'debop''s'"
 * null.quoted() // "null"
 * ```
 */
inline fun String?.quoted(): String {
    if (this == null) {
        return NULL_STRING_SQL
    }

    return if (isEmpty()) {
        "''"
    } else {
        "'" + replace("\'", "\'\'") + "'"
    }
}

/**
 * 지정한 길이의 랜덤 알파뉴메릭 문자열을 생성합니다.
 *
 * ## 동작/계약
 * - size가 0 이상이어야 합니다.
 * - 항상 새로운 문자열을 반환합니다.
 * - 예외 발생 시 기본값을 반환하지 않습니다.
 *
 * ```kotlin
 * randomString(10) // "a1b2c3d4e5"
 * ```
 */
inline fun randomString(size: Int = 10): String {
    size.requireZeroOrPositiveNumber("size")
    return Base58.randomString(size)
}

/**
 * 문자열의 길이가 [maxLength]보다 크면 축약이 필요함을 나타냅니다.
 *
 * ## 동작/계약
 * - null 또는 empty이면 false입니다.
 * - maxLength는 4 이상이어야 합니다.
 * - 길이가 maxLength 초과면 true입니다.
 *
 * ```kotlin
 * "debop".needEllipsis(3) // true
 * "debop".needEllipsis(5) // false
 * ```
 */
inline fun String?.needEllipsis(maxLength: Int = ELLIPSIS_LENGTH): Boolean {
    maxLength.requireGt(3, "maxLength")
    return !isNullOrEmpty() && length > maxLength
}

/**
 * 문자열이 [maxLength]보다 길면 끝을 [TRIMMING]으로 축약합니다.
 *
 * ## 동작/계약
 * - null이면 빈 문자열을 반환합니다.
 * - 축약이 필요없으면 원본을 반환합니다.
 * - maxLength보다 길면 끝에 ...을 붙여 자릅니다.
 *
 * ```kotlin
 * "debop.bae@gmail.com".ellipsisEnd(6) // "deb..."
 * ```
 */
fun String?.ellipsisEnd(maxLength: Int = ELLIPSIS_LENGTH): String =
    this
        ?.let { str ->
            if (str.needEllipsis(maxLength)) {
                str.substring(0, maxLength - TRIMMING.length) + TRIMMING
            } else {
                str
            }
        }.orEmpty()

/**
 * 문자열이 [maxLength]보다 길면 중간을 [TRIMMING]으로 축약합니다.
 *
 * ## 동작/계약
 * - null이면 빈 문자열을 반환합니다.
 * - 축약이 필요없으면 원본을 반환합니다.
 * - maxLength보다 길면 중간에 ...을 붙여 자릅니다.
 *
 * ```kotlin
 * "debop.bae@gmail.com".ellipsisMid(7) // "de...m"
 * ```
 */
fun String?.ellipsisMid(maxLength: Int = ELLIPSIS_LENGTH): String {
    if (this.isNullOrEmpty()) return EMPTY_STRING

    if (!needEllipsis(maxLength)) {
        return this
    }

    val length = maxLength / 2
    val sb = StringBuilder()
    sb.append(this.substring(0, length)).append(TRIMMING)

    val len = if (maxLength % 2 == 0) this.length - length else this.length - length - 1
    if (len >= 0) {
        sb.append(this.substring(len))
    }

    return sb.toString()
}

/**
 * 문자열이 [maxLength]보다 길면 앞을 [TRIMMING]으로 축약합니다.
 *
 * ## 동작/계약
 * - null이면 빈 문자열을 반환합니다.
 * - 축약이 필요없으면 원본을 반환합니다.
 * - maxLength보다 길면 앞에 ...을 붙여 자릅니다.
 *
 * ```kotlin
 * "debop.bae@gmail.com".ellipsisStart(6) // "...com"
 * ```
 */
fun String?.ellipsisStart(maxLength: Int = ELLIPSIS_LENGTH): String =
    this
        ?.let { str ->
            if (str.needEllipsis(maxLength)) {
                TRIMMING + str.substring(str.length - maxLength + TRIMMING.length)
            } else {
                str
            }
        }.orEmpty()

/**
 * 문자열에서 지정한 문자들을 모두 제거합니다.
 *
 * ## 동작/계약
 * - null 또는 empty면 빈 문자열을 반환합니다.
 * - chars에 지정된 모든 문자를 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "debop".deleteChars('d', 'o') // "ebp"
 * ```
 */
fun CharSequence?.deleteChars(vararg chars: Char): String =
    when {
        isNullOrEmpty() -> EMPTY_STRING
        chars.isEmpty() -> this.toString()
        else -> this.filterNot { chars.contains(it) }.toString()
    }

/**
 * 컬렉션의 각 요소를 문자열로 변환하여 리스트로 반환합니다.
 *
 * ## 동작/계약
 * - 각 요소가 null 또는 empty이면 defaultValue 사용
 * - 항상 새로운 리스트를 반환합니다.
 * - 변환 과정에서 예외가 발생하지 않습니다.
 *
 * ```kotlin
 * listOf(1, 2, 3).asStringList() // listOf("1", "2", "3")
 * ```
 */
@Deprecated("use mapAsString", replaceWith = ReplaceWith("mapAsString(defaultValue)"))
fun <T : Any> Iterable<T>.asStringList(defaultValue: String = EMPTY_STRING): List<String> = map { it.asString(defaultValue) }

/**
 * 컬렉션의 각 요소를 문자열로 변환하여 리스트로 반환합니다.
 *
 * ## 동작/계약
 * - 각 요소가 null 또는 empty이면 defaultValue 사용
 * - 항상 새로운 리스트를 반환합니다.
 * - 변환 과정에서 예외가 발생하지 않습니다.
 *
 * ```kotlin
 * listOf(1, 2, 3).mapAsString() // listOf("1", "2", "3")
 * ```
 */
fun <T : Any> Iterable<T>.mapAsString(defaultValue: String = EMPTY_STRING): List<String> = map { it.asString(defaultValue) }

/**
 * 시퀀스의 각 요소를 문자열로 변환하여 시퀀스로 반환합니다.
 *
 * ## 동작/계약
 * - 각 요소가 null 또는 empty이면 defaultValue 사용
 * - 항상 새로운 시퀀스를 반환합니다.
 * - 변환 과정에서 예외가 발생하지 않습니다.
 *
 * ```kotlin
 * sequenceOf(1, 2, 3).mapAsString().toList() // listOf("1", "2", "3")
 * ```
 */
fun <T : Any> Sequence<T>.mapAsString(defaultValue: String = EMPTY_STRING): Sequence<String> = map { it.asString(defaultValue) }

/**
 * 문자열을 [n]번 반복한 새로운 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - null이면 빈 문자열을 반환합니다.
 * - n이 0이면 빈 문자열을 반환합니다.
 * - 음수일 경우 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * "debop".replicate(3) // "debopdebopdebop"
 * ```
 */
inline fun CharSequence?.replicate(n: Int): String = this?.repeat(n).orEmpty()

/**
 * 문자열에서 주어진 단어가 몇 번 등장하는지 반환합니다.
 *
 * ## 동작/계약
 * - null이거나 word가 빈 문자열이면 0을 반환합니다.
 * - 중복 없이 단어의 등장 횟수를 셉니다.
 * - 예외는 발생하지 않습니다.
 *
 * ```kotlin
 * "hello world world".wordCount("world") // 2
 * ```
 */
fun CharSequence?.wordCount(word: String): Int {
    if (isNullOrEmpty() || word.isEmpty()) return 0

    var matched = 0
    var startIndex = 0

    while (true) {
        val index = indexOf(word, startIndex)
        if (index < 0) {
            break
        }
        matched++
        startIndex = index + word.length
    }

    return matched
}

/**
 * 문자열에서 첫 번째 줄(첫 개행 문자 전까지)을 반환합니다.
 *
 * ## 동작/계약
 * - null 또는 blank면 빈 문자열을 반환합니다.
 * - 기본 구분자를 사용할 때는 시스템 line separator 와 `\n` 형식을 모두 인식합니다.
 * - lineSeparator가 없으면 전체 문자열을 반환합니다.
 * - lineSeparator가 첫 글자부터 시작하면 빈 문자열을 반환합니다.
 * - 예외는 발생하지 않습니다.
 *
 * ```kotlin
 * "a\nb\nc".firstLine() // "a"
 * "\nbody".firstLine() // ""
 * ```
 */
fun CharSequence?.firstLine(lineSeparator: String = LINE_SEPARATOR): String {
    if (this.isNullOrBlank()) return EMPTY_STRING
    if (lineSeparator.isEmpty()) return this.toString()

    val index =
        if (lineSeparator == LINE_SEPARATOR) {
            val systemLineSeparatorIndex = indexOf(lineSeparator)
            val newlineIndex = indexOf('\n')

            when {
                systemLineSeparatorIndex < 0 -> newlineIndex
                newlineIndex < 0 -> systemLineSeparatorIndex
                else -> minOf(systemLineSeparatorIndex, newlineIndex)
            }
        } else {
            indexOf(lineSeparator)
        }

    return if (index >= 0) substring(0, index) else this.toString()
}

/**
 * 문자열에서 [start]와 [end] 사이의 문자열을 추출합니다(두 경계는 제외).
 *
 * ## 동작/계약
 * - null, empty, start==end, start 또는 end가 없으면 빈 문자열 반환
 * - 첫 번째 start 이후부터 end 이전까지 추출합니다.
 * - 예외는 발생하지 않습니다.
 *
 * ```kotlin
 * "abc[hello]def".between("[", "]") // "hello"
 * "abc[hello".between("[", "]") // ""
 * ```
 */
fun CharSequence?.between(
    start: String,
    end: String,
): String {
    if (this.isNullOrEmpty() || start.isEmpty() || end.isEmpty() || start == end) return EMPTY_STRING

    val startIndex = this.indexOf(start)
    if (startIndex < 0) return EMPTY_STRING

    val contentStartIndex = startIndex + start.length
    val endIndex = this.indexOf(end, contentStartIndex)
    if (endIndex < 0) return EMPTY_STRING

    return this.substring(contentStartIndex, endIndex)
}

/**
 * 문자열에서 앞에서 [count]만큼 문자를 제거한 나머지 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - count가 0이면 원본을 반환합니다.
 * - count가 길이 이상이면 빈 문자열 반환
 * - 음수면 예외 발생
 *
 * ```kotlin
 * "debop".dropFirst(3) // "op"
 * ```
 */
inline fun String.dropFirst(count: Int = 1): String {
    count.requireZeroOrPositiveNumber("count")

    return if (count < length) this.substring(count) else EMPTY_STRING
}

/**
 * 문자열에서 뒤에서 [count]만큼 문자를 제거한 나머지 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - count가 0이면 원본을 반환합니다.
 * - count가 길이 이상이면 빈 문자열 반환
 * - 음수면 예외 발생
 *
 * ```kotlin
 * "debop".dropLast(3) // "de"
 * ```
 */
inline fun String.dropLast(count: Int = 1): String {
    count.requireZeroOrPositiveNumber("count")

    return if (count < length) this.substring(0, this.length - count) else EMPTY_STRING
}

/**
 * 문자열에서 앞에서 [count]만큼 문자를 가져옵니다.
 *
 * ## 동작/계약
 * - count가 길이보다 크면 전체 문자열 반환
 * - count가 0이면 빈 문자열 반환
 * - 음수면 예외 발생
 *
 * ```kotlin
 * "debop".takeFirst(3) // "deb"
 * ```
 */
fun String.takeFirst(count: Int = 1): String {
    count.requireZeroOrPositiveNumber("count")

    return if (count < length) this.substring(0, count) else this
}

/**
 * 문자열에서 뒤에서 [count]만큼 문자를 가져옵니다.
 *
 * ## 동작/계약
 * - count가 길이보다 크면 전체 문자열 반환
 * - count가 0이면 빈 문자열 반환
 * - 음수면 예외 발생
 *
 * ```kotlin
 * "debop".takeLast(3) // "bop"
 * ```
 */
fun String.takeLast(count: Int = 1): String {
    count.requireZeroOrPositiveNumber("count")

    return if (count < length) this.substring(this.length - count) else this
}

/**
 * 문자열이 지정한 접두사로 시작하지 않으면 접두사를 추가합니다.
 *
 * ## 동작/계약
 * - 이미 접두사가 있으면 원본을 반환합니다.
 * - ignoreCase가 true면 대소문자 구분 없이 검사합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "debop".prefixIfAbsent("Mr.") // "Mr.debop"
 * ```
 */
fun String.prefixIfAbsent(
    prefix: String,
    ignoreCase: Boolean = false,
): String = if (this.startsWith(prefix, ignoreCase)) this else prefix + this

/**
 * 문자열이 지정한 접미사로 끝나지 않으면 접미사를 추가합니다.
 *
 * ## 동작/계약
 * - 이미 접미사가 있으면 원본을 반환합니다.
 * - ignoreCase가 true면 대소문자 구분 없이 검사합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "/path".suffixIfAbsent("/") // "/path/"
 * ```
 */
fun String.suffixIfAbsent(
    suffix: String,
    ignoreCase: Boolean = false,
): String = if (this.endsWith(suffix, ignoreCase)) this else this + suffix

/**
 * 문자열에서 중복되지 않는 문자만 남긴 새로운 문자열을 반환합니다.
 *
 * ## 동작/계약
 * - 공백 문자는 포함하지 않습니다.
 * - 순서를 보장하며, 중복 문자는 제거합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "abcabc".uniqueChars() // "abc"
 * ```
 */
fun CharSequence.uniqueChars(): String =
    buildString {
        this@uniqueChars.forEach { char ->
            if (char != ' ' && !contains(char)) {
                append(char)
            }
        }
    }

/**
 * 문자열을 [size] 크기의 윈도우로 이동하며 부분 문자열 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - size는 1 이상이어야 합니다.
 * - 각 윈도우는 겹치며, 순차적으로 반환됩니다.
 * - 새로운 시퀀스를 반환합니다.
 *
 * ```kotlin
 * "debop".sliding(3).toList() // ["deb", "ebo", "bop"]
 * ```
 */
fun CharSequence.sliding(size: Int): Sequence<CharSequence> {
    size.assertPositiveNumber("size")
    val self = this@sliding
    return sequence {
        var start = 0
        var end = size

        while (end <= self.length) {
            yield(self.subSequence(start, end))
            start++
            end++
        }
    }
}

/**
 * 문자열을 [size] 크기의 윈도우로 이동하며 부분 문자열 시퀀스를 반환합니다.
 *
 * ## 동작/계약
 * - size는 1 이상이어야 합니다.
 * - 각 윈도우는 겹치며, 순차적으로 반환됩니다.
 * - 새로운 시퀀스를 반환합니다.
 *
 * ```kotlin
 * "abcde".sliding(2).toList() // ["ab", "bc", "cd", "de"]
 * ```
 */
fun String.sliding(size: Int): Sequence<String> {
    size.assertPositiveNumber("size")
    val self = this@sliding
    return sequence {
        var start = 0
        var end = size

        while (end <= self.length) {
            yield(self.substring(start, end))
            start++
            end++
        }
    }
}

/**
 * 문자열을 지정한 문자로 모두 마스킹하여 반환합니다(비밀번호 등).
 *
 * ## 동작/계약
 * - 빈 문자열이면 빈 문자열을 반환합니다.
 * - 각 문자를 maskChar로 대체합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "secret".mask() // "******"
 * ```
 */
@Deprecated("use mask instead", replaceWith = ReplaceWith("mask(maskChar)"))
fun String.redact(maskChar: Char = '*'): String = mask(maskChar)

/**
 * 문자열을 지정한 문자로 모두 마스킹하여 반환합니다(비밀번호 등).
 *
 * ## 동작/계약
 * - 빈 문자열이면 빈 문자열을 반환합니다.
 * - 각 문자를 maskChar로 대체합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "password".mask('#') // "########"
 * ```
 */
fun String.mask(maskChar: Char = '*'): String =
    if (isEmpty()) {
        EMPTY_STRING
    } else {
        maskChar.toString().repeat(this@mask.length)
    }

/**
 * delimiter(기본은 '-')로 구분된 문자열을 camel case로 변환합니다.
 *
 * ## 동작/계약
 * - 구분자가 없으면 첫 글자만 소문자로 변환합니다.
 * - 각 구분자 이후 문자를 대문자로 변환합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "group-id".toCamelcase() // "groupId"
 * ```
 */
fun String.toCamelcase(delimiter: String = "-"): String {
    if (delimiter.isWhitespace() || !contains(delimiter)) {
        return replaceFirstChar { it.lowercase(Locale.getDefault()) }
    }

    return try {
        val elements = this.split(delimiter)
        if (elements.isNotEmpty()) {
            val head = elements.first().lowercase(Locale.getDefault())
            val tail =
                elements
                    .drop(1)
                    .joinToString(separator = "") {
                        it.replaceFirstChar { ch: Char ->
                            if (ch.isLowerCase()) {
                                ch.titlecaseChar().toString() // ch.titlecase(Locale.getDefault())
                            } else {
                                ch.toString()
                            }
                        }
                    }
            head + tail
        } else {
            this
        }
    } catch (e: Exception) {
        this
    }
}

/**
 * camel case 문자열을 delimiter(기본 '-')로 구분된 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - 각 대문자 앞에 delimiter를 추가합니다.
 * - 첫 글자는 항상 소문자로 변환합니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "serverHostName".toDashedString() // "server-host-name"
 * ```
 */
fun String.toDashedString(delimiter: String = "-"): String =
    buildString {
        this@toDashedString.forEachIndexed { index, char ->
            when {
                index == 0 -> append(char.lowercaseChar())
                char.isUpperCase() -> append(delimiter).append(char.lowercaseChar())
                else -> append(char)
            }
        }
    }

/**
 * 최소 [minLength] 길이가 되도록 문자열 앞을 [padChar]로 채워 반환합니다.
 *
 * ## 동작/계약
 * - 이미 minLength 이상이면 원본을 반환합니다.
 * - 부족한 만큼 padChar로 앞을 채웁니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "7".padStart(3, '0') // "007"
 * ```
 */
fun String.padStart(
    minLength: Int,
    padChar: Char,
): String {
    if (length >= minLength) return this

    return buildString(minLength) {
        repeat(minLength - this@padStart.length) {
            append(padChar)
        }
        append(this@padStart)
    }
}

/**
 * 최소 [minLength] 길이가 되도록 문자열 뒤를 [padChar]로 채워 반환합니다.
 *
 * ## 동작/계약
 * - 이미 minLength 이상이면 원본을 반환합니다.
 * - 부족한 만큼 padChar로 뒤를 채웁니다.
 * - 새로운 문자열을 반환합니다.
 *
 * ```kotlin
 * "4.".padEnd(5, '0') // "4.000"
 * ```
 */
fun String.padEnd(
    minLength: Int,
    padChar: Char,
): String {
    if (length >= minLength) return this

    return buildString {
        append(this@padEnd)

        repeat(minLength - this@padEnd.length) {
            append(padChar)
        }
    }
}

/**
 * 두 문자열의 공통 접두사를 반환합니다.
 *
 * ## 동작/계약
 * - 두 문자열이 모두 비어있으면 빈 문자열 반환
 * - 일치하는 앞부분만 반환합니다.
 * - surrogate pair가 중간에 걸치면 제외합니다.
 *
 * ```kotlin
 * commonPrefix("abc", "abd") // "ab"
 * ```
 */
@JvmName("commonPrefixString")
fun commonPrefix(
    a: CharSequence,
    b: CharSequence,
): String {
    if (a.isEmpty() || b.isEmpty()) return EMPTY_STRING
    if (a == b) return a.toString()

    val maxPrefixLength = minOf(a.length, b.length)
    var p = 0
    while (p < maxPrefixLength && a[p] == b[p]) {
        p++
    }
    if (a.validSurrogatePairAt(p - 1) || b.validSurrogatePairAt(p - 1)) {
        p--
    }
    return a.substring(0, p)
}

/**
 * 이 문자열과 [other]의 공통 접두사를 반환합니다.
 *
 * ## 동작/계약
 * - 둘 다 비어있으면 빈 문자열 반환
 * - 일치하는 앞부분만 반환합니다.
 * - surrogate pair가 중간에 걸치면 제외합니다.
 *
 * ```kotlin
 * "abc".commonPrefix("abd") // "ab"
 * ```
 */
@JvmName("commonPrefixStringExtension")
inline fun CharSequence.commonPrefix(other: CharSequence): String = commonPrefix(this, other)

/**
 * 두 문자열의 공통 접미사를 반환합니다.
 *
 * ## 동작/계약
 * - 두 문자열이 모두 비어있으면 빈 문자열 반환
 * - 일치하는 뒷부분만 반환합니다.
 * - surrogate pair가 중간에 걸치면 제외합니다.
 *
 * ```kotlin
 * commonSuffix("abc", "xbc") // "bc"
 * ```
 */
@JvmName("commonSuffixString")
fun commonSuffix(
    a: CharSequence,
    b: CharSequence,
): String {
    if (a.isEmpty() || b.isEmpty()) return EMPTY_STRING
    if (a == b) return a.toString()

    val maxSuffixLength = minOf(a.length, b.length)
    var s = 0
    while (s < maxSuffixLength && a[a.length - s - 1] == b[b.length - s - 1]) {
        s++
    }
    if (a.validSurrogatePairAt(a.length - s - 1) || b.validSurrogatePairAt(b.length - s - 1)) {
        s--
    }
    return a.substring(a.length - s, a.length)
}

/**
 * 이 문자열과 [other]의 공통 접미사를 반환합니다.
 *
 * ## 동작/계약
 * - 둘 다 비어있으면 빈 문자열 반환
 * - 일치하는 뒷부분만 반환합니다.
 * - surrogate pair가 중간에 걸치면 제외합니다.
 *
 * ```kotlin
 * "abc".commonSuffix("xbc") // "bc"
 * ```
 */
@JvmName("commonSuffixStringExtension")
inline fun CharSequence.commonSuffix(other: CharSequence): String = commonSuffix(this, other)

internal fun CharSequence.validSurrogatePairAt(index: Int): Boolean =
    index >= 0 && index <= (length - 2) &&
        Character.isHighSurrogate(this[index]) &&
        Character.isLowSurrogate(this[index + 1])

/**
 * 두 문자열을 대소문자 구분 없이 비교합니다.
 *
 * ## 동작/계약
 * - 둘 다 null이면 true를 반환합니다.
 * - 둘 중 하나만 null이면 false입니다.
 * - 대소문자를 무시하고 비교합니다.
 *
 * ```kotlin
 * "abc".equalsIgnoreCase("ABC") // true
 * ```
 */
inline fun String?.equalsIgnoreCase(other: String?): Boolean = equals(other, ignoreCase = true)
