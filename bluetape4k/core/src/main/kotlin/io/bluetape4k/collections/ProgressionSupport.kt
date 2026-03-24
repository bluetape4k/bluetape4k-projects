package io.bluetape4k.collections

import io.bluetape4k.support.assertPositiveNumber
import java.util.stream.IntStream
import java.util.stream.LongStream

private fun calculatePartitionCount(size: Int, chunkSize: Int): Int =
    size / chunkSize + if (size % chunkSize > 0) 1 else 0

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

/**
 * [IntProgression]을 [IntStream]으로 변환합니다.
 *
 * ```
 * val ints = intProgressionOf(1, 4, 1)
 * val stream = ints.asStream()
 * stream.count() shouldBeEqualTo 3
 * ```
 */
fun IntProgression.asStream(): IntStream =
    IntStream.builder()
        .also { builder -> forEach { builder.add(it) } }
        .build()

/**
 * [IntProgression]의 요소를 chunked 하여 [Sequence]로 반환합니다.
 *
 * @param groupSize group size
 * @return 그룹된 [IntProgression]의 [Sequence]
 */
@Deprecated("Use chunked instead", ReplaceWith("chunked(groupSize)"))
fun IntProgression.grouped(groupSize: Int): Sequence<IntProgression> =
    partitioning(partitionCount = calculatePartitionCount(count(), groupSize.also { it.assertPositiveNumber("groupSize") }))

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
 */
fun IntProgression.chunked(chunk: Int): Sequence<IntProgression> =
    partitioning(partitionCount = calculatePartitionCount(count(), chunk.also { it.assertPositiveNumber("chunk") }))

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
 */
fun IntProgression.partitioning(partitionCount: Int = 1): Sequence<IntProgression> = sequence {
    partitionCount.assertPositiveNumber("partitionCount")
    val self = this@partitioning

    if (partitionCount == 1) {
        yield(self)
        return@sequence
    }

    val step = self.step
    val count = if (step > 0) (self.last - self.first) / step + 1 else (self.first - self.last) / (-step) + 1
    val reminder = count % partitionCount
    val partitionSize = count / partitionCount + (if (reminder > 0) 1 else 0)

    var start = self.first
    repeat(partitionCount) {
        if ((step > 0 && start > self.last) || (step < 0 && start < self.last)) {
            return@sequence
        }
        var endInclusive = start
        repeat(partitionSize - 1) {
            endInclusive += self.step
        }
        endInclusive = when {
            step > 0 -> endInclusive.coerceAtMost(self.last)
            else     -> endInclusive.coerceAtLeast(self.last)
        }

        val partition = IntProgression.fromClosedRange(start, endInclusive, step)
        yield(partition)
        start = endInclusive + step
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


/**
 * asStream 기능을 제공합니다.
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
    LongStream.builder()
        .also { builder -> forEach { builder.add(it) } }
        .build()

/**
 * [LongProgression]의 요소를 chunked 하여 [Sequence]로 반환합니다.
 *
 * @param groupSize group size
 * @return 그룹된 [IntProgression]의 [Sequence]
 */
@Deprecated("Use chunked instead", ReplaceWith("chunked(groupSize)"))
fun LongProgression.grouped(groupSize: Int): Sequence<LongProgression> =
    partitioning(partitionCount = calculatePartitionCount(count(), groupSize.also { it.assertPositiveNumber("groupSize") }))

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
 * @return chunk된 [IntProgression]의 [Sequence]
 */
fun LongProgression.chunked(chunk: Int): Sequence<LongProgression> =
    partitioning(partitionCount = calculatePartitionCount(count(), chunk.also { it.assertPositiveNumber("chunk") }))

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
 */
fun LongProgression.partitioning(partitionCount: Int = 1): Sequence<LongProgression> = sequence {
    partitionCount.assertPositiveNumber("partitionCount")
    val self = this@partitioning

    if (partitionCount == 1) {
        yield(self)
        return@sequence
    }

    val step = self.step
    val count = (if (step > 0) (self.last - self.first) / step + 1 else (self.first - self.last) / (-step) + 1).toInt()
    val reminder = count % partitionCount
    val partitionSize = count / partitionCount + (if (reminder > 0) 1 else 0)

    var start = self.first
    repeat(partitionCount) {
        if ((step > 0 && start > self.last) || (step < 0 && start < self.last)) {
            return@sequence
        }
        var endInclusive = start
        repeat(partitionSize - 1) {
            endInclusive += self.step
        }
        endInclusive = when {
            step > 0 -> endInclusive.coerceAtMost(self.last)
            else     -> endInclusive.coerceAtLeast(self.last)
        }

        val partition = LongProgression.fromClosedRange(start, endInclusive, step)
        yield(partition)
        start = endInclusive + step
    }
}
