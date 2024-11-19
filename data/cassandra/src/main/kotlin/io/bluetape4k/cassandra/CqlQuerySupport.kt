package io.bluetape4k.cassandra

import com.datastax.oss.driver.internal.core.util.Strings

private const val DOUBLE_SINGLE_QUOTE = "\'\'"
private const val SINGLE_QUOTE = "\'"

/**
 * String 컬럼에 문자열을 저장할 때 single quotation (`'`)이 있다면 중복해주도록 합니다 (-> `''`)
 *
 * ```
 * Simpson's family -> 'Simpson''s family'
 * null -> ''  // NOTE: Cassandra 에는 null 이라는 게 없다. 그냥 빈문자열이다
 * ```
 */
fun Any?.quote(): String = Strings.quote(this?.toString())

/**
 * String 컬럼에 문자열을 저장할 때 double quotation (`"`)이 있다면 중복해주도록 합니다 (-> `""`)
 *
 * ```
 * <div class="content">Hello, World</div> -> <div class=""content"">Hello, World</div>
 * ```
 */
fun Any?.doubleQuote(): String = Strings.doubleQuote(this?.toString())

/**
 * String 컬럼에 저장된 문자열의 single quotation (`'`)을 제거합니다.
 *
 * ```
 * 'Simpson''s family' -> Simpson's family
 * ```
 */
fun String.unquote(): String = Strings.unquote(this)

/**
 * String 컬럼에 저장된 문자열의 double quotation (`"`)을 제거합니다.
 *
 * ```
 * <div class=""content"">Hello, World</div> -> <div class="content">Hello, World</div>
 * ```
 */
fun String.unDoubleQuote(): String = Strings.unDoubleQuote(this)

/**
 * 문자열이 single quotation (`'`)으로 감싸여 있는지 확인합니다.
 *
 * ```
 * 'Simpson''s family' -> true
 * ```
 */
fun String?.isQuoted(): Boolean = Strings.isQuoted(this)

/**
 * 문자열이 double quotation (`"`)으로 감싸여 있는지 확인합니다.
 *
 * ```
 * <div class=""content"">Hello, World</div> -> true
 * ```
 */
fun String?.isDoubleQuoted(): Boolean = Strings.isDoubleQuoted(this)

/**
 * 문자열이 double quotation (`"`)으로 감싸야 하는지 확인합니다.
 *
 * ```
 * <div class=""content"">Hello, World</div> -> false
 * <div class="content">Hello, World</div> -> true
 * ```
 */
fun String.needsDoubleQuotes(): Boolean = isNotEmpty() && Strings.needsDoubleQuotes(this)
