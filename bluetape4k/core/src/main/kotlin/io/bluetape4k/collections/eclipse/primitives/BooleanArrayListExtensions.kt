package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.BooleanIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList

/**
 * [BooleanArray]를 [BooleanArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = booleanArrayOf(true, false).toBooleanArrayList()
 * // list.size() == 2
 * // list[0]
 * ```
 */
fun BooleanArray.toBooleanArrayList(): BooleanArrayList =
    BooleanArrayList.newListWith(*this)

/**
 * [Iterable]의 Boolean 요소를 [BooleanArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = listOf(true, false).toBooleanArrayList()
 * // list.size() == 2
 * // !list[1]
 * ```
 */
fun Iterable<Boolean>.toBooleanArrayList(): BooleanArrayList =
    when (this) {
        is Collection<Boolean> -> BooleanArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                   -> BooleanArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Boolean 요소를 [BooleanArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = sequenceOf(true, false).toBooleanArrayList()
 * // list.size() == 2
 * // list[0]
 * ```
 */
fun Sequence<Boolean>.toBooleanArrayList(): BooleanArrayList = asIterable().toBooleanArrayList()

/**
 * [builder]를 호출해 [BooleanArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = booleanArrayList(3) { it % 2 == 0 }
 * // list.size() == 3
 * // list[0] && !list[1]
 * ```
 */
inline fun booleanArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Boolean,
): BooleanArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return BooleanArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Boolean을 [BooleanArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true, false)
 * // list.size() == 2
 * // !list[1]
 * ```
 */
fun booleanArrayListOf(vararg elements: Boolean): BooleanArrayList =
    BooleanArrayList.newListWith(*elements)

/**
 * [BooleanIterable]을 [BooleanArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [BooleanArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true).toBooleanArrayList()
 * // list.size() == 1
 * // list[0]
 * ```
 */
fun BooleanIterable.toBooleanArrayList(): BooleanArrayList = when (this) {
    is BooleanArrayList -> this
    else -> BooleanArrayList.newList(this)
}

/**
 * [BooleanIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `booleanIterator()`를 그대로 위임합니다.
 *
 * ```kotlin
 * val iter = booleanArrayListOf(true).asIterator()
 * // iter.hasNext()
 * // iter.next()
 * ```
 */
fun BooleanIterable.asIterator(): Iterator<Boolean> = object: Iterator<Boolean> {
    private val iter = booleanIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Boolean = iter.next()
}

/**
 * [BooleanIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 *
 * ```kotlin
 * val seq = booleanArrayListOf(true, false).asSequence()
 * // seq.count() == 2
 * // seq.first()
 * ```
 */
fun BooleanIterable.asSequence(): Sequence<Boolean> = sequence {
    val iter = booleanIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [BooleanIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 *
 * ```kotlin
 * val it = booleanArrayListOf(true, false).asIterable()
 * // it.count() == 2
 * // it.first()
 * ```
 */
fun BooleanIterable.asIterable(): Iterable<Boolean> = Iterable { asIterator() }

/**
 * [BooleanIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true, false).asList()
 * // list == listOf(true, false)
 * // list.size == 2
 * ```
 */
fun BooleanIterable.asList() = asIterable().toList()

/**
 * [BooleanIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true, false).asMutableList()
 * // list.size == 2
 * // !list[1]
 * ```
 */
fun BooleanIterable.asMutableList() = asIterable().toMutableList()

/**
 * [BooleanIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 *
 * ```kotlin
 * val set = booleanArrayListOf(true, true, false).asSet()
 * // set.size == 2
 * // true in set
 * ```
 */
fun BooleanIterable.asSet() = asIterable().toSet()

/**
 * [BooleanIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 *
 * ```kotlin
 * val set = booleanArrayListOf(true, true, false).asMutableSet()
 * // set.size == 2
 * // false in set
 * ```
 */
fun BooleanIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [BooleanIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true, false).toFastList()
 * // list.size == 2
 * // list[0]
 * ```
 */
fun BooleanIterable.toFastList() = asIterable().toFastList()

/**
 * [BooleanIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 *
 * ```kotlin
 * val set = booleanArrayListOf(true, true, false).toUnifiedSet()
 * // set.size == 2
 * // set.contains(true)
 * ```
 */
fun BooleanIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [BooleanIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 *
 * ```kotlin
 * val list = booleanArrayListOf(true, false).toFixedSizeList()
 * // list.size == 2
 * // !list[1]
 * ```
 */
fun BooleanIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [BooleanArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = booleanArrayOf(true, false).toFastList()
 * // list.size == 2
 * // !list[1]
 * ```
 */
fun BooleanArray.toFastList(): FastList<Boolean> = asIterable().toFastList()
