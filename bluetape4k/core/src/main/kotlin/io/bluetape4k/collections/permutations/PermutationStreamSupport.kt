@file:JvmName("PermutationStreamSupport")

package io.bluetape4k.collections.permutations

import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

/**
 * [Permutation]을 [Stream]으로 래핑합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1, 2, 3)
 * val stream = streamOf(perm)
 * stream.collect(Collectors.toList()) // [1, 2, 3]
 * ```
 *
 * @param permutation 변환할 순열
 * @return Stream 래퍼
 */
fun <T> streamOf(permutation: Permutation<T>): Stream<T> {
    return PermutationStream(permutation)
}

/**
 * [Permutation]을 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val perm = permutationOf("a", "b", "c")
 * perm.toStream()
 *     .filter { it != "b" }
 *     .collect(Collectors.toList()) // ["a", "c"]
 * ```
 *
 * @return Stream 래퍼
 */
fun <T> Permutation<T>.toStream(): Stream<T> {
    return PermutationStream(this)
}

/**
 * [Stream]을 [Permutation]으로 변환합니다.
 *
 * ```kotlin
 * val stream: Stream<Int> = Stream.of(1, 2, 3)
 * val perm = permutationOf(stream)
 * perm.toList() // [1, 2, 3]
 *
 * val nullStream: Stream<Int>? = null
 * permutationOf(nullStream) // emptyPermutation()
 * ```
 *
 * @param stream 변환할 Stream (null이면 빈 순열)
 * @return 변환된 순열
 */
fun <T> permutationOf(stream: Stream<T>?): Permutation<T> {
    if (stream == null)
        return emptyPermutation()

    return permutationOf(stream.iterator())
}

/**
 * [Stream]을 [Permutation]으로 변환합니다.
 *
 * ```kotlin
 * val perm = Stream.of(10, 20, 30).toPermutation()
 * perm.toList() // [10, 20, 30]
 *
 * val nullStream: Stream<Int>? = null
 * nullStream.toPermutation() // emptyPermutation()
 * ```
 *
 * @return 변환된 순열 (null이면 빈 순열)
 */
fun <T> Stream<T>?.toPermutation(): Permutation<T> {
    if (this == null)
        return emptyPermutation()

    return permutationOf(this.iterator())
}

/**
 * [IntStream]을 Int 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = IntStream.rangeClosed(1, 5).toPermutation()
 * perm.toList() // [1, 2, 3, 4, 5]
 * ```
 *
 * @return Int 순열
 */
fun IntStream.toPermutation(): Permutation<Int> = permutationOf(iterator())

/**
 * [LongStream]을 Long 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = LongStream.of(100L, 200L, 300L).toPermutation()
 * perm.toList() // [100L, 200L, 300L]
 * ```
 *
 * @return Long 순열
 */
fun LongStream.toPermutation(): Permutation<Long> = permutationOf(iterator())

/**
 * [DoubleStream]을 Double 순열로 변환합니다.
 *
 * ```kotlin
 * val perm = DoubleStream.of(1.1, 2.2, 3.3).toPermutation()
 * perm.toList() // [1.1, 2.2, 3.3]
 * ```
 *
 * @return Double 순열
 */
fun DoubleStream.toPermutation(): Permutation<Double> = permutationOf(iterator())

/**
 * Int 순열을 [IntStream]으로 변환합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1, 2, 3, 4, 5)
 * perm.toIntStream().sum() // 15
 * ```
 *
 * @return IntStream
 */
fun Permutation<Int>.toIntStream(): IntStream = toStream().mapToInt { it }

/**
 * Long 순열을 [LongStream]으로 변환합니다.
 *
 * ```kotlin
 * val perm = permutationOf(10L, 20L, 30L)
 * perm.toLongStream().sum() // 60L
 * ```
 *
 * @return LongStream
 */
fun Permutation<Long>.toLongStream(): LongStream = toStream().mapToLong { it }

/**
 * Float 순열을 [DoubleStream]으로 변환합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1.5f, 2.5f, 3.0f)
 * perm.toFloatStream().sum() // 7.0
 * ```
 *
 * @return DoubleStream
 */
fun Permutation<Float>.toFloatStream(): DoubleStream = toStream().mapToDouble(Float::toDouble)

/**
 * Double 순열을 [DoubleStream]으로 변환합니다.
 *
 * ```kotlin
 * val perm = permutationOf(1.0, 2.0, 3.0)
 * perm.toDoubleStream().average().orElse(0.0) // 2.0
 * ```
 *
 * @return DoubleStream
 */
fun Permutation<Double>.toDoubleStream(): DoubleStream = toStream().mapToDouble { it }
