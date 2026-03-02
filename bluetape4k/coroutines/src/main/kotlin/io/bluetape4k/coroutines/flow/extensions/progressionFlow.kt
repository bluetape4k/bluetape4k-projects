package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireLe
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 문자 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - Char overflow로 값이 되감기면 무한 루프를 방지하고 즉시 종료합니다.
 *
 * ```kotlin
 * val out = charFlowOf('a', 'e', 2).toList()
 * // out == ['a', 'c', 'e']
 * ```
 * @param start 시작 문자입니다.
 * @param endInclusive 마지막 문자(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun charFlowOf(start: Char, endInclusive: Char, step: Int = 1): Flow<Char> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")
    var current = start
    while (current <= endInclusive) {
        emit(current)
        // Overflow로 값이 되감기면 무한 루프가 될 수 있으므로 종료합니다.
        val next = current + step
        if (next <= current) break
        current = next
    }
}

/**
 * Byte 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - Byte overflow가 발생하면 무한 루프를 방지하고 즉시 종료합니다.
 *
 * ```kotlin
 * val out = byteFlowOf(1, 5, 2).toList()
 * // out == [1, 3, 5]
 * ```
 * @param start 시작 값입니다.
 * @param endInclusive 마지막 값(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun byteFlowOf(start: Byte, endInclusive: Byte, step: Byte = 1): Flow<Byte> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")

    var current = start
    while (current <= endInclusive) {
        emit(current)
        // Byte overflow 방지
        val next = (current + step).toByte()
        if (next <= current) break
        current = next
    }
}

/**
 * Int 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - Int overflow가 발생하면 무한 루프를 방지하고 즉시 종료합니다.
 *
 * ```kotlin
 * val out = intFlowOf(1, 5, 2).toList()
 * // out == [1, 3, 5]
 * ```
 * @param start 시작 값입니다.
 * @param endInclusive 마지막 값(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun intFlowOf(start: Int, endInclusive: Int, step: Int = 1): Flow<Int> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")
    var current = start
    while (current <= endInclusive) {
        emit(current)
        // Int overflow 방지
        val next = current + step
        if (next <= current) break
        current = next
    }
}

/**
 * Long 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - Long overflow가 발생하면 무한 루프를 방지하고 즉시 종료합니다.
 *
 * ```kotlin
 * val out = longFlowOf(1L, 5L, 2L).toList()
 * // out == [1, 3, 5]
 * ```
 * @param start 시작 값입니다.
 * @param endInclusive 마지막 값(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun longFlowOf(start: Long, endInclusive: Long, step: Long = 1L): Flow<Long> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")
    var current = start
    while (current <= endInclusive) {
        emit(current)
        // Long overflow 방지
        val next = current + step
        if (next <= current) break
        current = next
    }
}

/**
 * Float 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - 부동소수 정밀도 한계로 `next <= current`가 되면 무한 루프를 방지하고 종료합니다.
 *
 * ```kotlin
 * val out = floatFlowOf(1f, 5f, 2f).toList()
 * // out == [1.0, 3.0, 5.0]
 * ```
 * @param start 시작 값입니다.
 * @param endInclusive 마지막 값(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun floatFlowOf(start: Float, endInclusive: Float, step: Float = 1.0F): Flow<Float> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")

    var current = start
    while (current <= endInclusive) {
        emit(current)
        // 부동소수 정밀도 한계로 next == current 가 되면 종료합니다.
        val next = current + step
        if (next <= current) break
        current = next
    }
}

/**
 * Double 범위를 순차 방출하는 Flow를 생성합니다.
 *
 * ## 동작/계약
 * - `start <= endInclusive`, `step > 0`를 만족하지 않으면 `IllegalArgumentException`을 던집니다.
 * - `start`부터 `step` 간격으로 증가하며 `endInclusive` 이하 값만 방출합니다.
 * - 부동소수 정밀도 한계로 `next <= current`가 되면 무한 루프를 방지하고 종료합니다.
 *
 * ```kotlin
 * val out = doubleFlowOf(1.0, 5.0, 2.0).toList()
 * // out == [1.0, 3.0, 5.0]
 * ```
 * @param start 시작 값입니다.
 * @param endInclusive 마지막 값(포함)입니다.
 * @param step 증가 간격입니다. 0 이하이면 예외가 발생합니다.
 */
fun doubleFlowOf(start: Double, endInclusive: Double, step: Double = 1.0): Flow<Double> = flow {
    start.requireLe(endInclusive, "start")
    step.requirePositiveNumber("step")
    var current = start
    while (current <= endInclusive) {
        emit(current)
        // 부동소수 정밀도 한계로 next == current 가 되면 종료합니다.
        val next = current + step
        if (next <= current) break
        current = next
    }
}
