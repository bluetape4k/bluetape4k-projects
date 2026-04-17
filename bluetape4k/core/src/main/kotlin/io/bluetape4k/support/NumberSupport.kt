@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import io.bluetape4k.exceptions.NotSupportedException
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import kotlin.reflect.KClass


@JvmField
        /**
         * `Long` 최솟값을 [BigInteger]로 표현한 상수입니다.
         *
         * ## 동작/계약
         * - 오버플로우 검증 시 하한 비교 값으로 사용됩니다.
         * - null/blank 개념이 없는 순수 수치 상수입니다.
         * - 읽기 전용 필드이며 런타임 변경이 불가능합니다.
         * - 접근 비용은 상수 시간입니다.
         *
         * ```kotlin
         * // BigIntMin == Long.MIN_VALUE.toBigInt()
         * ```
         */
val BigIntMin = Long.MIN_VALUE.toBigInt()

@JvmField
        /**
         * `Long` 최댓값을 [BigInteger]로 표현한 상수입니다.
         *
         * ## 동작/계약
         * - 오버플로우 검증 시 상한 비교 값으로 사용됩니다.
         * - null/blank 개념이 없는 순수 수치 상수입니다.
         * - 읽기 전용 필드이며 런타임 변경이 불가능합니다.
         * - 접근 비용은 상수 시간입니다.
         *
         * ```kotlin
         * // BigIntMax == Long.MAX_VALUE.toBigInt()
         * ```
         */
val BigIntMax = Long.MAX_VALUE.toBigInt()

@JvmField
        /**
         * 기본 숫자 변환에서 지원하는 표준 Number 타입 집합입니다.
         *
         * ## 동작/계약
         * - 정수/부동소수/임의정밀도([BigDecimal], [BigInteger]) 타입을 포함합니다.
         * - 불변 Set으로 외부에서 변경할 수 없습니다.
         * - null 타입은 포함하지 않습니다.
         * - 조회 비용은 해시셋 기준 평균 `O(1)`입니다.
         *
         * ```kotlin
         * // Int::class in StandardNumberTypes
         * // BigDecimal::class in StandardNumberTypes
         * ```
         */
val StandardNumberTypes: Set<KClass<out Number>> = java.util.Collections.unmodifiableSet(
    hashSetOf(
        Byte::class,
        Short::class,
        Int::class,
        Long::class,
        Float::class,
        Double::class,
        BigDecimal::class,
        BigInteger::class,
    )
)

/**
 * 숫자 표시 기본 패턴(`#,##0.#`)입니다.
 *
 * ## 동작/계약
 * - [toHuman], [DefaultDecimalFormat]의 기본 포맷으로 사용됩니다.
 * - 소수점 이하 최대 1자리까지 표시합니다.
 * - 상수 문자열이므로 변경 불가능하며 추가 할당이 없습니다.
 * - locale별 기호(콤마/점)는 [DecimalFormat] 해석 규칙을 따릅니다.
 *
 * ```kotlin
 * val pattern = defaultNumberFormatPattern
 * // pattern == "#,##0.#"
 * ```
 */
const val defaultNumberFormatPattern = "#,##0.#"

/**
 * 소수점 자리를 표현하는 기본 포맷
 *
 * 매 호출마다 새 인스턴스를 반환합니다. DecimalFormat은 thread-safe하지 않으므로 각 스레드/호출별로 새 인스턴스를 사용하세요.
 */
val DefaultDecimalFormat: DecimalFormat get() = DecimalFormat(defaultNumberFormatPattern)

/**
 * 숫자를 인간이 읽기 편한 문자열로 만든다
 *
 * ```
 * val number = 1234567890
 * val humanReadable = number.toHuman()  // 1,234,567,890
 * ```
 *
 * @param decimalFormat DecimalFormat (default: #,##0.#)
 */
inline fun Number.toHuman(pattern: String = defaultNumberFormatPattern): String =
    DecimalFormat(pattern).format(this)

/**
 * 문자열이 16진수 숫자를 나타내는 문자열인지 확인한다.
 *
 * ```
 * "0x1234".isHexFormat()  // true
 * "0X1234".isHexFormat()  // true
 * "#1234".isHexFormat()  // true
 * "1234".isHexFormat()  // false
 * ```
 */
fun String.isHexFormat(): Boolean {
    val s = trim().removePrefix("-")
    val digits = when {
        s.startsWith("0x", ignoreCase = true) -> s.substring(2)
        s.startsWith("#")                     -> s.substring(1)
        else                                  -> return false
    }
    return digits.isNotEmpty() && digits.all { it.isHexDigit() }
}

