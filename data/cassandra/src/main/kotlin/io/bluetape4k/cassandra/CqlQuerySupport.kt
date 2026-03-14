package io.bluetape4k.cassandra

import com.datastax.oss.driver.internal.core.util.Strings

/**
 * 값을 CQL single quote 문자열로 이스케이프/감싸기 처리합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.quote(this?.toString())`를 호출합니다.
 * - `null` 입력은 빈 문자열을 quote한 결과(드라이버 규칙)로 처리됩니다.
 * - 문자열 내부 `'` 문자는 `''`로 이스케이프됩니다.
 *
 * ```kotlin
 * val s = "Simpson's".quote()
 * // s == "'Simpson''s'"
 * ```
 */
fun Any?.quote(): String = Strings.quote(this?.toString())

/**
 * 값을 CQL double quote 문자열로 이스케이프/감싸기 처리합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.doubleQuote(this?.toString())`를 호출합니다.
 * - 문자열 내부 `"` 문자는 `""`로 이스케이프됩니다.
 * - 수신 값을 변경하지 않고 새 문자열을 반환합니다.
 *
 * ```kotlin
 * val s = "a\"b".doubleQuote()
 * // s == "\"a\"\"b\""
 * ```
 */
fun Any?.doubleQuote(): String = Strings.doubleQuote(this?.toString())

/**
 * single quote로 감싼 CQL 문자열을 원문으로 복원합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.unquote(this)`를 호출합니다.
 * - 양끝 quote 제거와 `''` 언이스케이프를 수행합니다.
 * - 비인용 문자열 입력 시 동작은 드라이버 유틸 규칙을 따릅니다.
 *
 * ```kotlin
 * val raw = "'Simpson''s'".unquote()
 * // raw == "Simpson's"
 * ```
 */
fun String.unquote(): String = Strings.unquote(this)

/**
 * double quote로 감싼 CQL 문자열을 원문으로 복원합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.unDoubleQuote(this)`를 호출합니다.
 * - 양끝 quote 제거와 `""` 언이스케이프를 수행합니다.
 * - 입력 문자열 자체는 변경되지 않습니다.
 *
 * ```kotlin
 * val raw = "\"a\"\"b\"".unDoubleQuote()
 * // raw == "a\"b"
 * ```
 */
fun String.unDoubleQuote(): String = Strings.unDoubleQuote(this)

/**
 * 문자열이 single quote로 감싸져 있는지 검사합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.isQuoted(this)`를 호출합니다.
 * - `null` 입력은 `false`를 반환합니다.
 * - 문자열 내용 변경 없이 불리언만 반환합니다.
 *
 * ```kotlin
 * val quoted = "'abc'".isQuoted()
 * // quoted == true
 * ```
 */
fun String?.isQuoted(): Boolean = Strings.isQuoted(this)

/**
 * 문자열이 double quote로 감싸져 있는지 검사합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Strings.isDoubleQuoted(this)`를 호출합니다.
 * - `null` 입력은 `false`를 반환합니다.
 * - 검사만 수행하며 추가 할당이 없습니다.
 *
 * ```kotlin
 * val quoted = "\"abc\"".isDoubleQuoted()
 * // quoted == true
 * ```
 */
fun String?.isDoubleQuoted(): Boolean = Strings.isDoubleQuoted(this)

/**
 * 문자열이 double quote로 감싸져야 하는지 검사합니다.
 *
 * ## 동작/계약
 * - 빈 문자열은 항상 `false`를 반환합니다.
 * - 비어있지 않은 경우 `Strings.needsDoubleQuotes(this)` 결과를 반환합니다.
 * - CQL 식별자 규칙 기반의 판단만 수행합니다.
 *
 * ```kotlin
 * val a = "abc".needsDoubleQuotes()
 * val b = "a b".needsDoubleQuotes()
 * // a == false
 * // b == true
 * ```
 */
fun String.needsDoubleQuotes(): Boolean = isNotEmpty() && Strings.needsDoubleQuotes(this)
