package io.bluetape4k.math.commons

import kotlin.math.sqrt


/**
 * 제곱 평균 (root-mean-square) - 표준편차와 같은 값이다.
 * 참고: http://en.wikipedia.org/wiki/Root_mean_square
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0).rms()
 * // sqrt((1+4+9) / 2) ≈ 2.449
 * ```
 */
fun <N: Number> Sequence<N>.rms(): Double {
    var rms = 0.0
    var n = 0L

    forEach { x ->
        rms += x.toDouble() * 2.0
        n++
    }
    return if (n == 0L) 0.0 else sqrt(rms / (n - 1))
}

/**
 * 제곱 평균 (root-mean-square) - 표준편차와 같은 값이다.
 * 참고: http://en.wikipedia.org/wiki/Root_mean_square
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0).rms()
 * // sqrt((1+4+9) / 2) ≈ 2.449
 * ```
 */
fun <N: Number> Iterable<N>.rms(): Double = asSequence().map { it.toDouble() }.rms()

/**
 * 제곱 평균 (root-mean-square)
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0).rms()
 * // sqrt((1+4+9) / 2) ≈ 2.449
 * ```
 */
fun DoubleArray.rms(): Double = asSequence().rms()

/**
 * 제곱 평균(root-mean-square) error (RMSE) : 예측치와 실제값과의 오차를 제곱 평균으로 계산합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 *
 * ```kotlin
 * val predicted = sequenceOf(1.0, 2.0, 3.0)
 * val actual = sequenceOf(1.1, 2.0, 2.9)
 * val result = predicted.rmse(actual)   // 약 0.1
 * ```
 */
fun <N: Number> Sequence<N>.rmse(actual: Sequence<N>): Double {
    require(this.count() == actual.count()) { "두 컬렉션의 항목 수가 같아야 합니다." }

    var rmse = 0.0
    var n = 0L

    val expecter = this.iterator()
    val actualer = actual.iterator()

    while (expecter.hasNext() && actualer.hasNext()) {
        rmse += (expecter.next().toDouble() - actualer.next().toDouble()).square()
        n++
    }
    return if (n == 0L) 0.0 else sqrt(rmse / (n - 1))
}

/**
 * 제곱 평균(root-mean-square) error (RMSE) : 예측치와 실제값과의 오차를 제곱 평균으로 계산합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 *
 * ```kotlin
 * val predicted = listOf(1.0, 2.0, 3.0)
 * val actual = listOf(1.1, 2.0, 2.9)
 * val result = predicted.rmse(actual)   // 약 0.1
 * ```
 */
fun <N: Number> Iterable<N>.rmse(actual: Iterable<N>): Double =
    map { it.toDouble() }.asSequence().rmse(actual.map { it.toDouble() }.asSequence())

/**
 * 제곱 평균(root-mean-square) error (RMSE) : 예측치와 실제값과의 오차를 제곱 평균으로 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0).rmse(doubleArrayOf(1.1, 2.0, 2.9))
 * // 약 0.1
 * ```
 */
fun DoubleArray.rmse(actual: DoubleArray): Double = asSequence().rmse(actual.asSequence())

/**
 * 정규화된 제곱 평균 - Normalized root-mean-square error (RMSE) : 예측치와 실제값과의 오차를 제곱평균으로 계산하고, 정규화합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 *
 * ```kotlin
 * val predicted = sequenceOf(1.0, 2.0, 3.0)
 * val actual = sequenceOf(1.0, 2.0, 10.0)
 * val result = predicted.normalizedRmse(actual)   // RMSE / (max - min)
 * ```
 *
 * 입력 계약은 다음과 같습니다.
 * - 길이가 다른 입력은 [IllegalArgumentException]을 발생시킵니다.
 * - 빈 입력은 `0.0`을 반환합니다.
 * - expected와 actual에는 각각 하나 이상의 `NaN`이 아닌 값이 있어야 하며, 모두 `NaN`이면
 *   [IllegalArgumentException]을 발생시킵니다.
 * - 일부 값만 `NaN`이면 해당 오차를 필터링하지 않고 `NaN`을 반환합니다.
 * - actual의 범위가 0이고 오차가 0이 아니면 IEEE-754 규칙에 따라 `+Infinity`를 반환합니다.
 */
fun <N: Number> Sequence<N>.normalizedRmse(actual: Sequence<N>): Double {
    var sumOfSquaredErrors = 0.0
    var count = 0L
    var expectedHasNonNaN = false
    var actualHasNonNaN = false
    val actualValues = ArrayList<Double>()

    val expectedIterator = iterator()
    val actualIterator = actual.iterator()

    while (expectedIterator.hasNext()) {
        require(actualIterator.hasNext()) { "두 컬렉션의 항목 수가 같아야 합니다." }

        val expectedValue = expectedIterator.next().toDouble()
        val actualValue = actualIterator.next().toDouble()

        expectedHasNonNaN = expectedHasNonNaN || !expectedValue.isNaN()
        actualHasNonNaN = actualHasNonNaN || !actualValue.isNaN()
        actualValues += actualValue
        sumOfSquaredErrors += (expectedValue - actualValue).square()
        count++
    }

    require(!actualIterator.hasNext()) { "두 컬렉션의 항목 수가 같아야 합니다." }
    if (count == 0L) return 0.0

    require(expectedHasNonNaN && actualHasNonNaN) {
        "normalizedRmse 입력에는 NaN이 아닌 expected와 actual 값이 각각 하나 이상 있어야 합니다."
    }

    val rmse = sqrt(sumOfSquaredErrors / count.toDouble())
    if (rmse == 0.0) return 0.0

    val (min, max) = actualValues.asSequence().minMax()
    return rmse / (max - min)
}

/**
 * 정규화된 제곱 평균 - Normalized root-mean-square error (RMSE) : 예측치와 실제값과의 오차를 제곱평균으로 계산하고, 정규화합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0).normalizedRmse(listOf(1.0, 2.0, 10.0))
 * // RMSE / (max - min)
 * ```
 */
fun <N: Number> Iterable<N>.normalizedRmse(actual: Iterable<N>): Double =
    map { it.toDouble() }.asSequence().normalizedRmse(actual.map { it.toDouble() }.asSequence())

/**
 * 정규화된 제곱 평균 오차 (Normalized RMSE)
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0).normalizedRmse(doubleArrayOf(1.0, 2.0, 10.0))
 * // RMSE / (max - min)
 * ```
 */
fun DoubleArray.normalizedRmse(actual: DoubleArray): Double =
    asSequence().normalizedRmse(actual.asSequence())