/**
 * 문자가 16진수 숫자 문자인지 검사합니다.
 *
 * ## 동작/계약
 * - `0..9`, `a..f`, `A..F` 범위에 포함되면 `true`를 반환합니다.
 * - whitespace나 부호(`+`, `-`)는 허용하지 않습니다.
 * - 수신 문자를 변경하지 않으며 추가 할당이 없습니다.
 * - 검사 비용은 상수 시간입니다.
 *
 * ```kotlin
 * // 'f'.isHexDigit()
 * // 'A'.isHexDigit()
 * // !'z'.isHexDigit()
 * ```
 */
inline fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/**
 * 문자열을 BigInteger로 디코딩한다.
 *
 * ```
 * "1234".decodeBigInt()  // 1234
 * "-1234".decodeBigInt()  // -1234
 * "0x1234".decodeBigInt()  // 4660
 * "#1234".decodeBigInt()  // 4660
 * ```
 */
fun String.decodeBigInt(): BigInteger {
    if (isBlank()) {
        return BigInteger.ZERO
    }

    var radix = 10
    var index = 0
    var negative = false

    // Handle minus sign, if present.
    if (this.startsWith("-")) {
        negative = true
        index++
    }

    // Handle radix specifier, if present.
    if (this.startsWith("0x", index) || this.startsWith("0X", index)) {
        index += 2
        radix = 16
    } else if (this.startsWith("#", index)) {
        index++
        radix = 16
    } else if (this.startsWith("0", index) && this.length > 1 + index) {
        index++
        radix = 8
    }

    val result = BigInteger(this.substring(index), radix)
    return if (negative) result.negate() else result
}

/**
 * 문자열을 BigDecimal로 디코딩한다. 빈 문자열인 경우 0을 반환한다.
 *
 * ```
 * "1234".decodeBigDecimal()  // 1234
 * "-1234".decodeBigDecimal()  // -1234
 * "1234.5678".decodeBigDecimal()  // 1234.5678
 * "-1234.5678".decodeBigDecimal()  // -1234.5678
 * ```
 */
inline fun String.decodeBigDecimal(): BigDecimal {
    return if (this.isBlank()) BigDecimal.ZERO
    else BigDecimal(this)
}

/**
 * 문자열을 [T] 수형의 숫자로 파싱한다.
 *
 * ```
 * "1234".parseNumber<Int>()  // 1234
 * "1234".parseNumber<Long>()  // 1234L
 * "1234".parseNumber<BigInteger>()  // 1234
 * "1234".parseNumber<Float>()  // 1234.0f
 * "1234".parseNumber<Double>()  // 1234.0
 * "1234".parseNumber<BigDecimal>()  // 1234
 * ```
 */
inline fun <reified T: Number> String.parseNumber(): T {
    val trimmed: String = this.trim()

    return when (T::class) {
        Byte::class       -> (if (trimmed.isHexFormat()) java.lang.Byte.decode(trimmed) else trimmed.toByte()) as T
        Short::class      -> (if (trimmed.isHexFormat()) java.lang.Short.decode(trimmed) else trimmed.toShort()) as T
        Int::class        -> (if (trimmed.isHexFormat()) java.lang.Integer.decode(trimmed) else trimmed.toInt()) as T
        Long::class       -> (if (trimmed.isHexFormat()) java.lang.Long.decode(trimmed) else trimmed.toLong()) as T
        Float::class      -> trimmed.toFloat() as T
        Double::class     -> trimmed.toDouble() as T
        BigDecimal::class -> (if (trimmed.isHexFormat()) trimmed.decodeBigInt()
            .toBigDecimal() else BigDecimal(trimmed)) as T
        BigInteger::class -> (if (trimmed.isHexFormat()) trimmed.decodeBigInt() else BigInteger(trimmed)) as T
        Number::class     -> trimmed.toDouble() as T // trimmed as T
        else              -> throw NotSupportedException("Cannot convert CharSequence[$this] to target class [${T::class.simpleName}")
    }
}

/**
 * 문자열을 [numberFormat]에 맞게 [T] 수형의 숫자로 파싱한다.
 *
 * ```
 * "1234".parseNumber<Int>(DecimalFormat("#,##0.#"))  // 1234
 * "1,234".parseNumber<Int>(DecimalFormat("#,##0.#"))  // 1234
 * "1,234.5678".parseNumber<Float>(DecimalFormat("#,##0.###"))  // 1234.5678
 * ```
 */
inline fun <reified T: Number> String.parseNumber(numberFormat: NumberFormat): T =
    parseNumber(T::class, numberFormat)

