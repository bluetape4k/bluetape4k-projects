@file:OptIn(ExperimentalContracts::class)

package io.bluetape4k.support

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Pattern
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


private typealias JChar = Character

const val EMPTY_STRING = ""
const val TRIMMING = "..."
const val NULL_STRING = "<null>"
const val COMMA = ","
const val TAB = "\t"
private const val ELLIPSIS_LENGTH = 80

@JvmField
val LINE_SEPARATOR: String = System.lineSeparator()

@JvmField
val WHITESPACE_BLOCK: Pattern = Pattern.compile("\\s+")

@JvmField
val UTF_8: Charset = Charsets.UTF_8

/**
 * 문자열이 null이거나 blank (`\t`, `\b` 등의 ASCII 제어문자 포함) 인지 확인합니다. (empty 와 다르다)
 */
fun CharSequence?.isWhitespace(): Boolean = isNullOrBlank()

/**
 * 문자열이 null이거나 blank가 아닌지 확인합니다. (empty 와 다르다)
 */
fun CharSequence?.isNotWhitespace(): Boolean = !isWhitespace()

/**
 * 문자열이 null이거나 empty 인지 확인합니다.
 */
fun CharSequence?.hasLength(): Boolean {
    contract {
        returns(true) implies (this@hasLength != null)
    }
    return !isNullOrEmpty()
}

/**
 * 문자열이 null이거나 blank가 아니고, 길이가 0보다 큰지 확인합니다. (사람이 읽을 수 있는 문자열이 있다면 true, 없다면 false)
 */
fun CharSequence?.hasText(): Boolean = hasLength() && !this.indices.any { this[it].isWhitespace() }

/**
 * 문자열이 null이거나 blank가 아니고, 길이가 0보다 큰지 확인합니다. (사람이 읽을 수 있는 문자열이 없다면 true, 있다면 false)
 */
fun CharSequence?.noText(): Boolean = isNullOrEmpty() || this.indices.all { this[it].isWhitespace() }

/**
 * 문자열이 null 이거나 empty 라면 null 을 반환한다
 */
fun String?.asNullIfEmpty(): String? = if (isNullOrEmpty()) null else this

/**
 * 문자열을 UTF-8 인코딩의 [ByteArray]로 변환합니다.
 *
 * ```
 * "debop".toUtf8Bytes() // return byteArrayOf(100, 101, 98, 111, 112)
 * ```
 *
 * @receiver String  인코딩할 문자열
 * @return ByteArray UTF-8 인코딩된 바이트 배열
 */
fun String.toUtf8Bytes(): ByteArray = toByteArray(UTF_8)

/**
 * [ByteArray]를 UTF-8 인코딩의 문자열로 반환한다
 *
 * ```
 * byteArrayOf(100, 101, 98, 111, 112).toUtf8String() // return "debop"
 * ```
 *
 * @receiver ByteArray UTF-8 인코딩된 바이트 배열
 * @return String 디코딩된 문자열
 */
fun ByteArray.toUtf8String(): String = toString(UTF_8)

/**
 * 문자열을 UTF-8 인코딩의 [ByteBuffer]로 변환합니다.
 *
 * ```
 * "debop".toUtf8ByteBuffer() // return ByteBuffer.wrap(byteArrayOf(100, 101, 98, 111, 112))
 * ```
 *
 * @receiver String 인코딩할 문자열
 * @return ByteBuffer UTF-8 인코딩된 ByteBuffer
 */
fun String.toUtf8ByteBuffer(): ByteBuffer = UTF_8.encode(this)

/**
 * [ByteBuffer]를 UTF-8 인코딩의 문자열로 반환한다
 *
 * ```
 * ByteBuffer.wrap(byteArrayOf(100, 101, 98, 111, 112)).toUtf8String() // return "debop"
 * ```
 *
 * @receiver ByteBuffer UTF-8 인코딩된 ByteBuffer
 * @return String 디코딩된 문자열
 */
fun ByteBuffer.toUtf8String(): String = UTF_8.decode(this).toString()

/**
 * 문자열이 null이거나 empty라면 [fallback]을 수행합니다.
 *
 * ```
 * val name: String? = null
 * val result = name.ifEmpty { "debop" } // return "debop"
 * ```
 *
 * @receiver String? 문자열
 * @param fallback 문자열이 null이거나 empty인 경우 수행할 람다
 * @return String 문자열
 */
inline fun String?.ifEmpty(fallback: () -> String): String = when {
    isNullOrEmpty() -> fallback()
    else -> this
}

