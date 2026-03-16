package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asShort
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.ShortIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList

/**
 * [ShortArray]를 [ShortArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = shortArrayOf(1, 2).toShortArrayList()
 * // list.size() == 2
 * // list[0] == 1.toShort()
 * ```
 */
fun ShortArray.toShortArrayList(): ShortArrayList =
    ShortArrayList.newListWith(*this)

/**
 * [Iterable]의 Short 요소를 [ShortArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf<Short>(1, 2).toShortArrayList()
 * // list.size() == 2
 * // list[1] == 2.toShort()
 * ```
 */
fun Iterable<Short>.toShortArrayList(): ShortArrayList =
    when (this) {
        is Collection<Short> -> ShortArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else -> ShortArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Short 요소를 [ShortArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 sequence 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf<Short>(1, 2).toShortArrayList()
 * // list.size() == 2
 * // list[0] == 1.toShort()
 * ```
 */
fun Sequence<Short>.toShortArrayList(): ShortArrayList = asIterable().toShortArrayList()

/**
 * [Iterable] 숫자 요소를 [ShortArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asShort] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 값 범위/포맷 이슈는 [asShort] 변환 규칙을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2.toShort(), 3.0).asShortArrayList()
 * // list.size() == 3
 * // list[2] == 3.toShort()
 * ```
 */
fun Iterable<Number>.asShortArrayList() = when (this) {
    is Collection<Number> -> ShortArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asShort()) }
    }
    else                  -> ShortArrayList().also { array ->
        forEach { number -> array.add(number.asShort()) }
    }
}

/**
 * [builder]를 호출해 [ShortArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = shortArrayList(2) { (it + 1).toShort() }
 * // list.size() == 2
 * // list[0] == 1.toShort()
 * ```
 */
inline fun shortArrayList(
    initialCapacity: Int = 10,
    builder: (index: Int) -> Short,
): ShortArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return ShortArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Short를 [ShortArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2)
 * // list.size() == 2
 * // list[1] == 2.toShort()
 * ```
 */
fun shortArrayListOf(vararg elements: Short): ShortArrayList =
    ShortArrayList.newListWith(*elements)

/**
 * [ShortIterable]을 [ShortArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [ShortArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2).toShortArrayList()
 * // list.size() == 2
 * // list[0] == 1.toShort()
 * ```
 */
fun ShortIterable.toShortArrayList(): ShortArrayList = when (this) {
    is ShortArrayList -> this
    else -> ShortArrayList.newList(this)
}

/**
 * [ShortIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `shortIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = shortArrayListOf(1, 2).asIterator()
 * // iter.hasNext()
 * // iter.next() == 1.toShort()
 * ```
 */
fun ShortIterable.asIterator(): Iterator<Short> = object: Iterator<Short> {
    private val iter = shortIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Short = iter.next()
}

/**
 * [ShortIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence 재소비 시 새 iterator가 생성됩니다.
 *
 * ```kotlin
 * val seq = shortArrayListOf(1, 2).asSequence()
 * // seq.count() == 2
 * // seq.first() == 1.toShort()
 * ```
 */
fun ShortIterable.asSequence(): Sequence<Short> = sequence {
    val iter = shortIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [ShortIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = shortArrayListOf(1, 2).asIterable()
 * // iterable.count() == 2
 * // iterable.first() == 1.toShort()
 * ```
 */
fun ShortIterable.asIterable(): Iterable<Short> = Iterable { asIterator() }

/**
 * [ShortIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2).asList()
 * // list == listOf<Short>(1, 2)
 * // list.size == 2
 * ```
 */
fun ShortIterable.asList() = asIterable().toList()

/**
 * [ShortIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2).asMutableList()
 * // list.size == 2
 * // list[1] == 2.toShort()
 * ```
 */
fun ShortIterable.asMutableList() = asIterable().toMutableList()

/**
 * [ShortIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = shortArrayListOf(1, 1, 2).asSet()
 * // set.size == 2
 * // 1.toShort() in set
 * ```
 */
fun ShortIterable.asSet() = asIterable().toSet()

/**
 * [ShortIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = shortArrayListOf(1, 1, 2).asMutableSet()
 * // set.size == 2
 * // 2.toShort() in set
 * ```
 */
fun ShortIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [ShortIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2).toFastList()
 * // list.size == 2
 * // list[0] == 1.toShort()
 * ```
 */
fun ShortIterable.toFastList() = asIterable().toFastList()

/**
 * [ShortIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = shortArrayListOf(1, 1, 2).toUnifiedSet()
 * // set.size == 2
 * // set.contains(1.toShort())
 * ```
 */
fun ShortIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [ShortIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = shortArrayListOf(1, 2).toFixedSizeList()
 * // list.size == 2
 * // list[1] == 2.toShort()
 * ```
 */
fun ShortIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [ShortIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [ShortIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // shortArrayListOf(1, 3).maxOrNull() == 3.toShort()
 * // shortArrayListOf().maxOrNull() == null
 * ```
 */
fun ShortIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [ShortIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [ShortIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // shortArrayListOf(1, 3).minOrNull() == 1.toShort()
 * // shortArrayListOf().minOrNull() == null
 * ```
 */
fun ShortIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [ShortIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * // shortArrayListOf(2, 3).product() == 6.0
 * // shortArrayListOf().product() == 1.0
 * ```
 */
fun ShortIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [ShortArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = shortArrayOf(1, 2).toFastList()
 * // list.size == 2
 * // list[1] == 2.toShort()
 * ```
 */
fun ShortArray.toFastList(): FastList<Short> = asIterable().toFastList()
