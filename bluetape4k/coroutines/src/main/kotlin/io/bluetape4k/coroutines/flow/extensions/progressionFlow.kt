package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.support.requireLe
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 지정한 [Char] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * charFlowOf('a', 'c').toList() shouldBeEqualTo listOf('a', 'b', 'c')
 * ```
 * @param start 시작 문자
 * @param endInclusive 끝 문자 (포함)
 * @param step 증가량
 * @return Flow<Char>
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
 * 지정한 [Byte] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * byteFlowOf(1, 3).toList() shouldBeEqualTo listOf(1, 2, 3)
 * ```
 *
 * @param start 시작 바이트
 * @param endInclusive 끝 바이트 (포함)
 * @param step 증가량
 * @return Flow<Byte>
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
 * 지정한 [Int] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * intFlowOf(1, 3).toList() shouldBeEqualTo listOf(1, 2, 3)
 * ```
 *
 * @param start 시작 정수
 * @param endInclusive 끝 정수 (포함)
 * @param step 증가량
 * @return Flow<Int>
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
 * 지정한 [Long] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * longFlowOf(1L, 3L).toList() shouldBeEqualTo listOf(1L, 2L, 3L)
 * ```
 *
 * @param start 시작 정수
 * @param endInclusive 끝 정수 (포함)
 * @param step 증가량
 * @return Flow<Long>
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
 * 지정한 [Float] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * floatFlowOf(1.0F, 3.0F).toList() shouldBeEqualTo listOf(1.0F, 2.0F, 3.0F)
 * ```
 *
 * @param start 시작 실수
 * @param endInclusive 끝 실수 (포함)
 * @param step 증가량
 * @return Flow<Float>
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
 * 지정한 [Double] 범위 (Progression) 를 Flow로 변환합니다.
 *
 * ```
 * doubleFlowOf(1.0, 3.0).toList() shouldBeEqualTo listOf(1.0, 2.0, 3.0)
 * ```
 *
 * @param start 시작 실수
 * @param endInclusive 끝 실수 (포함)
 * @param step 증가량
 * @return Flow<Double>
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
