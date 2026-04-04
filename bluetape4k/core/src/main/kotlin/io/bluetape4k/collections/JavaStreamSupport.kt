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
 * [Stream]을 [Iterable]로 변환합니다.
 *
 * ```kotlin
 * val stream = Stream.of("a", "b", "c")
 * val iterable: Iterable<String> = stream.asIterable()
 * iterable.toList() // ["a", "b", "c"]
 * ```
 *
 * @receiver Stream
 * @return Iterable<T> 인스턴스
 */
fun <T> Stream<T>.asIterable(): Iterable<T> = Iterable { iterator() }

/**
 * [Stream]을 [Set]로 변환합니다.
 *
 * ```kotlin
 * val stream = Stream.of(1, 2, 2, 3, 3)
 * val set: Set<Int> = stream.toSet() // {1, 2, 3}
 * ```
 *
 * @receiver Stream
 * @return Set<T> 인스턴스
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
 * [Iterator]를 순차 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val iter = listOf(1, 2, 3).iterator()
 * val stream: Stream<Int> = iter.asStream()
 * stream.toList() // [1, 2, 3]
 * ```
 *
 * @receiver Iterator<T>
 * @return 순차 Stream<T> 인스턴스
 */
fun <T> Iterator<T>.asStream(): Stream<T> = StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, 0), false)

/**
 * [Iterable]을 순차 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val list = listOf("a", "b", "c")
 * val stream: Stream<String> = list.asStream()
 * stream.map { it.uppercase() }.toList() // ["A", "B", "C"]
 * ```
 *
 * @receiver Iterable<T>
 * @return 순차 Stream<T> 인스턴스
 */
fun <T> Iterable<T>.asStream(): Stream<T> = iterator().asStream()

/**
 * [Sequence]를 순차 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val seq = sequenceOf(10, 20, 30)
 * val stream: Stream<Int> = seq.asStream()
 * stream.filter { it > 10 }.toList() // [20, 30]
 * ```
 *
 * @receiver Sequence<T>
 * @return 순차 Stream<T> 인스턴스
 */
fun <T> Sequence<T>.asStream(): Stream<T> = iterator().asStream()

/**
 * [Iterator]를 병렬 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val iter = listOf(1, 2, 3, 4, 5).iterator()
 * val stream: Stream<Int> = iter.asParallelStream()
 * stream.isParallel // true
 * ```
 *
 * @receiver Iterator<T>
 * @return 병렬 Stream<T> 인스턴스
 */
fun <T> Iterator<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [Iterable]를 병렬 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3, 4, 5)
 * val sum = list.asParallelStream().mapToInt { it }.sum() // 15
 * ```
 *
 * @receiver Iterable<T>
 * @return 병렬 Stream<T> 인스턴스
 */
fun <T> Iterable<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [Sequence]를 병렬 [Stream]으로 변환합니다.
 *
 * ```kotlin
 * val seq = sequenceOf(1, 2, 3, 4, 5)
 * val stream: Stream<Int> = seq.asParallelStream()
 * stream.isParallel // true
 * ```
 *
 * @receiver Sequence<T>
 * @return 병렬 Stream<T> 인스턴스
 */
fun <T> Sequence<T>.asParallelStream(): Stream<T> = asStream().parallel()

/**
 * [IntStream]를 [Sequence]로 변환합니다.
 *
 * ```kotlin
 * val seq: Sequence<Int> = IntStream.range(1, 4).asSequence()
 * seq.toList() // [1, 2, 3]
 * ```
 *
 * @receiver IntStream
 * @return Sequence<Int> 인스턴스
 */
fun IntStream.asSequence(): Sequence<Int> = Sequence { iterator() }

/**
 * [IntStream]를 [Iterable]로 변환합니다.
 *
 * ```kotlin
 * val iterable: Iterable<Int> = IntStream.of(5, 10, 15).asIterable()
 * iterable.toList() // [5, 10, 15]
 * ```
 *
 * @receiver IntStream
 * @return Iterable<Int> 인스턴스
 */
fun IntStream.asIterable(): Iterable<Int> = Iterable { iterator() }

/**
 * [IntStream]를 [List]로 변환합니다.
 *
 * ```kotlin
 * val list: List<Int> = IntStream.rangeClosed(1, 5).toList() // [1, 2, 3, 4, 5]
 * ```
 *
 * @receiver IntStream
 * @return List<Int> 인스턴스
 */
fun IntStream.toList(): List<Int> = asSequence().toList()

/**
 * [IntStream]를 [IntArray]로 변환합니다.
 *
 * ```kotlin
 * val arr: IntArray = IntStream.of(1, 2, 3).toIntArray() // intArrayOf(1, 2, 3)
 * ```
 *
 * @receiver IntStream
 * @return IntArray 인스턴스
 */
fun IntStream.toIntArray(): IntArray = toArray() // asSequence().asIntArray()

/**
 * [Sequence]<[Int]>를 [IntStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: IntStream = sequenceOf(1, 2, 3).toIntStream()
 * stream.sum() // 6
 * ```
 *
 * @receiver Sequence<Int>
 * @return IntStream 인스턴스
 */