/**
 * 문자열을 지정한 [numberFormat] 규칙으로 파싱해 [targetClass] 숫자 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - 입력 문자열은 `trim()` 후 파싱합니다.
 * - [targetClass]가 [BigDecimal]인 경우 필요한 경우 `isParseBigDecimal=true`로 복제 포맷을 사용합니다.
 * - 파싱 실패 시 [IllegalArgumentException]을 발생시킵니다.
 * - 수신 문자열을 변경하지 않으며, 포맷 복제가 필요한 경우 새 포맷 객체를 할당합니다.
 *
 * ```kotlin
 * val format = DecimalFormat("#,##0.##")
 * val value = "1,234.5".parseNumber(BigDecimal::class, format)
 * // value == BigDecimal("1234.5")
 * ```
 *
 * @param targetClass 변환할 최종 숫자 타입
 * @param numberFormat 문자열 파싱에 사용할 숫자 포맷
 * @throws IllegalArgumentException 문자열 파싱에 실패한 경우
 */
fun <T: Number> String.parseNumber(targetClass: KClass<T>, numberFormat: NumberFormat): T {
    val format =
        if (numberFormat is DecimalFormat && BigDecimal::class == targetClass && !numberFormat.isParseBigDecimal) {
            (numberFormat.clone() as DecimalFormat).apply { isParseBigDecimal = true }
        } else {
            numberFormat
        }

    try {
        val number = format.parse(this.trim())
        return number.toTargetClass(targetClass)
    } catch (e: ParseException) {
        throw IllegalArgumentException("Could not parse number: " + e.message)
    }
}

/**
 * 숫자를 [T] 수형의 숫자로 casting 한다
 *
 * ```
 * 1234.toTargetClass<Int>()  // 1234
 * 1234.toTargetClass<Long>()  // 1234L
 * 1234.toTargetClass<BigInteger>()  // 1234
 * 1234.toTargetClass<Float>()  // 1234.0f
 * 1234.toTargetClass<Double>()  // 1234.0
 * 1234.toTargetClass<BigDecimal>()  // 1234
 * ```
 */
inline fun <reified T: Number> Number.toTargetClass(): T = toTargetClass(T::class)

/**
 * 숫자를 [targetClass] 수형의 숫자로 casting 한다
 *
 * ```
 * 1234.toTargetClass(Int::class)  // 1234
 * 1234.toTargetClass(Long::class)  // 1234L
 * 1234.toTargetClass(BigInteger::class)  // 1234
 * 1234.toTargetClass(Float::class)  // 1234.0f
 * 1234.toTargetClass(Double::class)  // 1234.0
 * 1234.toTargetClass(BigDecimal::class)  // 1234
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <T: Number> Number.toTargetClass(targetClass: KClass<T>): T {
    if (targetClass.isInstance(this))
        return this as T

    return when (targetClass) {

        Byte::class       -> {
            val value = checkedLongValue(targetClass)
            if (value !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                raiseOverflowException(this, targetClass)
            }
            value.toByte() as T
        }

        Short::class      -> {
            val value = checkedLongValue(targetClass)
            if (value !in Short.MIN_VALUE..Short.MAX_VALUE) {
                raiseOverflowException(this, targetClass)
            }
            value.toShort() as T
        }

        Int::class        -> {
            val value = checkedLongValue(targetClass)
            if (value !in Int.MIN_VALUE..Int.MAX_VALUE) {
                raiseOverflowException(this, targetClass)
            }
            value.toInt() as T
        }

        Long::class       -> checkedLongValue(targetClass) as T
        BigInteger::class -> {
            when (this) {
                is BigDecimal -> this.toBigInteger()
                is Double     -> this.toBigDecimal().toBigInteger()
                is Float      -> this.toBigDecimal().toBigInteger()
                else          -> toLong().toBigInt()
            } as T
        }
        Float::class      -> this.toFloat() as T
        Double::class     -> this.toDouble() as T
        BigDecimal::class -> this.toBigDecimal() as T
        else              ->
            throw IllegalArgumentException(
                "Could not convert number[$this] of type[${this::class.java.simpleName}] " +
                        "to unsupported target class [${targetClass.simpleName}]"
            )
    }
}

private fun Number.checkedLongValue(targetClass: KClass<out Number>): Long {
    val bigInt: BigInteger? = when (this) {
        is BigInteger -> this
        is BigDecimal -> this.toBigInteger()
        else          -> null
    }
    bigInt?.let {
        if (it !in BigIntMin..BigIntMax) {
            raiseOverflowException(this, targetClass)
        }
    }
    return toLong()
}

private fun raiseOverflowException(number: Number, targetClass: KClass<*>) {
    throw IllegalArgumentException(
        "Could not convert number[$number] of type [${number::class.java.simpleName}] " +
                "to target class [${targetClass.qualifiedName}]: overflow"
    )
}
