package io.bluetape4k.math.commons

import io.bluetape4k.collections.toDoubleArray
import io.bluetape4k.math.MathConsts.BLOCK_SIZE
import java.util.concurrent.ArrayBlockingQueue

/**
 * 표준 이동평균 (Standard Moving Average)
 *
 * ```kotlin
 * val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.standardMovingAverage(blockSize = 3).toList()
 * // result == [2.0, 3.0, 4.0]
 * ```
 *
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return 이동평균
 */
fun Sequence<Double>.standardMovingAverage(blockSize: Int = BLOCK_SIZE): Sequence<Double> {
    assert(blockSize > 1) { "blockSize[$blockSize]는 2 이상이어야 합니다." }

    return sequence {
        var sum = 0.0
        var block = blockSize
        var nans = -1

        val left = this@standardMovingAverage.iterator()
        val right = this@standardMovingAverage.iterator()

        var value: Double

        while (block > 1) {
            block--
            if (!right.hasNext()) {
                if (nans > 0)
                    yield(Double.NaN)
                else
                    yield(sum / (blockSize - block - 1))
                break
            }

            value = right.next()
            if (value.isNaN()) {
                nans = blockSize
            } else {
                sum += value
                nans--
            }
        }

        while (right.hasNext()) {
            value = right.next()

            if (value.isNaN()) {
                nans = blockSize
            } else {
                sum += value
                nans--
            }
            if (nans > 0) {
                yield(Double.NaN)
            } else {
                yield(sum / blockSize)
            }

            value = left.next()
            if (!value.isNaN()) {
                sum -= value
            }
        }
    }
}

/**
 * 표준 이동평균 (Standard Moving Average)
 *
 * ```kotlin
 * val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.standardMovingAverage(blockSize = 3)
 * // result == [2.0, 3.0, 4.0]
 * ```
 *
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return 이동평균
 */
fun Iterable<Double>.standardMovingAverage(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().standardMovingAverage(blockSize).toDoubleArray()
}

/**
 * 표준 이동평균 (Standard Moving Average)
 *
 * ```kotlin
 * val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.standardMovingAverage(blockSize = 3)
 * // result == [2.0, 3.0, 4.0]
 * ```
 *
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return 이동평균
 */
fun DoubleArray.standardMovingAverage(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().standardMovingAverage(blockSize).toDoubleArray()
}

/**
 * 지수 방식으로 이동평균을 구합니다. (표준방식보다 부드러운 곡선을 만듭니다)
 *
 * ```kotlin
 * val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.exponentialMovingAverage(blockSize = 3).toList()
 * // result ≈ [2.0, 3.0, 4.0] (지수 가중 평균)
 * ```
 *
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return 지수 이동평균 시퀀스
 */
fun Sequence<Double>.exponentialMovingAverage(blockSize: Int = BLOCK_SIZE): Sequence<Double> {
    assert(blockSize > 1) { "blockSize[$blockSize]는 2 이상이어야 합니다." }

    return sequence {
        var sum = 0.0
        var block = blockSize
        var nans = -1

        val factor = 2.0 / (blockSize + 1)
        var prevAvg = 0.0

        val left = this@exponentialMovingAverage.iterator()
        val right = this@exponentialMovingAverage.iterator()

        var value: Double

        while (block > 1) {
            block--
            if (!right.hasNext()) {
                if (nans > 0) {
                    yield(Double.NaN)
                } else {
                    prevAvg = sum / (blockSize - block - 1)
                    yield(prevAvg)
                }
                break
            }
            value = right.next()

            if (value.isNaN()) {
                nans = blockSize
            } else {
                sum += value
                nans--
            }
        }

        while (right.hasNext()) {
            value = right.next()

            if (value.isNaN()) {
                nans = blockSize
            } else {
                sum += value
                nans--
            }

            if (nans > 0) {
                yield(Double.NaN)
            } else {
                val result = factor * (value - prevAvg) + prevAvg
                yield(result)
                prevAvg = result
            }

            value = left.next()
            if (!value.isNaN()) {
                sum -= value
            }
        }
    }
}

/**
 * 지수 방식으로 이동평균을 구합니다. (표준방식보다 부드러운 곡선을 만듭니다)
 *
 * ```kotlin
 * val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.exponentialMovingAverage(blockSize = 3)
 * // result ≈ [2.0, 3.0, 4.0] (지수 가중 평균)
 * ```
 */
