package io.bluetape4k.math.commons

import io.bluetape4k.collections.toDoubleArray
import io.bluetape4k.math.MathConsts.BLOCK_SIZE
import java.util.concurrent.ArrayBlockingQueue

/**
 * 표준 이동평균 (Standard Moving Average)
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
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return 이동평균
 */
fun Iterable<Double>.standardMovingAverage(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().standardMovingAverage(blockSize).toDoubleArray()
}

/**
 * 표준 이동평균 (Standard Moving Average)
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
 * @param blockSize 이동 평균을 계산하기 위한 항목 수 (최소 2)
 * @return
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

fun Iterable<Double>.exponentialMovingAverage(blockSize: Int = BLOCK_SIZE): Iterable<Double> {
    return asSequence().exponentialMovingAverage(blockSize).asIterable()
}

fun DoubleArray.expontentialMovingAverage(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().exponentialMovingAverage(blockSize).toDoubleArray()
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * @return 누적 이동평균
 */
fun Sequence<Double>.cumulativeMovingAverage(): Sequence<Double> = sequence {
    var sum = 0.0
    var idx = 0

    this@cumulativeMovingAverage.forEach {
        sum += it
        yield(sum / ++idx)
    }
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * @return 누적 이동평균
 */
fun Iterable<Double>.cumulativeMovingAverage(): Iterable<Double> {
    return asSequence().cumulativeMovingAverage().asIterable()
}

/**
 * 누적 이동평균 (Cumulative Moving Average)
 *
 * @return 누적 이동평균
 */
fun DoubleArray.cumulativeMovingAverage(): DoubleArray {
    return asSequence().cumulativeMovingAverage().toDoubleArray()
}

/**
 * 지정한 시퀀스의 항목에 가중치를 준 이동평균을 계산합니다.
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

inline fun Iterable<Double>.weightedMovingAverage(
    blockSize: Int = BLOCK_SIZE,
    crossinline weightingFunc: (Int) -> Double,
): Iterable<Double> {
    return asSequence().weightedMovingAverage(blockSize, weightingFunc).asIterable()
}

inline fun DoubleArray.weightedMovingAverage(
    blockSize: Int = BLOCK_SIZE,
    crossinline weightingFunc: (Int) -> Double,
): DoubleArray {
    return asSequence().weightedMovingAverage(blockSize, weightingFunc).toDoubleArray()
}