fun Sequence<Int>.toIntStream(): IntStream = asStream().mapToInt { it }

/**
 * [Iterable]<[Int]>를 [IntStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: IntStream = listOf(1, 2, 3).toIntStream()
 * stream.average().orElse(0.0) // 2.0
 * ```
 *
 * @receiver Iterable<Int>
 * @return IntStream 인스턴스
 */
fun Iterable<Int>.toIntStream(): IntStream = asStream().mapToInt { it }

/**
 * [IntArray]를 [IntStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: IntStream = intArrayOf(10, 20, 30).toIntStream()
 * stream.max().orElse(0) // 30
 * ```
 *
 * @receiver IntArray
 * @return IntStream 인스턴스
 */
fun IntArray.toIntStream(): IntStream = Arrays.stream(this)

/**
 * [LongStream]를 [Sequence]로 변환합니다.
 *
 * ```kotlin
 * val seq: Sequence<Long> = LongStream.range(1L, 4L).asSequence()
 * seq.toList() // [1L, 2L, 3L]
 * ```
 *
 * @receiver LongStream
 * @return Sequence<Long> 인스턴스
 */
fun LongStream.asSequence(): Sequence<Long> = Sequence { iterator() }

/**
 * [LongStream]를 [Iterable]로 변환합니다.
 *
 * ```kotlin
 * val iterable: Iterable<Long> = LongStream.of(100L, 200L, 300L).asIterable()
 * iterable.toList() // [100L, 200L, 300L]
 * ```
 *
 * @receiver LongStream
 * @return Iterable<Long> 인스턴스
 */
fun LongStream.asIterable(): Iterable<Long> = Iterable { iterator() }

/**
 * [LongStream]를 [List]로 변환합니다.
 *
 * ```kotlin
 * val list: List<Long> = LongStream.rangeClosed(1L, 3L).toList() // [1L, 2L, 3L]
 * ```
 *
 * @receiver LongStream
 * @return List<Long> 인스턴스
 */
fun LongStream.toList(): List<Long> = asSequence().toList()

/**
 * [LongStream]를 [LongArray]로 변환합니다.
 *
 * ```kotlin
 * val arr: LongArray = LongStream.of(1L, 2L, 3L).toLongArray() // longArrayOf(1L, 2L, 3L)
 * ```
 *
 * @receiver LongStream
 * @return LongArray 인스턴스
 */
fun LongStream.toLongArray(): LongArray = toArray() // asSequence().asLongArray()

/**
 * [Sequence]<[Long]>를 [LongStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: LongStream = sequenceOf(10L, 20L, 30L).toLongStream()
 * stream.sum() // 60L
 * ```
 *
 * @receiver Sequence<Long>
 * @return LongStream 인스턴스
 */
fun Sequence<Long>.toLongStream(): LongStream = asStream().mapToLong { it }

/**
 * [Iterable]<[Long]>를 [LongStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: LongStream = listOf(10L, 20L, 30L).toLongStream()
 * stream.max().orElse(0L) // 30L
 * ```
 *
 * @receiver Iterable<Long>
 * @return LongStream 인스턴스
 */
fun Iterable<Long>.toLongStream(): LongStream = asStream().mapToLong { it }

