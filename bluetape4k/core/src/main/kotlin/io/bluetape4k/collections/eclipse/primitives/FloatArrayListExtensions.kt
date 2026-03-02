package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asFloat
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.FloatIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList

/**
 * [FloatArray]를 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = floatArrayOf(1, 2).toFloatArrayList()
 * // list.size() == 2
 * // list[0] == 1.0f
 * ```
 */
fun FloatArray.toFloatArrayList(): FloatArrayList =
    FloatArrayList.newListWith(*this)

/**
 * [Iterable]의 Float 요소를 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf<Float>(1, 2).toFloatArrayList()
 * // list.size() == 2
 * // list[1] == 2.0f
 * ```
 */
fun Iterable<Float>.toFloatArrayList(): FloatArrayList =
    when (this) {
        is Collection<Float> -> FloatArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else -> FloatArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Float 요소를 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 sequence 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf<Float>(1, 2).toFloatArrayList()
 * // list.size() == 2
 * // list[0] == 1.0f
 * ```
 */
fun Sequence<Float>.toFloatArrayList(): FloatArrayList = asIterable().toFloatArrayList()

/**
 * [Iterable] 숫자 요소를 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asFloat] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 값 범위/포맷 이슈는 [asFloat] 변환 규칙을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2.0f, 3.0).asFloatArrayList()
 * // list.size() == 3
 * // list[2] == 3.0f
 * ```
 */
fun Iterable<Number>.asFloatArrayList() = when (this) {
    is Collection<Number> -> FloatArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asFloat()) }
    }
    else                  -> FloatArrayList().also { array ->
        forEach { number -> array.add(number.asFloat()) }
    }
}

/**
 * [builder]를 호출해 [FloatArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = floatArrayList(2) { (it + 1).toFloat() }
 * // list.size() == 2
 * // list[0] == 1.0f
 * ```
 */
inline fun floatArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Float,
): FloatArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return FloatArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Float를 [FloatArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2)
 * // list.size() == 2
 * // list[1] == 2.0f
 * ```
 */
fun floatArrayListOf(vararg elements: Float): FloatArrayList =
    FloatArrayList.newListWith(*elements)

/**
 * [FloatIterable]을 [FloatArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [FloatArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2).toFloatArrayList()
 * // list.size() == 2
 * // list[0] == 1.0f
 * ```
 */
fun FloatIterable.toFloatArrayList(): FloatArrayList = when (this) {
    is FloatArrayList -> this
    else -> FloatArrayList.newList(this)
}

/**
 * [FloatIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `floatIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = floatArrayListOf(1, 2).asIterator()
 * // iter.hasNext()
 * // iter.next() == 1.0f
 * ```
 */
fun FloatIterable.asIterator(): Iterator<Float> = object: Iterator<Float> {
    private val iter = floatIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Float = iter.next()
}

/**
 * [FloatIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence 재소비 시 새 iterator가 생성됩니다.
 *
 * ```kotlin
 * val seq = floatArrayListOf(1, 2).asSequence()
 * // seq.count() == 2
 * // seq.first() == 1.0f
 * ```
 */
fun FloatIterable.asSequence(): Sequence<Float> = sequence {
    val iter = floatIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [FloatIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = floatArrayListOf(1, 2).asIterable()
 * // iterable.count() == 2
 * // iterable.first() == 1.0f
 * ```
 */
fun FloatIterable.asIterable(): Iterable<Float> = Iterable { asIterator() }

/**
 * [FloatIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2).asList()
 * // list == listOf<Float>(1, 2)
 * // list.size == 2
 * ```
 */
fun FloatIterable.asList() = asIterable().toList()

/**
 * [FloatIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2).asMutableList()
 * // list.size == 2
 * // list[1] == 2.0f
 * ```
 */
fun FloatIterable.asMutableList() = asIterable().toMutableList()

/**
 * [FloatIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = floatArrayListOf(1, 1, 2).asSet()
 * // set.size == 2
 * // 1.0f in set
 * ```
 */
fun FloatIterable.asSet() = asIterable().toSet()

/**
 * [FloatIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = floatArrayListOf(1, 1, 2).asMutableSet()
 * // set.size == 2
 * // 2.0f in set
 * ```
 */
fun FloatIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [FloatIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2).toFastList()
 * // list.size == 2
 * // list[0] == 1.0f
 * ```
 */
fun FloatIterable.toFastList() = asIterable().toFastList()

/**
 * [FloatIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = floatArrayListOf(1, 1, 2).toUnifiedSet()
 * // set.size == 2
 * // set.contains(1.0f)
 * ```
 */
fun FloatIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [FloatIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = floatArrayListOf(1, 2).toFixedSizeList()
 * // list.size == 2
 * // list[1] == 2.0f
 * ```
 */
fun FloatIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [FloatIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [FloatIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // floatArrayListOf(1, 3).maxOrNull() == 3.0f
 * // floatArrayListOf().maxOrNull() == null
 * ```
 */
fun FloatIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [FloatIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [FloatIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * // floatArrayListOf(1, 3).minOrNull() == 1.0f
 * // floatArrayListOf().minOrNull() == null
 * ```
 */
fun FloatIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [FloatIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * // floatArrayListOf(2, 3).product() == 6.0
 * // floatArrayListOf().product() == 1.0
 * ```
 */
fun FloatIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [FloatArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = floatArrayOf(1, 2).toFastList()
 * // list.size == 2
 * // list[1] == 2.0f
 * ```
 */
fun FloatArray.toFastList(): FastList<Float> = asIterable().toFastList()
