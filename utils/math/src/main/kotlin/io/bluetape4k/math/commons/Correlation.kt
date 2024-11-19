package io.bluetape4k.math.commons

import kotlin.math.sqrt

/**
 * 두 차원의 변량들의 상관관계 계수를 계산합니다.
 *
 * @param that 두번째 변량 시퀀스
 * @return 두 변량 시퀀스의 상관관계 계수
 */
fun Sequence<Double>.correlationCoefficient(that: Sequence<Double>): Double {
    var sxx = 0.0
    var syy = 0.0
    var sxy = 0.0

    val count = this.count()
    val sx = this.average()
    val sy = that.average()

    val iter1 = this.iterator()
    val iter2 = that.iterator()

    while (iter1.hasNext() && iter2.hasNext()) {
        val dx = iter1.next() - sx
        val dy = iter2.next() - sy

        sxx += dx * dx
        syy += dy * dy
        sxy += dx * dy
    }

    sxx = sqrt(sxx / (count - 1))
    syy = sqrt(syy / (count - 1))
    sxy /= (count - 1) * sxx * syy

    return sxy
}

/**
 * 두 차원의 변량들의 상관관계 계수를 계산합니다.
 *
 * @param that 두번째 변량 컬렉션
 * @return 두 변량 컬렉션의 상관관계 계수
 */
fun Iterable<Double>.correlationCoefficient(that: Iterable<Double>): Double {
    return asSequence().correlationCoefficient(that.asSequence())
}

/**
 * 두 차원의 변량들의 상관관계 계수를 계산합니다.
 *
 * @param that 두번째 변량 컬렉션
 * @return 두 변량 컬렉션의 상관관계 계수
 */
fun DoubleArray.correlationCoefficient(that: DoubleArray): Double {
    return asSequence().correlationCoefficient(that.asSequence())
}