/**
 * [LongArray]를 [LongStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: LongStream = longArrayOf(100L, 200L, 300L).toLongStream()
 * stream.average().orElse(0.0) // 200.0
 * ```
 *
 * @receiver LongArray
 * @return LongStream 인스턴스
 */
fun LongArray.toLongStream(): LongStream = Arrays.stream(this)

/**
 * [DoubleStream]를 [Sequence]로 변환합니다.
 *
 * ```kotlin
 * val seq: Sequence<Double> = DoubleStream.of(1.0, 2.0, 3.0).asSequence()
 * seq.toList() // [1.0, 2.0, 3.0]
 * ```
 *
 * @receiver DoubleStream
 * @return Sequence<Double> 인스턴스
 */
fun DoubleStream.asSequence(): Sequence<Double> = Sequence { iterator() }

/**
 * [DoubleStream]를 [Iterable]로 변환합니다.
 *
 * ```kotlin
 * val iterable: Iterable<Double> = DoubleStream.of(0.1, 0.2, 0.3).asIterable()
 * iterable.toList() // [0.1, 0.2, 0.3]
 * ```
 *
 * @receiver DoubleStream
 * @return Iterable<Double> 인스턴스
 */
fun DoubleStream.asIterable(): Iterable<Double> = Iterable { iterator() }

/**
 * [DoubleStream]를 [List]로 변환합니다.
 *
 * ```kotlin
 * val list: List<Double> = DoubleStream.of(1.5, 2.5, 3.5).toList() // [1.5, 2.5, 3.5]
 * ```
 *
 * @receiver DoubleStream
 * @return List<Double> 인스턴스
 */
fun DoubleStream.toList(): List<Double> = asSequence().toList()

/**
 * [DoubleStream]를 [DoubleArray]로 변환합니다.
 *
 * ```kotlin
 * val arr: DoubleArray = DoubleStream.of(1.0, 2.0, 3.0).toDoubleArray() // doubleArrayOf(1.0, 2.0, 3.0)
 * ```
 *
 * @receiver DoubleStream
 * @return DoubleArray 인스턴스
 */
fun DoubleStream.toDoubleArray(): DoubleArray = toArray() // asSequence().asDoubleArray()

/**
 * [Sequence]<[Double]>를 [DoubleStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: DoubleStream = sequenceOf(1.0, 2.0, 3.0).toDoubleStream()
 * stream.sum() // 6.0
 * ```
 *
 * @receiver Sequence<Double>
 * @return DoubleStream 인스턴스
 */
fun Sequence<Double>.toDoubleStream(): DoubleStream = asStream().mapToDouble { it }

/**
 * [Iterable]<[Double]>를 [DoubleStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: DoubleStream = listOf(1.0, 2.0, 3.0).toDoubleStream()
 * stream.average().orElse(0.0) // 2.0
 * ```
 *
 * @receiver Iterable<Double>
 * @return DoubleStream 인스턴스
 */
fun Iterable<Double>.toDoubleStream(): DoubleStream = asStream().mapToDouble { it }

/**
 * [DoubleArray]를 [DoubleStream]로 변환합니다.
 *
 * ```kotlin
 * val stream: DoubleStream = doubleArrayOf(1.5, 2.5, 3.5).toDoubleStream()
 * stream.max().orElse(0.0) // 3.5
 * ```
 *
 * @receiver DoubleArray
 * @return DoubleStream 인스턴스
 */
fun DoubleArray.toDoubleStream(): DoubleStream = Arrays.stream(this)

/**
 * [FloatArray]를 [DoubleStream]으로 변환합니다.
 * 각 `Float` 요소를 `Double`로 변환하여 스트림에 추가합니다.
 *
 * ```kotlin
 * val stream: DoubleStream = floatArrayOf(1.0f, 2.5f, 3.0f).toDoubleStream()
 * stream.sum() // 6.5
 * ```
 *
 * @receiver FloatArray
 * @return DoubleStream 인스턴스
 */
fun FloatArray.toDoubleStream(): DoubleStream =
    DoubleStream.builder()
        .also { builder ->
            forEach { builder.add(it.toDouble()) }
        }
        .build()
