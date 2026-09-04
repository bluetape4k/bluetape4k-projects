package io.bluetape4k.collections

import io.bluetape4k.support.requirePositiveNumber
import java.math.BigInteger
import java.util.Spliterator
import java.util.Spliterators
import java.util.function.IntConsumer
import java.util.function.LongConsumer
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.StreamSupport

private val BIG_INTEGER_ONE = BigInteger.ONE
private val BIG_INTEGER_INT_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())

private fun progressionElementCount(first: Long, last: Long, step: Long): BigInteger {
    if ((step > 0 && first > last) || (step < 0 && first < last)) {
        return BigInteger.ZERO
    }

    val distance = if (step > 0) {
        BigInteger.valueOf(last).subtract(BigInteger.valueOf(first))
    } else {
        BigInteger.valueOf(first).subtract(BigInteger.valueOf(last))
    }
    return distance.divide(BigInteger.valueOf(step).abs()).add(BIG_INTEGER_ONE)
}

private fun calculatePartitionCount(size: BigInteger, chunkSize: Int): Int {
    if (size.signum() == 0) return 0

    val chunk = BigInteger.valueOf(chunkSize.toLong())
    val partitionCount = size.add(chunk).subtract(BIG_INTEGER_ONE).divide(chunk)
    require(partitionCount <= BIG_INTEGER_INT_MAX) {
        "partition count exceeds Int.MAX_VALUE: $partitionCount"
    }
    return partitionCount.intValueExact()
}

private fun IntProgression.elementCount(): BigInteger =
    progressionElementCount(first.toLong(), last.toLong(), step.toLong())

private fun LongProgression.elementCount(): BigInteger =
    progressionElementCount(first, last, step)

private fun IntProgression.valueAt(index: BigInteger): Int =
    BigInteger.valueOf(first.toLong())
        .add(BigInteger.valueOf(step.toLong()).multiply(index))
        .intValueExact()

private fun LongProgression.valueAt(index: BigInteger): Long =
    BigInteger.valueOf(first)
        .add(BigInteger.valueOf(step).multiply(index))
        .longValueExact()

/**
 * 시작 문자([start]) ~ 종료 문자([endInclusive]) 에 해당하는 [CharProgression]을 빌드합니다.
 *
 * ```
 * val chars = charProgressOf('a', 'd', 1)   // a, b, c
 * ```
 *
 * @param start         시작 [Char]
 * @param endInclusive  종료 [Char] (Exclusive)
 * @param step          증가 값 (기본: 1)
 * @return [CharProgression] 인스턴스
 */
fun charProgressionOf(start: Char, endInclusive: Char, step: Int = 1): CharProgression =
    CharProgression.fromClosedRange(start, endInclusive, step)

/**
 * [IntProgression]을 빌드합니다.
 *
 * ```
 * val ints = intProgressOf(1, 4, 1)   // 1, 2, 3
 * ```
 *
 * @param start         시작 값
 * @param endInclusive  종료 값 (제외)
 * @param step          증가 값 (기본 1)
 * @return [IntProgression] 인스턴스
 */
fun intProgressionOf(start: Int, endInclusive: Int, step: Int = 1): IntProgression =
    IntProgression.fromClosedRange(start, endInclusive, step)

private fun IntProgression.toStreamSpliterator(): Spliterator.OfInt {
    val progressionIterator = iterator()
    return object : Spliterators.AbstractIntSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
        override fun tryAdvance(action: IntConsumer): Boolean {
            if (!progressionIterator.hasNext()) return false
            action.accept(progressionIterator.nextInt())
            return true
        }
    }
}

/**
 * [IntProgression]을 [IntStream]으로 변환합니다.
 *
 * 경계에서 다음 값이 표현 범위를 벗어나면 overflow 값을 방출하지 않고 progression의
 * 마지막 요소에서 정상 종료합니다.
 *
 * ```
 * val ints = intProgressionOf(1, 4, 1)
 * val stream = ints.asStream()
 * stream.count() shouldBeEqualTo 3
 * ```
 */
