package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.toDoubleArray
import io.bluetape4k.collections.toLongArray
import io.bluetape4k.math.MathConsts.BLOCK_SIZE

/**
 * 이동 합을 구합니다.
 *
 * @param blockSize sum 을 수행할 최대 요소 수 (기본: [BLOCK_SIZE])
 * @return Moving Sum
 */
fun Sequence<Double>.movingSum(blockSize: Int = BLOCK_SIZE): Sequence<Double> {
    assert(blockSize > 1) { "blockSize[$blockSize]는 2 이상이어야 합니다." }

    return sequence {
        var sum = 0.0
        var block = blockSize
        var nans = -1

        val left = this@movingSum.iterator()
        val right = this@movingSum.iterator()

        var value: Double
        while (block > 1) {
            block--
            if (!right.hasNext()) {
                if (nans > 0) {
                    yield(Double.NaN)
                } else {
                    yield(sum)
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
                yield(sum)
            }

            value = left.next()

            if (!value.isNaN()) {
                sum -= value
            }
        }
    }
}

/**
 * 이동 합을 구합니다.
 *
 * @param blockSize sum 을 수행할 최대 요소 수 (기본: [BLOCK_SIZE])
 * @return Moving Sum
 */
fun Iterable<Double>.movingSum(blockSize: Int = BLOCK_SIZE): List<Double> {
    return asSequence().movingSum(blockSize).toFastList()
}

/**
 * 이동 합을 구합니다.
 *
 * @param blockSize sum 을 수행할 최대 요소 수 (기본: [BLOCK_SIZE])
 * @return Moving Sum
 */
fun DoubleArray.movingSum(blockSize: Int = BLOCK_SIZE): DoubleArray {
    return asSequence().movingSum(blockSize).toDoubleArray()
}


/**
 * Moving Sum 을 계산합니다.
 *
 * @param blockSize 합을 계산할 구간
 * @return 이동 합
 */
@JvmName("movingSumOfLong")
fun Sequence<Long>.movingSum(blockSize: Int = BLOCK_SIZE): Sequence<Long> {
    assert(blockSize > 1) { "blockSize[$blockSize]는 2 이상이어야 합니다." }

    return sequence {
        var sum = 0L
        var block = blockSize

        val left = this@movingSum.iterator()
        val right = this@movingSum.iterator()

        while (block > 1) {
            block--
            if (!right.hasNext()) {
                yield(sum)
                return@sequence
            }
            sum += right.next()
        }

        while (right.hasNext()) {
            sum += right.next()
            yield(sum)
            sum -= left.next()
        }
    }
}

@JvmName("movingSumOfLong")
fun Iterable<Long>.movingSum(blockSize: Int = BLOCK_SIZE): List<Long> {
    return asSequence().movingSum(blockSize).toFastList()
}

/**
 * 이동 합을 구합니다.
 *
 * @param blockSize sum 을 수행할 최대 요소 수 (기본: [BLOCK_SIZE])
 * @return Moving Sum
 */
fun LongArray.movingSum(blockSize: Int = BLOCK_SIZE): LongArray {
    return asSequence().movingSum(blockSize).toLongArray()
}