fun Iterable<Double>.exponentialMovingAverage(blockSize: Int = BLOCK_SIZE): List<Double> {
    return asSequence().exponentialMovingAverage(blockSize).toList()
}

/**
 * 지수 방식으로 이동평균을 구합니다. (표준방식보다 부드러운 곡선을 만듭니다)
 *
 * ```kotlin
 * val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.expontentialMovingAverage(blockSize = 3)
 * // result ≈ [2.0, 3.0, 4.0] (지수 가중 평균)
 * ```
 */
fun DoubleArray.expontentialMovingAverage(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().exponentialMovingAverage(blockSize).toDoubleArray()
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * ```kotlin
 * val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.cumulativeMovingAverage().toList()
 * // result == [1.0, 1.5, 2.0, 2.5, 3.0]
 * ```
 *
 * @return 누적 이동평균
 */
fun Sequence<Double>.cumulativeMovingAverage(): Sequence<Double> {
    var sum = 0.0
    var idx = 0
    return map {
        sum += it
        sum / ++idx
    }
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * ```kotlin
 * val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.cumulativeMovingAverage()
 * // result == [1.0, 1.5, 2.0, 2.5, 3.0]
 * ```
 *
 * @return 누적 이동평균
 */
fun Iterable<Double>.cumulativeMovingAverage(): List<Double> {
    return asSequence().cumulativeMovingAverage().toList()
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * ```kotlin
 * val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.cumulativeMovingAverage()
 * // result == [1.0, 1.5, 2.0, 2.5, 3.0]
 * ```
 *
 * @return 누적 이동평균
 */
fun DoubleArray.cumulativeMovingAverage(): DoubleArray {
    return asSequence().cumulativeMovingAverage().toDoubleArray()
}

/**
 * 지정한 시퀀스의 항목에 가중치를 준 이동평균을 계산합니다.
 *
 * ```kotlin
 * val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.weightedMovingAverage(blockSize = 3) { i -> i.toDouble() }.toList()
 * // result ≈ [2.33, 3.33, 4.33] (가중치 1, 2, 3 적용)
 * ```
 *
 * @param blockSize  이동평균 계산 시 변량 수
 * @param weightingFunc 가중치 함수
 * @return 가중치가 적용된 이동평균
 */
inline fun Sequence<Double>.weightedMovingAverage(
    blockSize: Int = BLOCK_SIZE,
    crossinline weightingFunc: (Int) -> Double,
): Sequence<Double> {
    assert(blockSize > 1) { "blockSize[$blockSize]는 2 이상이어야 합니다." }

    return sequence {
        val queue = ArrayBlockingQueue<Double>(blockSize)
        val factors = DoubleArray(blockSize)

        val iter = this@weightedMovingAverage.iterator()

        for (i in 0 until (blockSize - 1)) {
            check(iter.hasNext()) { "컬렉션의 항목 수가 blockSize[$blockSize]보다 커야합니다." }
            queue.put(iter.next())
            factors[i] = weightingFunc(i + 1)
        }

        factors[blockSize - 1] = weightingFunc(blockSize)
        val factorSum = factors.sum()
        val factorList = factors.toList()

        while (iter.hasNext()) {
            queue.put(iter.next())
            yield((queue * factorList).sum() / factorSum)
            queue.take()
        }
    }
}

/**
 * 지정한 컬렉션의 항목에 가중치를 준 이동평균을 계산합니다.
 *
 * ```kotlin
 * val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.weightedMovingAverage(blockSize = 3) { i -> i.toDouble() }
 * // result ≈ [2.33, 3.33, 4.33] (가중치 1, 2, 3 적용)
 * ```
 */
inline fun Iterable<Double>.weightedMovingAverage(
    blockSize: Int = BLOCK_SIZE,
    crossinline weightingFunc: (Int) -> Double,
): List<Double> {
    return asSequence().weightedMovingAverage(blockSize, weightingFunc).toList()
}

/**
 * 지정한 배열의 항목에 가중치를 준 이동평균을 계산합니다.
 *
 * ```kotlin
 * val data = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = data.weightedMovingAverage(blockSize = 3) { i -> i.toDouble() }
 * // result ≈ [2.33, 3.33, 4.33] (가중치 1, 2, 3 적용)
 * ```
 */
inline fun DoubleArray.weightedMovingAverage(
    blockSize: Int = BLOCK_SIZE,
    crossinline weightingFunc: (Int) -> Double,
): DoubleArray {
    return asSequence().weightedMovingAverage(blockSize, weightingFunc).toDoubleArray()
}
