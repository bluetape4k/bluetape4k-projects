package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.toDoubleArray
import org.eclipse.collections.impl.list.mutable.FastList

/**
 * 시퀀스의 누적 분산을 계산합니다.
 */
fun <N: Number> Sequence<N>.cumulativeVariance(): Sequence<Double> {
    var n = 1
    var sum = first().toDouble()
    var sumSqrt = sum.square()

    return drop(1)
        .map {
            val curr = it.toDouble()
            n++
            sum += curr
            sumSqrt += curr.square()

            (sumSqrt - sum.square() / n) / (n - 1)
        }
}

/**
 * Collection의 누적 분산을 계산합니다.
 */
fun <N: Number> Iterable<N>.cumulativeVariance(
    destination: FastList<Double> = FastList.newList(),
): List<Double> {
    return asSequence().cumulativeVariance().toFastList(destination)
}

fun DoubleArray.cumulativeVariance(): DoubleArray {
    return asSequence().cumulativeVariance().toDoubleArray()
}
