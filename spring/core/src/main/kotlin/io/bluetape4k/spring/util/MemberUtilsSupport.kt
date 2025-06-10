package io.bluetape4k.spring.util

import org.springframework.util.NumberUtils
import java.text.NumberFormat

/**
 * 문자열을 [numberFormat] 형식으로 Number 타입으로 파싱합니다.
 *
 * ```
 * "123".parseNumber<Int>(NumberFormat.getInstance()) // 123
 * "1,234.56".parseNumber<Double>(NumberFormat.getInstance()) // 1234.56
 * ```
 *
 * @receiver String 파싱할 문자열
 * @param numberFormat NumberFormat 파싱할 문자열의 형식
 * @return T 파싱된 Number 타입
 */
inline fun <reified T: Number> String.parseNumber(
    numberFormat: NumberFormat = NumberFormat.getInstance(),
): T =
    NumberUtils.parseNumber(this, T::class.java, numberFormat)

/**
 * 문자열을 [targetClass] 형식으로 Number 타입으로 파싱합니다.
 *
 * ```
 * "123".parseNumber(Int::class.java, NumberFormat.getInstance()) // 123
 * "1,234.56".parseNumber(Double::class.java, NumberFormat.getInstance()) // 1234.56
 * ```
 *
 * @receiver String 파싱할 문자열
 * @param targetClass Class<T> 파싱할 Number 타입
 */
fun <T: Number> String.parseNumber(
    targetClass: Class<T>,
    numberFormat: NumberFormat = NumberFormat.getInstance(),
): T =
    NumberUtils.parseNumber(this, targetClass, numberFormat)

/**
 * Number 타입을 [T] 타입으로 변환합니다.
 *
 * ```
 * 123.toTargetClass<Int>() // 123
 * 123.toTargetClass<Double>() // 123.0
 * ```
 *
 * @receiver Number 변환할 Number 타입
 * @return T 변환된 Number 타입
 */
inline fun <reified T: Number> Number.toTargetClass(): T =
    NumberUtils.convertNumberToTargetClass(this, T::class.java)

/**
 * Number 타입을 [T] 타입으로 변환합니다.
 *
 * ```
 * 123.convertAs<Int>() // 123
 * 123.convertAs<Double>() // 123.0
 * ```
 *
 * @receiver Number 변환할 Number 타입
 * @return T 변환된 Number 타입
 */
inline fun <reified T: Number> Number.convertAs(): T =
    NumberUtils.convertNumberToTargetClass(this, T::class.java)
