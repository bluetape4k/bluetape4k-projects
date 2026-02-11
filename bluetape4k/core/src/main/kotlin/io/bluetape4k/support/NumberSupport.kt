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
val BigIntMin = Long.MIN_VALUE.toBigInt()

@JvmField
val BigIntMax = Long.MAX_VALUE.toBigInt()

@JvmField
val StandardNumberTypes: HashSet<KClass<out Number>> = hashSetOf(
    Byte::class,
    Short::class,
    Int::class,
    Long::class,
    Float::class,
    Double::class,
    BigDecimal::class,
    BigInteger::class,
)

const val defaultNumberFormatPattern = "#,##0.#"
/**
 * 소수점 자리를 표현하는 기본 포맷
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
 * 숫자의 범위를 [minValue]와 [maxValue] 사이로 제한한다.
 *
 * ```
 * val number = 123
 * number.coerce(0, 100)  // 100
 * number.coerce(200, 300)  // 200
 * number.coerce(0, 200)  // 123
 * ```
 */
@Deprecated(
    message = "stdlib의 coerceIn을 사용하세요.",
    replaceWith = ReplaceWith("this.coerceIn(minValue, maxValue)"),
)
fun <T> T.coerce(minValue: T, maxValue: T): T where T: Number, T: Comparable<T> =
    this.coerceIn(minValue, maxValue)

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
