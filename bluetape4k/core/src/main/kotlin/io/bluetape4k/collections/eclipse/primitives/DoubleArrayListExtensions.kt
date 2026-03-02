package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asDouble
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.DoubleIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList

/**
 * [DoubleArray]를 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = doubleArrayOf(1, 2).toDoubleArrayList()
 * // list.size() == 2
 * // list[0] == 1.0
 * ```
 */
fun DoubleArray.toDoubleArrayList(): DoubleArrayList =
    DoubleArrayList.newListWith(*this)

/**
 * [Iterable]의 Double 요소를 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf<Double>(1, 2).toDoubleArrayList()
 * // list.size() == 2
 * // list[1] == 2.0
 * ```
 */
fun Iterable<Double>.toDoubleArrayList(): DoubleArrayList =
    when (this) {
        is Collection<Double> -> DoubleArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else -> DoubleArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Double 요소를 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 sequence 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf<Double>(1, 2).toDoubleArrayList()
 * // list.size() == 2
 * // list[0] == 1.0
 * ```
 */
fun Sequence<Double>.toDoubleArrayList(): DoubleArrayList = asIterable().toDoubleArrayList()

/**
 * [Iterable] 숫자 요소를 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asDouble] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 값 범위/포맷 이슈는 [asDouble] 변환 규칙을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2.0, 3.0).asDoubleArrayList()
 * // list.size() == 3
 * // list[2] == 3.0
 * ```
 */
fun Iterable<Number>.asDoubleArrayList() = when (this) {
    is Collection<Number> -> DoubleArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asDouble()) }
    }
    else                  -> DoubleArrayList().also { array ->
        forEach { number -> array.add(number.asDouble()) }
    }
}

/**
 * [builder]를 호출해 [DoubleArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = doubleArrayList(2) { (it + 1).toDouble() }
 * // list.size() == 2
 * // list[0] == 1.0
 * ```
 */
inline fun doubleArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Double,
): DoubleArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return DoubleArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Double를 [DoubleArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2)
 * // list.size() == 2
 * // list[1] == 2.0
 * ```
 */
fun doubleArrayListOf(vararg elements: Double): DoubleArrayList =
    DoubleArrayList.newListWith(*elements)

/**
 * [DoubleIterable]을 [DoubleArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [DoubleArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2).toDoubleArrayList()
 * // list.size() == 2
 * // list[0] == 1.0
 * ```
 */
fun DoubleIterable.toDoubleArrayList(): DoubleArrayList = when (this) {
    is DoubleArrayList -> this
    else -> DoubleArrayList.newList(this)
}

/**
 * [DoubleIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `doubleIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = doubleArrayListOf(1, 2).asIterator()
 * // iter.hasNext()
 * // iter.next() == 1.0
 * ```
 */
fun DoubleIterable.asIterator(): Iterator<Double> = object: Iterator<Double> {
    private val iter = doubleIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Double = iter.next()
}

/**
 * [DoubleIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence 재소비 시 새 iterator가 생성됩니다.
 *
 * ```kotlin
 * val seq = doubleArrayListOf(1, 2).asSequence()
 * // seq.count() == 2
 * // seq.first() == 1.0
 * ```
 */
fun DoubleIterable.asSequence(): Sequence<Double> = sequence {
    val iter = doubleIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [DoubleIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = doubleArrayListOf(1, 2).asIterable()
 * // iterable.count() == 2
 * // iterable.first() == 1.0
 * ```
 */
fun DoubleIterable.asIterable(): Iterable<Double> = Iterable { asIterator() }

/**
 * [DoubleIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2).asList()
 * // list == listOf<Double>(1, 2)
 * // list.size == 2
 * ```
 */
fun DoubleIterable.asList() = asIterable().toList()

/**
 * [DoubleIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2).asMutableList()
 * // list.size == 2
 * // list[1] == 2.0
 * ```
 */
fun DoubleIterable.asMutableList() = asIterable().toMutableList()

/**
 * [DoubleIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = doubleArrayListOf(1, 1, 2).asSet()
 * // set.size == 2
 * // 1.0 in set
 * ```
 */
fun DoubleIterable.asSet() = asIterable().toSet()

/**
 * [DoubleIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = doubleArrayListOf(1, 1, 2).asMutableSet()
 * // set.size == 2
 * // 2.0 in set
 * ```
 */
fun DoubleIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [DoubleIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2).toFastList()
 * // list.size == 2
 * // list[0] == 1.0
 * ```
 */
fun DoubleIterable.toFastList() = asIterable().toFastList()

/**
 * [DoubleIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = doubleArrayListOf(1, 1, 2).toUnifiedSet()
 * // set.size == 2
 * // set.contains(1.0)
 * ```
 */
fun DoubleIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [DoubleIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = doubleArrayListOf(1, 2).toFixedSizeList()
 * // list.size == 2
 * // list[1] == 2.0
 * ```
 */
fun DoubleIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [DoubleIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [DoubleIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // doubleArrayListOf(1, 3).maxOrNull() == 3.0
 * // doubleArrayListOf().maxOrNull() == null
 * ```
 */
fun DoubleIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [DoubleIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [DoubleIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // doubleArrayListOf(1, 3).minOrNull() == 1.0
 * // doubleArrayListOf().minOrNull() == null
 * ```
 */
fun DoubleIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [DoubleIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * // doubleArrayListOf(2, 3).product() == 6.0
 * // doubleArrayListOf().product() == 1.0
 * ```
 */
fun DoubleIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [DoubleArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = doubleArrayOf(1, 2).toFastList()
 * // list.size == 2
 * // list[1] == 2.0
 * ```
 */
fun DoubleArray.toFastList(): FastList<Double> = asIterable().toFastList()