fun IntProgression.asStream(): IntStream =
    if (step == 1) {
        IntStream.rangeClosed(first, last)
    } else {
        StreamSupport.intStream(toStreamSpliterator(), false)
    }

/**
 * [IntProgression]의 요소를 chunked 하여 [Sequence]로 반환합니다.
 *
 * ```
 * val ints = intProgressionOf(1, 10, 1)
 * ints.size() shouldBeEqualTo 10
 * val chunked = ints.chunked(2).toList()
 * chunked.size shouldBeEqualTo 5
 * ```
 *
 * @param chunk chunk size
 * @return chunk된 [IntProgression]의 [Sequence]
 *
 * progression 요소 수가 커서 계산된 partition 수가 Int 범위를 넘으면
 * [IllegalArgumentException]을 던집니다.
 */
fun IntProgression.chunked(chunk: Int): Sequence<IntProgression> {
    chunk.requirePositiveNumber("chunk")
    val partitionCount = calculatePartitionCount(elementCount(), chunk)
    return if (partitionCount == 0) emptySequence() else partitioning(partitionCount)
}

/**
 * [IntProgression]을 partitioning 하여 [Sequence]로 반환합니다.
 *
 * ```
 * val ints = intProgressionOf(1, 10, 1)
 * val partitioned = ints.partitioning(3).toList()
 * partitioned.size shouldBeEqualTo 3
 * partitioned.forEach {
 *     log.debug { "progression=$it" }
 * }
 * partitioned[0] shouldBeEqualTo intProgressionOf(1, 4)
 * partitioned[1] shouldBeEqualTo intProgressionOf(5, 8)
 * partitioned[2] shouldBeEqualTo intProgressionOf(9, 10)
 * ```
 *
 * @param partitionCount partition count
 * @return partitioned [IntProgression]의 [Sequence]
 *
 * 전체 요소 수와 각 경계는 확장 정밀도로 계산하므로 Int 표현 범위 전체를
 * 포함하는 progression도 요소를 잃지 않습니다. `chunked`에서 계산된 partition
 * 수가 Int 범위를 넘으면 [IllegalArgumentException]을 던집니다.
 */
fun IntProgression.partitioning(partitionCount: Int = 1): Sequence<IntProgression> = sequence {
    partitionCount.requirePositiveNumber("partitionCount")
    val self = this@partitioning

    if (partitionCount == 1) {
        yield(self)
        return@sequence
    }

    val count = self.elementCount()
    if (count.signum() == 0) {
        return@sequence
    }

    val step = self.step
    val partitionSize = count.add(BigInteger.valueOf(partitionCount.toLong()))
        .subtract(BIG_INTEGER_ONE)
        .divide(BigInteger.valueOf(partitionCount.toLong()))
    repeat(partitionCount) { partitionIndex ->
        val startIndex = partitionSize.multiply(BigInteger.valueOf(partitionIndex.toLong()))
        if (startIndex >= count) {
            return@sequence
        }

        val endIndex = startIndex.add(partitionSize).min(count).subtract(BIG_INTEGER_ONE)
        val partition = IntProgression.fromClosedRange(
            self.valueAt(startIndex),
            self.valueAt(endIndex),
            step,
        )
        yield(partition)
    }
}

/**
 * 시작 값([start]) ~ 종료 값([endInclusive]) 사이의 [LongProgression]을 빌드합니다.
 *
 * ```
 * val longs = longProgressionOf(1, 4, 1)   // 1, 2, 3
 * ```
 *
 * @param start 시작 값
 * @param endInclusive 종료 값 (Exclusive)
 * @param step 증가 값 (기본: 1)
 * @return [LongProgression] 인스턴스
 */
fun longProgressionOf(start: Long, endInclusive: Long, step: Long = 1L): LongProgression =
    LongProgression.fromClosedRange(start, endInclusive, step)

