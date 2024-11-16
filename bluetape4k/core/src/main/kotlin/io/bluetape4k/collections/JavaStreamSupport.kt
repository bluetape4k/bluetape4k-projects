package io.bluetape4k.collections

import java.util.*
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * [Stream]을 [Sequence]로 변환합니다.
 *
 * ```
 * val stream = Stream.of(1, 2, 3, 4, 5)
 * val sequence = stream.asSequence()
 * ```
 *
 * @receiver Stream
 * @return Sequence<T> 인스턴스
 */
fun <T> Stream<T>.asSequence(): Sequence<T> = Sequence { iterator() }

/**
 * [Stream]을 [List]로 변환합니다.
 *
 * @receiver Stream
 * @return List<T> 인스턴스
 */
fun <T> Stream<T>.asIterable(): Iterable<T> = Iterable { iterator() }

/**
 * [Stream]을 [Set]로 변환합니다.
 *
 * @receiver Stream
 * @return List<T> 인스턴스
 */
fun <T> Stream<T>.toSet(): Set<T> = asIterable().toSet()

/**
 * [Stream]을 [Map] 으로 변환합니다.
 *
 * ```
 * val stream = Stream.of(1, 2, 3, 4, 5)
 * val map = stream.toMap { it to it * 2 } // {1=2, 2=4, 3=6, 4=8, 5=10}
 * ```
 *
 * @param T  Stream 요소의 수형
 * @param K  Map 의 Key 수형
 * @param V  Map의 Value 수형
 * @param mapper Stream 요소를 Key와 Value로 변환하는 함수
 * @receiver Stream
 * @return Map<K, V> 인스턴스
 */
inline fun <T, K, V> Stream<T>.toMap(crossinline mapper: (item: T) -> Pair<K, V>): Map<K, V> =
    this.map { mapper(it) }.toList().toMap()

/**
 * [Stream]을 [MutableMap] 으로 변환합니다.
 *
 * ```
 * val stream = Stream.of(1, 2, 3, 4, 5)
 * val map = stream.toMutableMap { it to it * 2 } // {1=2, 2=4, 3=6, 4=8, 5=10}
 * ```
 *
 * @param T  Stream 요소의 수형
 * @param K  Map 의 Key 수형
 * @param V  Map의 Value 수형
 * @param mapper Stream 요소를 Key와 Value로 변환하는 함수
 * @receiver Stream
 * @return Map<K, V> 인스턴스
 */
inline fun <T, K, V> Stream<T>.toMutableMap(crossinline mapper: (item: T) -> Pair<K, V>): MutableMap<K, V> =
    this.toMap(mapper).toMutableMap()

/**
 * [Iterable]을 [Stream]으로 변환합니다.
 */
fun <T> Iterator<T>.asStream(): Stream<T> = StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, 0), false)

/**
 * [Iterable]을 [Stream]으로 변환합니다.
 */
fun <T> Iterable<T>.asStream(): Stream<T> = iterator().asStream()

/**
 * [Sequence]을 [Stream]으로 변환합니다.
 */
fun <T> Sequence<T>.asStream(): Stream<T> = iterator().asStream()

/**
 * [Iterator]를 Parallel [Stream]으로 변환합니다.
 */
fun <T> Iterator<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [Iterable]를 Parallel [Stream]으로 변환합니다.
 */
fun <T> Iterable<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [Sequence]를 Parallel [Stream]으로 변환합니다.
 */
fun <T> Sequence<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [IntStream]를 [Sequence]로 변환합니다.
 */
fun IntStream.asSequence(): Sequence<Int> = Sequence { iterator() }

/**
 * [IntStream]를 [Iterable]로 변환합니다.
 */
fun IntStream.asIterable(): Iterable<Int> = Iterable { iterator() }

/**
 * [IntStream]를 [List]로 변환합니다.
 */
fun IntStream.toList(): List<Int> = asSequence().toList()

/**
 * [IntStream]를 [IntArray]로 변환합니다.
 */
fun IntStream.toIntArray(): IntArray = asSequence().asIntArray()

/**
 * [Sequence]를 [IntStream]로 변환합니다.
 */
fun Sequence<Int>.toIntStream(): IntStream = asStream().mapToInt { it }

/**
 * [Iterable]를 [IntStream]로 변환합니다.
 */
fun Iterable<Int>.toIntStream(): IntStream = asStream().mapToInt { it }

/**
 * [IntArray]를 [IntStream]로 변환합니다.
 */
fun IntArray.toIntStream(): IntStream = Arrays.stream(this)

/**
 * [LongStream]를 [Sequence]로 변환합니다.
 */
fun LongStream.asSequence(): Sequence<Long> = Sequence { iterator() }

/**
 * [LongStream]를 [Iterable]로 변환합니다.
 */
fun LongStream.asIterable(): Iterable<Long> = Iterable { iterator() }

/**
 * [LongStream]를 [List]로 변환합니다.
 */
fun LongStream.toList(): List<Long> = asSequence().toList()

/**
 * [LongStream]를 [LongArray]로 변환합니다.
 */
fun LongStream.toLongArray(): LongArray = asSequence().asLongArray()

/**
 * [Sequence]를 [LongStream]로 변환합니다.
 */
fun Sequence<Long>.toLongStream(): LongStream = asStream().mapToLong { it }

/**
 * [Iterable]를 [LongStream]로 변환합니다.
 */
fun Iterable<Long>.toLongStream(): LongStream = asStream().mapToLong { it }

/**
 * [LongArray]를 [LongStream]로 변환합니다.
 */
fun LongArray.toLongStream(): LongStream = Arrays.stream(this)

/**
 * [DoubleStream]를 [Sequence]로 변환합니다.
 */
fun DoubleStream.asSequence(): Sequence<Double> = Sequence { iterator() }

/**
 * [DoubleStream]를 [Iterable]로 변환합니다.
 */
fun DoubleStream.asIterable(): Iterable<Double> = Iterable { iterator() }

/**
 * [DoubleStream]를 [List]로 변환합니다.
 */
fun DoubleStream.toList(): List<Double> = asSequence().toList()

/**
 * [DoubleStream]를 [DoubleArray]로 변환합니다.
 */
fun DoubleStream.toDoubleArray(): DoubleArray = asSequence().asDoubleArray()

/**
 * Double [Sequence]를 [DoubleStream]로 변환합니다.
 */
fun Sequence<Double>.toDoubleStream(): DoubleStream = asStream().mapToDouble { it }

/**
 * Double [Iterable]를 [DoubleStream]로 변환합니다.
 */
fun Iterable<Double>.toDoubleStream(): DoubleStream = asStream().mapToDouble { it }

/**
 * [DoubleArray]를 [DoubleStream]로 변환합니다.
 */
fun DoubleArray.toDoubleStream(): DoubleStream = Arrays.stream(this)

/**
 * [FloatArray]를 [DoubleStream]으로 변환합니다.
 */
fun FloatArray.toDoubleStream(): DoubleStream {
    return DoubleStream.builder()
        .also { builder ->
            forEach { builder.add(it.toDouble()) }
        }
        .build()
}
