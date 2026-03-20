package io.bluetape4k.spring4.util

import org.springframework.util.NumberUtils
import java.text.NumberFormat

/**
 * 문자열을 reified 숫자 타입으로 파싱합니다.
 *
 * ## 동작/계약
 * - [NumberUtils.parseNumber]에 [NumberFormat]을 전달해 파싱합니다.
 * - 테스트에서 `"123".parseNumber<Int>()` 결과는 `123`입니다.
 *
 * ```kotlin
 * val intValue = "123".parseNumber<Int>()
 * // intValue == 123
 * ```
 */
inline fun <reified T : Number> String.parseNumber(numberFormat: NumberFormat = NumberFormat.getInstance()): T =
    NumberUtils.parseNumber(this, T::class.java, numberFormat)

/**
 * 문자열을 지정한 숫자 클래스로 파싱합니다.
 *
 * ## 동작/계약
 * - [targetClass] 타입으로 변환 가능한 숫자를 반환합니다.
 * - 테스트에서 `"123.4".parseNumber(Int::class.java, ...)` 결과는 `123`입니다.
 *
 * ```kotlin
 * val value = "123.4".parseNumber(Int::class.java, NumberFormat.getNumberInstance())
 * // value == 123
 * ```
 */
fun <T : Number> String.parseNumber(
    targetClass: Class<T>,
    numberFormat: NumberFormat = NumberFormat.getInstance(),
): T = NumberUtils.parseNumber(this, targetClass, numberFormat)

/**
 * 숫자를 reified 대상 숫자 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - [NumberUtils.convertNumberToTargetClass]를 사용해 숫자 변환을 수행합니다.
 * - 테스트에서 `123.toTargetClass<Double>()` 결과는 `123.0`입니다.
 *
 * ```kotlin
 * val converted = 123.toTargetClass<Double>()
 * // converted == 123.0
 * ```
 */
inline fun <reified T : Number> Number.toTargetClass(): T = NumberUtils.convertNumberToTargetClass(this, T::class.java)

/**
 * 숫자를 reified 대상 숫자 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - [toTargetClass]와 동일하게 [NumberUtils.convertNumberToTargetClass]를 호출합니다.
 * - 테스트에서 `123.4.convertAs<Long>()` 결과는 `123L`입니다.
 *
 * ```kotlin
 * val converted = 123.4.convertAs<Long>()
 * // converted == 123L
 * ```
 */
inline fun <reified T : Number> Number.convertAs(): T = NumberUtils.convertNumberToTargetClass(this, T::class.java)
