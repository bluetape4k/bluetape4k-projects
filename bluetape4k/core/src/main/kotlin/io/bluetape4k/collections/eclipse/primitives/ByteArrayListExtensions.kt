package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asByte
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.ByteIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList

/**
 * [ByteArray]를 [ByteArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = byteArrayOf(1, 2).toByteArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1.toByte())
 * ```
 */
fun ByteArray.toByteArrayList(): ByteArrayList =
    ByteArrayList.newListWith(*this)

/**
 * [Iterable]의 Byte 요소를 [ByteArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf<Byte>(1, 2).toByteArrayList()
 * check(list.size() == 2)
 * check(list[1] == 2.toByte())
 * ```
 */
fun Iterable<Byte>.toByteArrayList(): ByteArrayList =
    when (this) {
        is Collection<Byte> -> ByteArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                -> ByteArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Byte 요소를 [ByteArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 sequence 순회를 따릅니다.
 *
 * ```kotlin
 * val list = sequenceOf<Byte>(1, 2).toByteArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1.toByte())
 * ```
 */
fun Sequence<Byte>.toByteArrayList(): ByteArrayList = asIterable().toByteArrayList()

/**
 * [Iterable] 숫자 요소를 [ByteArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asByte] 규칙으로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 값 범위/포맷 이슈는 [asByte] 변환 규칙을 따릅니다.
 *
 * ```kotlin
 * val list = listOf(1, 2L, 3.0).asByteArrayList()
 * check(list.size() == 3)
 * check(list[2] == 3.toByte())
 * ```
 */
fun Iterable<Number>.asByteArrayList() = when (this) {
    is Collection<Number> -> ByteArrayList(this.size).also { array ->
        forEach { number -> array.add(number.asByte()) }
    }
    else                  -> ByteArrayList().also { array ->
        forEach { number -> array.add(number.asByte()) }
    }
}

/**
 * [builder]를 호출해 [ByteArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 하며 수신 객체 mutate는 없습니다.
 *
 * ```kotlin
 * val list = byteArrayList(2) { (it + 1).toByte() }
 * check(list.size() == 2)
 * check(list[0] == 1.toByte())
 * ```
 */
inline fun byteArrayList(
    initialCapacity: Int = 10,
    @BuilderInference builder: (index: Int) -> Byte,
): ByteArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return ByteArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Byte를 [ByteArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 수신 배열(vararg backing array)은 mutate 하지 않습니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2)
 * check(list.size() == 2)
 * check(list[1] == 2.toByte())
 * ```
 */
fun byteArrayListOf(vararg elements: Byte): ByteArrayList =
    ByteArrayList.newListWith(*elements)

/**
 * [ByteIterable]을 [ByteArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [ByteArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2).toByteArrayList()
 * check(list.size() == 2)
 * check(list[0] == 1.toByte())
 * ```
 */
fun ByteIterable.toByteArrayList(): ByteArrayList = when (this) {
    is ByteArrayList -> this
    else -> ByteArrayList.newList(this)
}

/**
 * [ByteIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `byteIterator()`를 그대로 위임합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iter = byteArrayListOf(1, 2).asIterator()
 * check(iter.hasNext())
 * check(iter.next() == 1.toByte())
 * ```
 */
fun ByteIterable.asIterator(): Iterator<Byte> = object: Iterator<Byte> {
    private val iter = byteIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Byte = iter.next()
}

/**
 * [ByteIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 * - sequence 재소비 시 새 iterator가 생성됩니다.
 *
 * ```kotlin
 * val seq = byteArrayListOf(1, 2).asSequence()
 * check(seq.count() == 2)
 * check(seq.first() == 1.toByte())
 * ```
 */
fun ByteIterable.asSequence(): Sequence<Byte> = sequence {
    val iter = byteIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [ByteIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val iterable = byteArrayListOf(1, 2).asIterable()
 * check(iterable.count() == 2)
 * check(iterable.first() == 1.toByte())
 * ```
 */
fun ByteIterable.asIterable(): Iterable<Byte> = Iterable { asIterator() }

/**
 * [ByteIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 [List]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2).asList()
 * check(list == listOf<Byte>(1, 2))
 * check(list.size == 2)
 * ```
 */
fun ByteIterable.asList() = asIterable().toList()

/**
 * [ByteIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 따릅니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2).asMutableList()
 * check(list.size == 2)
 * check(list[1] == 2.toByte())
 * ```
 */
fun ByteIterable.asMutableList() = asIterable().toMutableList()

/**
 * [ByteIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = byteArrayListOf(1, 1, 2).asSet()
 * check(set.size == 2)
 * check(1.toByte() in set)
 * ```
 */
fun ByteIterable.asSet() = asIterable().toSet()

/**
 * [ByteIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 * - 순서는 set 구현체 특성을 따릅니다.
 *
 * ```kotlin
 * val set = byteArrayListOf(1, 1, 2).asMutableSet()
 * check(set.size == 2)
 * check(2.toByte() in set)
 * ```
 */
fun ByteIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [ByteIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2).toFastList()
 * check(list.size == 2)
 * check(list[0] == 1.toByte())
 * ```
 */
fun ByteIterable.toFastList() = asIterable().toFastList()

/**
 * [ByteIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 * - 반환 타입은 Eclipse Collections set 구현입니다.
 *
 * ```kotlin
 * val set = byteArrayListOf(1, 1, 2).toUnifiedSet()
 * check(set.size == 2)
 * check(set.contains(1.toByte()))
 * ```
 */
fun ByteIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [ByteIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 * - 반환 리스트는 크기 변경이 제한될 수 있습니다.
 *
 * ```kotlin
 * val list = byteArrayListOf(1, 2).toFixedSizeList()
 * check(list.size == 2)
 * check(list[1] == 2.toByte())
 * ```
 */
fun ByteIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [ByteIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [ByteIterable.max] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * check(byteArrayListOf(1, 3).maxOrNull() == 3.toByte())
 * check(byteArrayListOf().maxOrNull() == null)
 * ```
 */
fun ByteIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [ByteIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [ByteIterable.min] 값을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 *
 * ```kotlin
 * check(byteArrayListOf(1, 3).minOrNull() == 1.toByte())
 * check(byteArrayListOf().minOrNull() == null)
 * ```
 */
fun ByteIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [ByteIterable] 요소의 곱을 `Double`로 계산합니다.
 *
 * ## 동작/계약
 * - empty이면 곱셈 항등원인 `1.0`을 반환합니다.
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 누적 타입이 `Double`이므로 정밀도 손실이 발생할 수 있습니다.
 *
 * ```kotlin
 * check(byteArrayListOf(2, 3).product() == 6.0)
 * check(byteArrayListOf().product() == 1.0)
 * ```
 */
fun ByteIterable.product(): Double = asIterable().fold(1.0) { acc, i -> acc * i }

/**
 * [ByteArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 * - null 수신은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = byteArrayOf(1, 2).toFastList()
 * check(list.size == 2)
 * check(list[1] == 2.toByte())
 * ```
 */
fun ByteArray.toFastList(): FastList<Byte> = asIterable().toFastList()