private fun LongProgression.toStreamSpliterator(): Spliterator.OfLong {
    val progressionIterator = iterator()
    return object : Spliterators.AbstractLongSpliterator(Long.MAX_VALUE, Spliterator.ORDERED) {
        override fun tryAdvance(action: LongConsumer): Boolean {
            if (!progressionIterator.hasNext()) return false
            action.accept(progressionIterator.nextLong())
            return true
        }
    }
}


/**
 * asStream 기능을 제공합니다.
 *
 * 경계에서 다음 값이 표현 범위를 벗어나면 overflow 값을 방출하지 않고 progression의
 * 마지막 요소에서 정상 종료합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = longProgressionOf(1, 3).asStream().toArray()
 * // result == [1, 2, 3]
 * ```
 */
fun LongProgression.asStream(): LongStream =
    if (step == 1L) {
        LongStream.rangeClosed(first, last)
    } else {
        StreamSupport.longStream(toStreamSpliterator(), false)
    }

/**
 * [LongProgression]의 요소를 chunked 하여 [Sequence]로 반환합니다.
 *
 * ```
 * val longs = longProgressionOf(1, 10, 1)
 * longs.size() shouldBeEqualTo 10
 * val chunked = longs.chunked(2).toList()
 * chunked.size shouldBeEqualTo 5
 * ```
 *
 * @param chunk chunk size
 * @return chunk된 [LongProgression]의 [Sequence]
 *
 * progression 요소 수가 커서 계산된 partition 수가 Int 범위를 넘으면
 * [IllegalArgumentException]을 던집니다.
 */
fun LongProgression.chunked(chunk: Int): Sequence<LongProgression> {
    chunk.requirePositiveNumber("chunk")
    val partitionCount = calculatePartitionCount(elementCount(), chunk)
    return if (partitionCount == 0) emptySequence() else partitioning(partitionCount)
}

/**
 * [LongProgression]을 [partitionCount] 갯수로 분할합니다.
 *
 * ```
 * val longs = longProgressionOf(1, 10, 1)
 * val partitioned = longs.partitioning(3).toList()
 * partitioned.size shouldBeEqualTo 3
 * partitioned.forEach {
 *     log.debug { "progression=$it" }
 * }
 * partitioned[0] shouldBeEqualTo longProgressionOf(1, 4)
 * partitioned[1] shouldBeEqualTo longProgressionOf(5, 8)
 * partitioned[2] shouldBeEqualTo longProgressionOf(9, 10)
 * ```
 *
 * @param partitionCount 분할 갯수 (기본: 1)
 * @return 분할된 [LongProgression]의 [Sequence]
 *
 * 전체 요소 수와 각 경계는 확장 정밀도로 계산하므로 Long 표현 범위 전체를
 * 포함하는 progression도 요소를 잃지 않습니다. `chunked`에서 계산된 partition
 * 수가 Int 범위를 넘으면 [IllegalArgumentException]을 던집니다.
 */
fun LongProgression.partitioning(partitionCount: Int = 1): Sequence<LongProgression> = sequence {
    partitionCount.requirePositiveNumber("partitionCount")
    val self = this@partitioning

    if (partitionCount == 1) {
        yield(self)
        return@sequence
    }

    val count = self.elementCount()
    if (count.signum() == 0) {
        return@sequence
    }

    val step = self.step
    val partitionSize = count.add(BigInteger.valueOf(partitionCount.toLong()))
        .subtract(BIG_INTEGER_ONE)
        .divide(BigInteger.valueOf(partitionCount.toLong()))
    repeat(partitionCount) { partitionIndex ->
        val startIndex = partitionSize.multiply(BigInteger.valueOf(partitionIndex.toLong()))
        if (startIndex >= count) {
            return@sequence
        }

        val endIndex = startIndex.add(partitionSize).min(count).subtract(BIG_INTEGER_ONE)
        val partition = LongProgression.fromClosedRange(
            self.valueAt(startIndex),
            self.valueAt(endIndex),
            step,
        )
        yield(partition)
    }
}