/**
 * 문자열이 null이거나 empty라면 [fallback]을 수행합니다.
 *
 * ```
 * val name: String? = null
 * val result = name.ifEmpty { "debop" } // return "debop"
 * ```
 *
 * @receiver String? 문자열
 * @param fallback 문자열이 null이거나 empty인 경우 수행할 람다
 * @return String 문자열
 */
inline fun String?.ifNullOrEmpty(fallback: () -> String): String = when {
    isNullOrEmpty() -> fallback()
    else -> this
}

/**
 * 문자열이 null이거나 blank라면 [fallback]을 수행합니다.
 *
 * ```
 * val name: String? = "\t"
 * val result = name.ifBlank { "debop" } // return "debop"
 * ```
 *
 * @receiver String? 문자열
 * @param fallback 문자열이 null이거나 blank인 경우 수행할 람다
 * @return String 문자열
 */
inline fun String?.ifNullOrBlank(fallback: () -> String): String = when {
    isNullOrBlank() -> fallback()
    else -> this
}


/**
 * 문자열 앞 뒤의 Whitespace를 제거합니다.
 *
 * ```
 * " \t  debop  ".trimWhitespace() // return "debop"
 * ```
 *
 * @receiver String 문자열
 * @return String Whitespace가 제거된 문자열
 * @see String.trim
 */
