package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asLong
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.LongIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

/**
 * [LongArray]를 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = longArrayOf(1, 2).toLongArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1L)
 * ```
 */
fun LongArray.toLongArrayList(): LongArrayList =
    LongArrayList.newListWith(*this)

/**
 * [Iterable]의 Long 요소를 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf<Long>(1, 2).toLongArrayList()
 * check(list.size() == 2)
 * check(list[1] == 2L)
 * ```
 */
fun Iterable<Long>.toLongArrayList(): LongArrayList =
    when (this) {
        is Collection<Long> -> LongArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                -> LongArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Long 요소를 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 sequence 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf<Long>(1, 2).toLongArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1L)
 * ```
 */
fun Sequence<Long>.toLongArrayList(): LongArrayList = asIterable().toLongArrayList()

/**
 * [Iterable] 숫자 요소를 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asLong] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 값 범위/포맷 이슈는 [asLong] 변환 규칙을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2L, 3.0).asLongArrayList()
 * check(list.size() == 3)
 * check(list[2] == 3L)
 * ```
 */
fun Iterable<Number>.asLongArrayList() = when (this) {
    is Collection<Number> -> LongArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asLong()) }
    }
    else                  -> LongArrayList().also { array ->
        forEach { number -> array.add(number.asLong()) }
    }
}

/**
 * [builder]를 호출해 [LongArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = longArrayList(2) { (it + 1).toLong() }
 * check(list.size() == 2)
 * check(list[0] == 1L)
 * ```
 */
inline fun longArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Long,
): LongArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return LongArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Long를 [LongArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2)
 * check(list.size() == 2)
 * check(list[1] == 2L)
 * ```
 */
fun longArrayListOf(vararg elements: Long): LongArrayList =
    LongArrayList.newListWith(*elements)

/**
 * [LongIterable]을 [LongArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [LongArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2).toLongArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1L)
 * ```
 */
fun LongIterable.toLongArrayList(): LongArrayList = when (this) {
    is LongArrayList -> this
    else -> LongArrayList.newList(this)
}

/**
 * [LongIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `longIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = longArrayListOf(1, 2).asIterator()
 * check(iter.hasNext())
 * check(iter.next() == 1L)
 * ```
 */
fun LongIterable.asIterator(): Iterator<Long> = object: Iterator<Long> {
    private val iter = longIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Long = iter.next()
}

/**
 * [LongIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence 재소비 시 새 iterator가 생성됩니다.
 *
 * ```kotlin
 * val seq = longArrayListOf(1, 2).asSequence()
 * check(seq.count() == 2)
 * check(seq.first() == 1L)
 * ```
 */
fun LongIterable.asSequence(): Sequence<Long> = sequence {
    val iter = longIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [LongIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = longArrayListOf(1, 2).asIterable()
 * check(iterable.count() == 2)
 * check(iterable.first() == 1L)
 * ```
 */
fun LongIterable.asIterable(): Iterable<Long> = Iterable { asIterator() }

/**
 * [LongIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2).asList()
 * check(list == listOf<Long>(1, 2))
 * check(list.size == 2)
 * ```
 */
fun LongIterable.asList() = asIterable().toList()

/**
 * [LongIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2).asMutableList()
 * check(list.size == 2)
 * check(list[1] == 2L)
 * ```
 */
fun LongIterable.asMutableList() = asIterable().toMutableList()

/**
 * [LongIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = longArrayListOf(1, 1, 2).asSet()
 * check(set.size == 2)
 * check(1L in set)
 * ```
 */
fun LongIterable.asSet() = asIterable().toSet()

/**
 * [LongIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = longArrayListOf(1, 1, 2).asMutableSet()
 * check(set.size == 2)
 * check(2L in set)
 * ```
 */
fun LongIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [LongIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2).toFastList()
 * check(list.size == 2)
 * check(list[0] == 1L)
 * ```
 */
fun LongIterable.toFastList() = asIterable().toFastList()

/**
 * [LongIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = longArrayListOf(1, 1, 2).toUnifiedSet()
 * check(set.size == 2)
 * check(set.contains(1L))
 * ```
 */
fun LongIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [LongIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = longArrayListOf(1, 2).toFixedSizeList()
 * check(list.size == 2)
 * check(list[1] == 2L)
 * ```
 */
fun LongIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [LongIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [LongIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * check(longArrayListOf(1, 3).maxOrNull() == 3L)
 * check(longArrayListOf().maxOrNull() == null)
 * ```
 */
fun LongIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [LongIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [LongIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * check(longArrayListOf(1, 3).minOrNull() == 1L)
 * check(longArrayListOf().minOrNull() == null)
 * ```
 */
fun LongIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [LongIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * check(longArrayListOf(2, 3).product() == 6.0)
 * check(longArrayListOf().product() == 1.0)
 * ```
 */
fun LongIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [LongArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = longArrayOf(1, 2).toFastList()
 * check(list.size == 2)
 * check(list[1] == 2L)
 * ```
 */
fun LongArray.toFastList(): FastList<Long> = asIterable().toFastList()
