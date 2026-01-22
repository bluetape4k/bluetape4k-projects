package io.bluetape4k.math.commons

import kotlin.math.sqrt


/**
 * 제곱 평균 (root-mean-square) - 표준편차와 같은 값이다.
 * 참고: http://en.wikipedia.org/wiki/Root_mean_square
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
 */
fun <N: Number> Iterable<N>.rms(): Double = asSequence().map { it.toDouble() }.rms()

fun DoubleArray.rms(): Double = asSequence().rms()

/**
 * 제곱 평균(root-mean-square) error (RMSE) : 예측치와 실제값과의 오차를 제곱 평균으로 계산합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
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
 */
fun <N: Number> Iterable<N>.rmse(actual: Iterable<N>): Double =
    map { it.toDouble() }.asSequence().rmse(actual.map { it.toDouble() }.asSequence())

fun DoubleArray.rmse(actual: DoubleArray): Double = asSequence().rmse(actual.asSequence())

/**
 * 정규화된 제곱 평균 - Normalized root-mean-square error (RMSE) : 예측치와 실제값과의 오차를 제곱평균으로 계산하고, 정규화합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 */
fun <N: Number> Sequence<N>.normalizedRmse(actual: Sequence<N>): Double {
    return when (val rmse = rmse(actual)) {
        0.0  -> 0.0
        else -> {
            val (min, max) = actual.map { it.toDouble() }.minMax()
            rmse / (max - min)
        }
    }
}

/**
 * 정규화된 제곱 평균 - Normalized root-mean-square error (RMSE) : 예측치와 실제값과의 오차를 제곱평균으로 계산하고, 정규화합니다.
 * 참고 : http://en.wikipedia.org/wiki/Root_mean_square_error
 */
fun <N: Number> Iterable<N>.normalizedRmse(actual: Iterable<N>): Double =
    map { it.toDouble() }.asSequence().normalizedRmse(actual.map { it.toDouble() }.asSequence())

fun DoubleArray.normalizedRmse(actual: DoubleArray): Double =
    asSequence().normalizedRmse(actual.asSequence())
