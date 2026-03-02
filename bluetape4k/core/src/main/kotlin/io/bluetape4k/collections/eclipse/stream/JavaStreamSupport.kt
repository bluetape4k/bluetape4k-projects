package io.bluetape4k.collections.eclipse.stream

import io.bluetape4k.collections.asIterable
import io.bluetape4k.collections.eclipse.primitives.toDoubleArrayList
import io.bluetape4k.collections.eclipse.primitives.toFloatArrayList
import io.bluetape4k.collections.eclipse.primitives.toIntArrayList
import io.bluetape4k.collections.eclipse.primitives.toLongArrayList
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

/**
 * Java [Stream]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 수신 스트림은 재사용할 수 없습니다.
 * - 새 [FastList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = Stream.of("a", "b").toFastList()
 * // list.size == 2
 * // list[0] == "a"
 * ```
 */
fun <T> Stream<T>.toFastList(): FastList<T> = asIterable().toFastList()

/**
 * [IntStream]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [FastList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = IntStream.of(1, 2).toFastList()
 * // list.size == 2
 * // list[1] == 2
 * ```
 */
fun IntStream.toFastList(): FastList<Int> = asIterable().toFastList()

/**
 * [LongStream]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [FastList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = LongStream.of(1L, 2L).toFastList()
 * // list.size == 2
 * // list[0] == 1L
 * ```
 */
fun LongStream.toFastList(): FastList<Long> = asIterable().toFastList()

/**
 * [DoubleStream]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [FastList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = DoubleStream.of(1.0, 2.0).toFastList()
 * // list.size == 2
 * // list[1] == 2.0
 * ```
 */
fun DoubleStream.toFastList(): FastList<Double> = asIterable().toFastList()

/**
 * Java [Stream]을 [UnifiedSet]으로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 set을 allocate 하며 중복 요소는 제거됩니다.
 *
 * ```kotlin
 * val set = Stream.of("a", "a", "b").toUnifiedSet()
 * // set.size == 2
 * // set.contains("a")
 * ```
 */
fun <T> Stream<T>.toUnifiedSet(): UnifiedSet<T> = asIterable().toUnifiedSet()

/**
 * [IntStream]을 [UnifiedSet]으로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 set을 allocate 하며 중복 요소는 제거됩니다.
 *
 * ```kotlin
 * val set = IntStream.of(1, 1, 2).toUnifiedSet()
 * // set.size == 2
 * // set.contains(2)
 * ```
 */
fun IntStream.toUnifiedSet(): UnifiedSet<Int> = asIterable().toUnifiedSet()

/**
 * [LongStream]을 [UnifiedSet]으로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 set을 allocate 하며 중복 요소는 제거됩니다.
 *
 * ```kotlin
 * val set = LongStream.of(1L, 1L, 2L).toUnifiedSet()
 * // set.size == 2
 * // set.contains(1L)
 * ```
 */
fun LongStream.toUnifiedSet(): UnifiedSet<Long> = asIterable().toUnifiedSet()

/**
 * [DoubleStream]을 [UnifiedSet]으로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 set을 allocate 하며 중복 요소는 제거됩니다.
 *
 * ```kotlin
 * val set = DoubleStream.of(1.0, 1.0, 2.0).toUnifiedSet()
 * // set.size == 2
 * // set.contains(2.0)
 * ```
 */
fun DoubleStream.toUnifiedSet(): UnifiedSet<Double> = asIterable().toUnifiedSet()

/**
 * [IntStream]을 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [IntArrayList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = IntStream.of(1, 2).toIntArrayList()
 * // list.size() == 2
 * // list[0] == 1
 * ```
 */
fun IntStream.toIntArrayList(): IntArrayList = asIterable().toIntArrayList()

/**
 * [LongStream]을 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [LongArrayList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = LongStream.of(1L, 2L).toLongArrayList()
 * // list.size() == 2
 * // list[1] == 2L
 * ```
 */
fun LongStream.toLongArrayList(): LongArrayList = asIterable().toLongArrayList()

/**
 * [DoubleStream]을 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 각 값을 `toFloat()`로 변환합니다.
 * - 새 [FloatArrayList]를 allocate 하며 요소 순서를 유지합니다.
 * - double -> float 변환에서 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * val list = DoubleStream.of(1.5, 2.5).toFloatArrayList()
 * // list.size() == 2
 * // list[0] == 1.5f
 * ```
 */
fun DoubleStream.toFloatArrayList(): FloatArrayList = asIterable().map { it.toFloat() }.toFloatArrayList()

/**
 * [DoubleStream]을 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 스트림을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 [DoubleArrayList]를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = DoubleStream.of(1.0, 2.0).toDoubleArrayList()
 * // list.size() == 2
 * // list[1] == 2.0
 * ```
 */
fun DoubleStream.toDoubleArrayList(): DoubleArrayList = asIterable().toDoubleArrayList()

/**
 * [IntArrayList]를 [IntStream]으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트는 mutate 하지 않고 새 스트림을 allocate 합니다.
 * - 리스트 요소를 순서대로 builder에 추가해 스트림을 생성합니다.
 *
 * ```kotlin
 * val stream = intArrayListOf(1, 2).toIntStream()
 * // stream.sum() == 3
 * // IntStream.of(1).count() == 1L
 * ```
 */
fun IntArrayList.toIntStream(): IntStream =
    IntStream.builder()
        .apply {
            this@toIntStream.forEach { accept(it) }
        }
        .build()

/**
 * [LongArrayList]를 [LongStream]으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트는 mutate 하지 않고 새 스트림을 allocate 합니다.
 * - 리스트 요소를 순서대로 builder에 추가해 스트림을 생성합니다.
 *
 * ```kotlin
 * val stream = longArrayListOf(1L, 2L).toLongStream()
 * // stream.sum() == 3L
 * // LongStream.of(1L).count() == 1L
 * ```
 */
fun LongArrayList.toLongStream(): LongStream =
    LongStream.builder()
        .also { builder ->
            this@toLongStream.forEach { builder.accept(it) }
        }
        .build()

/**
 * [FloatArrayList]를 [DoubleStream]으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트는 mutate 하지 않고 새 스트림을 allocate 합니다.
 * - 각 float 값을 `toDouble()`로 승격해 스트림에 추가합니다.
 *
 * ```kotlin
 * val stream = floatArrayListOf(1.5f, 2.5f).toDoubleStream()
 * // stream.sum() == 4.0
 * // DoubleStream.of(1.0).count() == 1L
 * ```
 */
fun FloatArrayList.toDoubleStream(): DoubleStream =
    DoubleStream.builder()
        .also { builder ->
            this@toDoubleStream.forEach { builder.accept(it.toDouble()) }
        }
        .build()


/**
 * [DoubleArrayList]를 [DoubleStream]으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 리스트는 mutate 하지 않고 새 스트림을 allocate 합니다.
 * - 리스트 요소를 순서대로 builder에 추가해 스트림을 생성합니다.
 *
 * ```kotlin
 * val stream = doubleArrayListOf(1.0, 2.0).toDoubleStream()
 * // stream.sum() == 3.0
 * // DoubleStream.of(1.0).count() == 1L
 * ```
 */
fun DoubleArrayList.toDoubleStream(): DoubleStream =
    DoubleStream.builder()
        .also { builder ->
            this@toDoubleStream.forEach { builder.accept(it) }
        }
        .build()