fun String.trimWhitespace(): String {
    if (isEmpty())
        return this.trim()

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
 * 문자열 앞의 Whitespace를 제거합니다.
 *
 * ```
 * " \t  debop  ".trimStartWhitespace() // return "debop  "
 * ```
 *
 * @receiver String 문자열
 * @return String 앞쪽의 Whitespace가 제거된 문자열
 * @see String.trimStart
 */
fun String.trimStartWhitespace(): String {
    if (isEmpty())
        return this.trimStart()

    val sb = StringBuilder(this.trimStart())
    while (sb.isNotEmpty() && JChar.isWhitespace(sb[0])) {
        sb.deleteCharAt(0)
    }
    return sb.toString()
}

/**
 * 문자열 뒷쪽의 Whitespace를 제거합니다.
 *
 * ```
 * " \t  debop  ".trimEndWhitespace() // return " \t  debop"
 * ```
 *
 * @receiver String 문자열
 * @return String 뒷쪽의 Whitespace가 제거된 문자열
 * @see String.trimEnd
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
 * 문자열의 모든 곳의 Whitespace를 제거합니다.
 *
 * ```
 * " \t  de\tbop  ".trimAllWhitespace() // return "debop"
 * ```
 *
 * @receiver String 문자열
 * @return String 모든 Whitespace가 제거된 문자열
 * @see String.trim
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
 * SQL 구문 중에 문자열인 경우에는 single quotation을 지정할 수 있도록 합니다.
 *
 * ```
 * "debop".quoted() // return "'debop'"
 * "debop's".quoted() // return "'debop''s'"
 * null.quoted() // return "null"
 * "".quoted() // return "''"
 * ```
 *
 * @receiver String? 문자열
 * @return null 인 경우 "null" 문자열을 반환하고, 문자열인 경우 single quotation을 추가합니다.
 *
 */
fun String?.quoted(): String {
    if (this == null)
        return "null"

    return if (isEmpty()) "''"
    else "'" + replace("\'", "\'\'") + "'"
}

/**
 * 램덤한 Alpha numeric 문자열을 만듭니다.
 *
 * ```
 * randomString(10) // return "a1b2c3d4e5"
 * ```
 *
 * @param size 생성할 문자열의 길이
 * @return 랜덤한 Alpha numeric 문자열
 */
@JvmOverloads
fun randomString(size: Int = 10): String {
    size.assertZeroOrPositiveNumber("size")
    return RandomStringUtils.secureStrong().nextAlphanumeric(size)
}

/**
 * 문자열의 길이가 [maxLength]다 크다면, 축약이 필요하다 (true)
 *
 * ```
 * "debop".needEllipsis(3) // return true
 * "debop".needEllipsis(5) // return false
 * ```
 *
 * @receiver String? 축약 대상 문자열
 * @param maxLength 최대 길이
 * @return 문자열의 길이가 [maxLength]보다 크다면 true, 아니면 false
 */
fun String?.needEllipsis(maxLength: Int = ELLIPSIS_LENGTH): Boolean {
    return !isNullOrBlank() && length > maxLength
}

/**
 * 문자열의 길이가 [maxLength]보다 크다면, [TRIMMING]를 문자열 끝에 추가하여 [maxLength] 길이로 축약합니다.
 *
 * ```
 * "debop.bae@gmail.com".ellipsisEnd(6) // return "deb..."
 * "debop.bae@gmail.com".ellipsisEnd(20) // return "debop.bae@gmail.com"
 * ```
 *
 * @receiver String? 축약 대상 문자열
 * @param maxLength 최대 길이
 * @return 축약된 문자열
 */
fun String?.ellipsisEnd(maxLength: Int = ELLIPSIS_LENGTH): String {
    return this?.let { self ->
        when {
            self.needEllipsis(maxLength) -> self.substring(0, maxLength - TRIMMING.length) + TRIMMING
            else -> self
        }
    } ?: EMPTY_STRING
}

/**
 * 문자열의 길이가 [maxLength]보다 크다면, [TRIMMING]를 문자열 중간에 추가하여 [maxLength] 길이로 축약합니다.
 *
 * ```
 * "debop.bae@gmail.com".ellipsisEnd(6) // return "de....b"
 * "debop.bae@gmail.com".ellipsisEnd(20) // return "debop.bae@gmail.com"
 * ```
 *
 * @receiver String? 축약 대상 문자열
 * @param maxLength 최대 길이
 * @return 축약된 문자열
 */
fun String?.ellipsisMid(maxLength: Int = ELLIPSIS_LENGTH): String {
    if (this == null || this.isEmpty()) return EMPTY_STRING

    if (!needEllipsis(maxLength))
        return this

    val length = maxLength / 2
    val sb = StringBuilder()
    sb.append(this.substring(0, length)).append(TRIMMING)

    val len = if (maxLength % 2 == 0) this.length - length
    else this.length - length - 1

    sb.append(this.substring(len))
    return sb.toString()
}

/**
 * 문자열의 길이가 [maxLength]보다 크다면, [TRIMMING]를 문자열 처음에 추가하여 [maxLength] 길이로 축약합니다.
 *
 * ```
 * "debop.bae@gmail.com".ellipsisEnd(6) // return "...op."
 * "debop.bae@gmail.com".ellipsisEnd(20) // return "debop.bae@gmail.com"
 * ```
 *
 * @receiver String? 축약 대상 문자열
 * @param maxLength 최대 길이
 * @return 축약된 문자열
 */
fun String?.ellipsisStart(maxLength: Int = ELLIPSIS_LENGTH): String {
    return this?.let { self ->
        when {
            self.needEllipsis(maxLength) -> TRIMMING + self.substring(self.length - maxLength + TRIMMING.length)
            else -> self
        }
    } ?: EMPTY_STRING
}

/**
 * 문자열에서 [chars] 문자들을 제거합니다.
 *
 * ```
 * "debop".deleteChars('d', 'o') // return "ebp"
 * "debop".deleteChars('d', 'o', 'p') // return "eb"
 * ```
 *
 * @receiver String? 문자열
 * @param chars 제거할 문자들
 * @return 제거된 문자열
 */
fun CharSequence?.deleteChars(vararg chars: Char): String {
    if (isNullOrEmpty()) {
        return EMPTY_STRING
    }
    if (chars.isEmpty()) {
        return this.toString()
    }
    return this.filterNot { chars.contains(it) }.toString()
}

/**
 * 컬렉션의 요소를 문자열로 변환하여, 문자열 컬렉션으로 반환합니다.
 *
 * ```
 * listOf(1, 2, 3).asStringList() // return listOf("1", "2", "3")
 * ```
 *
 * @receiver Iterable<T> 문자열로 변환할 요소들
 * @param defaultValue 요소가 null이거나 empty인 경우 사용할 기본 문자열 (기본값: "")
 */
@JvmOverloads
fun <T: Any> Iterable<T>.asStringList(defaultValue: String = EMPTY_STRING): List<String> =
    map { it.asString(defaultValue) }


/**
 * 문자열을 [n]번 반복한 문자열을 반환합니다.
 *
 * ```
 * "debop".replicate(3) // return "debopdebopdebop"
 * ```
 *
 * @receiver String? 문자열
 * @param n 반복 횟수
 */
fun CharSequence?.replicate(n: Int): String =
    this?.repeat(n) ?: EMPTY_STRING

/**
 * 문자열에서 [word] 단어가 포함된 횟수를 반환합니다.
 *
 * ```
 * "debop is developer and architecture".wordCount("developer") // return 1
 * ```
 *
 * @receiver CharSequence? 문자열
 * @param word 단어
 * @return 단어의 포함된 횟수
 */
fun CharSequence?.wordCount(word: String): Int =
    StringUtils.countMatches(this, word)

/**
 * 문자열에서 첫번째 라인 (첫번째 개행문자 전까지)을 반환합니다.
 *
 * ```
 * "debop\nis developer\nand architecture".firstLine() // return "debop"
 * ```
 *
 * @receiver CharSequence? 문자열
 * @param lineSeparator 라인 구분자 (기본값: [LINE_SEPARATOR])
 * @return 첫번째 라인
 */
fun CharSequence?.firstLine(lineSeparator: String = LINE_SEPARATOR): String {
    if (this.isNullOrBlank())
        return EMPTY_STRING

    val index = this.indexOf(lineSeparator)
    return if (index > 0) substring(0, index) else this.toString()
}

/**
 * 문자열에서 [start] 문자열과 [end]문자열 사이의 문자열을 추출합니다. (start와 end는 제외됩니다)
 *
 * ```
 * val origin = "debop is developer and architecture"
 *
 * origin.between("developer", "architecture") shouldBeEqualTo " and "
 * origin.between("debop", "developer") shouldBeEqualTo " is "
 *
 * origin.between("eb", "p is") shouldBeEqualTo "o"
 * ```
 *
 * @param start 시작 문자열
 * @param end 끝 문자열
 * @return 시작문자열과 끝 문자열 사이의 문자열
 */
fun CharSequence?.between(start: String, end: String): String {
    if (this.isNullOrBlank())
        return this?.toString() ?: EMPTY_STRING

    if (areEquals(start, end))
        return EMPTY_STRING

    var startIndex = 0
    if (start.isNotEmpty()) {
        val index = this.indexOf(start)
        if (index >= 0)
            startIndex = index + start.length
    }

    var endIndex = this.length
    if (end.isNotEmpty()) {
        val index = this.indexOf(end, startIndex)
        if (index >= 0)
            endIndex = index
    }

    return if (endIndex >= startIndex) this.substring(startIndex, endIndex) else EMPTY_STRING
}

/**
 * 문자열에서 [count] 숫자만큼 앞에서부터 문자열을 제거하고, 나머지 문자열을 반환합니다.
 * [count] 가 문자열 길이보다 크다면 빈 문자열을 반환합니다.
 *
 * ```
 * "debop".dropFirst(3) // return "op"
 * "debop".dropFirst(10) // return ""
 * ```
 *
 * @receiver String 문자열
 * @param count 제거할 문자열의 길이
 * @return 제거된 문자열
 */
fun String.dropFirst(count: Int = 1): String =
    if (count < length) this.substring(count)
    else EMPTY_STRING


/**
 * 문자열에서 [count] 숫자만큼 뒤에서부터 문자열을 제거하고, 나머지 문자열을 반환합니다.
 * [count] 가 문자열 길이보다 크다면 빈 문자열을 반환합니다.
 *
 * ```
 * "debop".dropLast(3) // return "de"
 * "debop".dropLast(10) // return ""
 * ```
 *
 * @receiver String 문자열
 * @param count 제거할 문자열의 길이
 */
fun String.dropLast(count: Int = 1): String =
    if (count < length) this.substring(0, this.length - count)
    else EMPTY_STRING

/**
 * 문자열에서 [count] 숫자만큼 앞에서부터 문자열을 가져옵니다.
 * [count] 가 문자열 길이보다 크다면 전체 문자열을 반환합니다.
 *
 * ```
 * "debop".takeFirst(3) // return "deb"
 * "debop".takeFirst(10) // return "debop"
 * ```
 *
 * @receiver String 문자열
 * @param count 가져올 문자열의 길이
 * @return 가져온 문자열
 */
fun String.takeFirst(count: Int = 1): String =
    if (count < length) this.substring(0, count)
    else this

/**
 * 문자열에서 [count] 숫자만큼 뒤에서부터 문자열을 가져옵니다.
 * [count] 가 문자열 길이보다 크다면 전체 문자열을 반환합니다.
 *
 * ```
 * "debop".takeLast(3) // return "bop"
 * "debop".takeLast(10) // return "debop"
 * ```
 *
 * @receiver String 문자열
 * @param count 가져올 문자열의 길이
 * @return 가져온 문자열
 */
fun String.takeLast(count: Int = 1): String =
    if (count < length) this.substring(this.length - count)
    else this

/**
 * 지정한 접두사로 시작하지 않는다면 접두사를 추가합니다.
 *
 * ```
 * "debop".prefixIfAbsent("Mr.") // return "Mr.debop"
 * "Mr.debop".prefixIfAbsent("Mr.") // return "Mr.debop"
 * "mr.debop".prefixIfAbsent("Mr.", ignoreCase = true) // return "Mr.debop"
 * "Mr.debop".prefixIfAbsent("Mr.", ignoreCase = true) // return "Mr.debop"
 * ```
 *
 * @param prefix 접두사
 * @param ignoreCase 대소문자 구분 여부 (기본: false)
 * @return [prefix]가 접두사로 붙은 문자열
 */
@JvmOverloads
fun String.prefixIfAbsent(prefix: String, ignoreCase: Boolean = false): String =
    if (this.startsWith(prefix, ignoreCase)) this else prefix + this

/**
 * 지정한 접미사로 끝나지 않는다면 접미사를 추가합니다.
 *
 * ```
 * "/path/to/debop".suffixIfAbsent("/") // return "/path/to/debop/"
 * ```
 *
 * @param suffix 접미사
 * @param ignoreCase 대소문자 구분 여부 (기본: false)
 * @return [suffix]가 접미사로 붙은 문자열
 */
@JvmOverloads
fun String.suffixIfAbsent(suffix: String, ignoreCase: Boolean = false): String =
    if (this.endsWith(suffix, ignoreCase)) this else this + suffix

/**
 * 문자열의 문자들 중 유니크한 문자로만 필터링해서 문자열로 반환합니다. (distinct)
 *
 * ```
 * "abcde".uniqueChars() // return "abcde"
 * "abcdeabcde".uniqueChars() // return "abcde"
 * ```
 *
 * @receiver CharSequence 문자열
 * @return 유니크한 문자들로만 이루어진 문자열
 */
fun CharSequence.uniqueChars(): String = buildString {
    this@uniqueChars.forEach { char ->
        if (char != ' ' && !contains(char)) {
            append(char)
        }
    }
}

/**
 * 문자열을 [size] 크기의 window로 이동하면서 문자열을 반환합니다.
 *
 * ```
 * "debop".sliding(3).toList() // return ["deb", "ebo", "bop"]
 * "가나다라마".sliding(2).toList() // return ["가나", "나다", "다라", "라마"]
 * ```
 *
 * @receiver CharSequence 문자열
 * @param size window 크기
 * @return window 크기로 이동하면서 생성된 문자열
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
 * 문자열을 [size] 크기의 window로 이동하면서 문자열을 반환합니다.
 *
 * ```
 * "debop".sliding(3).toList() // return ["deb", "ebo", "bop"]
 * "가나다라마".sliding(2).toList() // return ["가나", "나다", "다라", "라마"]
 * ```
 *
 * @receiver String 문자열
 * @param size window 크기
 * @return window 크기로 이동하면서 생성된 문자열
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
 * 비밀번호 등 지정한 문자를 외부에 공개 안되도록 [mask] 문자로 변경합니다.
 *
 * ```
 * val password = "debop"
 * log.debug { "password=${password.redact()}" }    // "debop" --> "*****"
 * ```
 */
fun String.redact(mask: String = "*"): String = mask.repeat(length)

/**
 * 비밀번호 등 지정한 문자를 외부에 공개 안되도록 [mask] 문자로 변경합니다.
 *
 * ```
 * val password = "debop"
 * log.debug { "password=${password.redact()}" }    // "debop" --> "*****"
 * ```
 */
fun String.mask(mask: String = "*"): String = mask.repeat(length)


/**
 * [delimiter](기본=`-`)로 구분된 문자열을 camel case 문자열로 변환합니다.
 *
 * ```
 * "group-id".toCamelcase()  // return "groupId"
 * "server-host-name".toCamelcase()  // return "serverHostName"
 * "ServerName".toCamelcase()   // return "serverName"
 * ```
 *
 * @param delimiter 구분자
 * @return camel case 문자열
 */
fun String.toCamelcase(delimiter: String = "-"): String {
    if (delimiter.isWhitespace() || !contains(delimiter)) {
        return replaceFirstChar { it.lowercase(Locale.getDefault()) }
    }

    return try {
        val elements = this.split(delimiter)
        if (elements.isNotEmpty()) {
            val head = elements.first().lowercase(Locale.getDefault())
            val tail = elements
                .drop(1)
                .joinToString(separator = "") {
                    it.replaceFirstChar { ch: Char ->
                        if (ch.isLowerCase()) ch.titlecaseChar().toString() // ch.titlecase(Locale.getDefault())
                        else ch.toString()
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
 * camel case 문자열을 [delimiter] 로 구분되는 문자열로 변환합니다.
 *
 * ```
 * "groupId".toDashedString()           // return "group-id"
 * "serverHostName".toDashedString()    // return "server-host-name"
 * "ServerName".toDashedString()        // return "server-name"
 * ```
 *
 * @param delimiter 구분자 ("-")
 * @return 구분자로 구분된 문자열
 */
fun String.toDashedString(delimiter: String = "-"): String = buildString {
    this@toDashedString.forEachIndexed { index, char ->
        when {
            index == 0 -> append(char.lowercaseChar())
            char.isUpperCase() -> append(delimiter).append(char.lowercaseChar())
            else -> append(char)
        }
    }
}

/**
 * 최소 [minLength] 길이를 가지는 문자열을 반환합니다. 원본 문자열이 [minLength]보다 작다면 [padChar]로 앞에서부터 채웁니다.
 * 문자열이 이미 [minLength]보다 길면 원래 문자열을 반환합니다.
 *
 * ```
 * "7".padStart(3, '0')  // return "007"
 * "2010".padStart(3, '0')  // return "2010"
 * ```
 *
 * @receiver the string which should appear at the beginning of the result
 * @param minLength 결과 문자열이 가져야 하는 최소 길이입니다. 0 또는 음수일 수 있으며, 이 경우 입력 문자열이 항상 반환됩니다.
 * @param padChar 결과 문자열이 최소 길이에 도달할 때까지 결과 문자열 앞에 추가할 문자
 * @return 최소 길이의 문자열
 */
fun String.padStart(minLength: Int, padChar: Char): String {
    if (length >= minLength) return this

    val sb = StringBuilder(minLength)
    for (i in length until minLength) {
        sb.append(padChar)
    }
    sb.append(this)
    return sb.toString()
}

/**
 * 최소 [minLength] 길이를 가지는 문자열을 반환합니다. 원본 문자열이 [minLength]보다 작다면 [padChar]로 뒤에서부터 채웁니다.
 * 문자열이 이미 [minLength]보다 길면 원래 문자열을 반환합니다.
 *
 * ```
 * "4.".padEnd(5, '0')  // return "4.000"
 * "2010".padEnd(3, '!')  // return "2010"
 * ```
 *
 * @receiver the string which should appear at the beginning of the result
 * @param minLength 결과 문자열이 가져야 하는 최소 길이입니다. 0 또는 음수일 수 있으며, 이 경우 입력 문자열이 항상 반환됩니다.
 * @param padChar 결과 문자열이 최소 길이에 도달할 때까지 결과 문자열 끝에 추가할 문자
 * @return 최소 길이의 문자열
 */
fun String.padEnd(minLength: Int, padChar: Char): String {
    if (length >= minLength) return this

    val sb = StringBuilder(minLength)
    sb.append(this)
    for (i in length until minLength) {
        sb.append(padChar)
    }
    return sb.toString()
}

/**
 * 두 문자열의 공통된 prefix를 찾아서 반환합니다.
 *
 * ```
 * commonPrefix("debop", "debop") // return "debop"
 * commonPrefix("debop", "deb") // return "deb"
 * commonPrefix("debop", "bae") // return ""
 * ```
 *
 * @return 공통된 prefix 문자열
 */
fun commonPrefix(a: CharSequence, b: CharSequence): String {
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
 * 두 문자열의 공통된 suffix를 찾아서 반환합니다.
 *
 * ```
 * commonSuffix("debop", "debop") // return "debop"
 * commonSuffix("debop", "op") // return "op"
 * commonSuffix("debop", "bae") // return ""
 * ```
 *
 * @return 공통된 suffix 문자열
 */
fun commonSuffix(a: CharSequence, b: CharSequence): String {
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
 * 유효한 surrogate pair가 주어진 `index`에서 시작할 때 true를 반환합니다.
 */
internal fun CharSequence.validSurrogatePairAt(index: Int): Boolean {
    return index >= 0 && index <= (length - 2) &&
            Character.isHighSurrogate(this[index]) &&
            Character.isLowSurrogate(this[index + 1])
}
