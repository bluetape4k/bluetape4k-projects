package io.bluetape4k.math.commons

import io.bluetape4k.math.skewness

/**
 * 변량의 분포의 대칭성을 나타냅니다. 0 을 기준으로 좌우로 기울어져 분포하는 것을 표현합니다.
 *
 * @return 분포의 좌우 기울기
 */
fun <N: Number> Iterable<N>.skewness(): Double =
    map { it.toDouble() }.toDoubleArray().skewness

/**
 * 변량의 분포의 대칭성을 나타냅니다. 0 을 기준으로 좌우로 기울어져 분포하는 것을 표현합니다.
 *
 * @return 분포의 좌우 기울기
 */
fun <N: Number> Sequence<N>.skewness(): Double =
    map { it.toDouble() }.toList().toDoubleArray().skewness
