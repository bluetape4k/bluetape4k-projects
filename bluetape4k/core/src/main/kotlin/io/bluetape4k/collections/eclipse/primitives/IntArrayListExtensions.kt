package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asInt
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.IntIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList

/**
 * [IntArray]를 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [IntArrayList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = intArrayOf(1, 2, 3).toIntArrayList()
 * // list.size() == 3
 * // list[0] == 1
 * ```
 */
fun IntArray.toIntArrayList(): IntArrayList = IntArrayList.newListWith(*this)

/**
 * [Iterable]의 Int 요소를 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3).toIntArrayList()
 * // list.size() == 3
 * // list[1] == 2
 * ```
 */
fun Iterable<Int>.toIntArrayList(): IntArrayList =
    when (this) {
        is Collection<Int> -> IntArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else               -> IntArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Int 요소를 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 시퀀스 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf(1, 2, 3).toIntArrayList()
 * // list.size() == 3
 * // list[2] == 3
 * ```
 */
fun Sequence<Int>.toIntArrayList(): IntArrayList = asIterable().toIntArrayList()

/**
 * [Iterable] 숫자 요소를 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asInt] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 변환 과정에서 숫자 범위/포맷 이슈는 [asInt] 동작을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2L, 3.0).asIntArrayList()
 * // list.size() == 3
 * // list[1] == 2
 * ```
 */
fun Iterable<Number>.asIntArrayList() = when (this) {
    is Collection<Number> -> IntArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asInt()) }
    }
    else                  -> IntArrayList().also { array ->
        forEach { number -> array.add(number.asInt()) }
    }
}

/**
 * [builder]를 호출해 [IntArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며, 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = intArrayList(3) { it + 1 }
 * // list.size() == 3
 * // list[0] == 1
 * ```
 *
 * @param initialCapacity 생성할 요소 개수이자 초기 용량
 * @param builder 인덱스별 값 생성 함수
 */
inline fun intArrayList(
    initialCapacity: Int = 10,
    builder: (index: Int) -> Int,
): IntArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return IntArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Int를 [IntArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2, 3)
 * // list.size() == 3
 * // list[2] == 3
 * ```
 */
fun intArrayListOf(vararg elements: Int): IntArrayList =
    IntArrayList.newListWith(*elements)

/**
 * [IntIterable]을 [IntArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [IntArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2).toIntArrayList()
 * // list.size() == 2
 * // list[1] == 2
 * ```
 */
fun IntIterable.toIntArrayList(): IntArrayList = when (this) {
    is IntArrayList -> this
    else -> IntArrayList.newList(this)
}

/**
 * [IntIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `intIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = intArrayListOf(1, 2).asIterator()
 * // iter.hasNext()
 * // iter.next() == 1
 * ```
 */
fun IntIterable.asIterator(): Iterator<Int> = object: Iterator<Int> {
    private val iter = intIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Int = iter.next()
}

/**
 * [IntIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 시퀀스 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence를 여러 번 소비하면 매번 새 iterator를 사용합니다.
 *
 * ```kotlin
 * val seq = intArrayListOf(1, 2).asSequence()
 * // seq.sum() == 3
 * // seq.count() == 2
 * ```
 */
fun IntIterable.asSequence(): Sequence<Int> = sequence {
    val iter = intIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [IntIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = intArrayListOf(1, 2).asIterable()
 * // iterable.count() == 2
 * // iterable.first() == 1
 * ```
 */
fun IntIterable.asIterable() = Iterable { asIterator() }

/**
 * [IntIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2).asList()
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun IntIterable.asList() = asIterable().toList()

/**
 * [IntIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2).asMutableList()
 * // list.size == 2
 * // list[0] == 1
 * ```
 */
fun IntIterable.asMutableList() = asIterable().toMutableList()

/**
 * [IntIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = intArrayListOf(1, 1, 2).asSet()
 * // set.size == 2
 * // 1 in set
 * ```
 */
fun IntIterable.asSet() = asIterable().toSet()

/**
 * [IntIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = intArrayListOf(1, 1, 2).asMutableSet()
 * // set.size == 2
 * // 2 in set
 * ```
 */
fun IntIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [IntIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2).toFastList()
 * // list.size == 2
 * // list[0] == 1
 * ```
 */
fun IntIterable.toFastList() = asIterable().toFastList()

/**
 * [IntIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = intArrayListOf(1, 1, 2).toUnifiedSet()
 * // set.size == 2
 * // set.contains(1)
 * ```
 */
fun IntIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [IntIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = intArrayListOf(1, 2).toFixedSizeList()
 * // list.size == 2
 * // list[1] == 2
 * ```
 */
fun IntIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [IntIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [IntIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // intArrayListOf(1, 3).maxOrNull() == 3
 * // intArrayListOf().maxOrNull() == null
 * ```
 */
fun IntIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [IntIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [IntIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // intArrayListOf(1, 3).minOrNull() == 1
 * // intArrayListOf().minOrNull() == null
 * ```
 */
fun IntIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [IntIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 큰 값에서 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * // intArrayListOf(2, 3, 4).product() == 24.0
 * // intArrayListOf().product() == 1.0
 * ```
 */
fun IntIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [IntArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = intArrayOf(1, 2).toFastList()
 * // list.size == 2
 * // list[1] == 2
 * ```
 */
fun IntArray.toFastList(): FastList<Int> = asIterable().toFastList()

//
