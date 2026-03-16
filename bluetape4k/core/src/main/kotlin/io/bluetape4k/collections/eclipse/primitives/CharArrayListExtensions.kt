package io.bluetape4k.collections.eclipse.primitives

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toFixedSizeList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.support.asChar
import io.bluetape4k.support.requireZeroOrPositiveNumber
import org.eclipse.collections.api.CharIterable
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList

/**
 * [CharArray]를 [CharArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = charArrayOf('a', 'b').toCharArrayList()
 * // list.size() == 2
 * // list[0] == 'a'
 * ```
 */
fun CharArray.toCharArrayList(): CharArrayList = CharArrayList.newListWith(*this)

/**
 * [Iterable]의 Char 요소를 [CharArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - `Collection`이면 크기 힌트로 초기 용량을 최적화합니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = listOf('a', 'b').toCharArrayList()
 * // list.size() == 2
 * // list[1] == 'b'
 * ```
 */
fun Iterable<Char>.toCharArrayList(): CharArrayList =
    when (this) {
        is Collection<Char> -> CharArrayList(this.size).also { array ->
            forEach { array.add(it) }
        }
        else                -> CharArrayList().also { array ->
            forEach { array.add(it) }
        }
    }

/**
 * [Sequence]의 Char 요소를 [CharArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 수신 sequence는 mutate 하지 않고 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = sequenceOf('a', 'b').toCharArrayList()
 * // list.size() == 2
 * // list[0] == 'a'
 * ```
 */
fun Sequence<Char>.toCharArrayList(): CharArrayList = asIterable().toCharArrayList()

/**
 * [Iterable] 요소를 [asChar] 규칙으로 [CharArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 각 요소는 [asChar]로 변환됩니다.
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = listOf<Any>('a', 66, "C").asCharArrayList()
 * // list.size() == 3
 * // list[1] == 'B'
 * ```
 */
fun Iterable<Any>.asCharArrayList(): CharArrayList = when (this) {
    is Collection<Any> -> CharArrayList(this.size).also { array ->
        forEach { value -> array.add(value.asChar()) }
    }
    else               -> CharArrayList().also { array ->
        forEach { value -> array.add(value.asChar()) }
    }
}

/**
 * [builder]를 호출해 [CharArrayList]를 생성합니다.
 *
 * ## 동작/계약
 * - [initialCapacity]는 0 이상이어야 하며 음수면 예외가 발생합니다.
 * - [builder]는 `0 until initialCapacity` 인덱스로 호출됩니다.
 * - 항상 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = charArrayList(2) { ('a'.code + it).toChar() }
 * // list.size() == 2
 * // list[1] == 'b'
 * ```
 */
inline fun charArrayList(
    initialCapacity: Int = 10,
    builder: (index: Int) -> Char,
): CharArrayList {
    initialCapacity.requireZeroOrPositiveNumber("initialCapacity")
    return CharArrayList(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(builder(index))
        }
    }
}

/**
 * 가변 인자 Char를 [CharArrayList]로 생성합니다.
 *
 * ## 동작/계약
 * - 입력 요소를 복사해 새 리스트를 allocate 합니다.
 * - 요소 순서는 인자 전달 순서를 유지합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b')
 * // list.size() == 2
 * // list[0] == 'a'
 * ```
 */
fun charArrayListOf(vararg elements: Char): CharArrayList =
    CharArrayList.newListWith(*elements)

/**
 * [CharIterable]을 [CharArrayList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신이 이미 [CharArrayList]면 allocate 없이 그대로 반환합니다.
 * - 그 외 구현체는 새 리스트를 allocate 해 복사합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b').toCharArrayList()
 * // list.size() == 2
 * // list[1] == 'b'
 * ```
 */
fun CharIterable.toCharArrayList(): CharArrayList = when (this) {
    is CharArrayList -> this
    else -> CharArrayList.newList(this)
}

/**
 * [CharIterable]을 Kotlin [Iterator]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterator를 allocate 하며 원본 iterable은 mutate 하지 않습니다.
 * - 순회는 원본 `charIterator()`를 그대로 위임합니다.
 *
 * ```kotlin
 * val iter = charArrayListOf('a').asIterator()
 * // iter.hasNext()
 * // iter.next() == 'a'
 * ```
 */
fun CharIterable.asIterator(): Iterator<Char> = object: Iterator<Char> {
    private val iter = charIterator()
    override fun hasNext(): Boolean = iter.hasNext()
    override fun next(): Char = iter.next()
}

/**
 * [CharIterable]을 Kotlin [Sequence]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - sequence 소비 시 원본 iterable을 순차적으로 순회합니다.
 * - 수신 iterable은 mutate 하지 않고 sequence 객체를 allocate 합니다.
 *
 * ```kotlin
 * val seq = charArrayListOf('a', 'b').asSequence()
 * // seq.count() == 2
 * // seq.first() == 'a'
 * ```
 */
fun CharIterable.asSequence(): Sequence<Char> = sequence {
    val iter = charIterator()
    while (iter.hasNext()) {
        yield(iter.next())
    }
}

/**
 * [CharIterable]을 Kotlin [Iterable]로 감싸 반환합니다.
 *
 * ## 동작/계약
 * - 래퍼 iterable을 allocate 하며 원본을 mutate 하지 않습니다.
 * - 각 순회 시작 시 새로운 iterator를 제공합니다.
 *
 * ```kotlin
 * val it = charArrayListOf('a', 'b').asIterable()
 * // it.count() == 2
 * // it.first() == 'a'
 * ```
 */
fun CharIterable.asIterable(): Iterable<Char> = Iterable { asIterator() }

/**
 * [CharIterable]을 Kotlin [List]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b').asList()
 * // list == listOf('a', 'b')
 * // list.size == 2
 * ```
 */
fun CharIterable.asList() = asIterable().toList()

/**
 * [CharIterable]을 Kotlin [MutableList]로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과로 새 mutable 리스트를 allocate 합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b').asMutableList()
 * // list.size == 2
 * // list[1] == 'b'
 * ```
 */
fun CharIterable.asMutableList() = asIterable().toMutableList()

/**
 * [CharIterable]을 Kotlin [Set]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 set은 중복 요소를 제거하며 새 객체를 allocate 합니다.
 *
 * ```kotlin
 * val set = charArrayListOf('a', 'a', 'b').asSet()
 * // set.size == 2
 * // 'a' in set
 * ```
 */
fun CharIterable.asSet() = asIterable().toSet()

/**
 * [CharIterable]을 Kotlin [MutableSet]으로 복사해 반환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않습니다.
 * - 결과 mutable set은 중복을 제거해 새 객체를 allocate 합니다.
 *
 * ```kotlin
 * val set = charArrayListOf('a', 'a', 'b').asMutableSet()
 * // set.size == 2
 * // 'b' in set
 * ```
 */
fun CharIterable.asMutableSet() = asIterable().toMutableSet()

/**
 * [CharIterable]을 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 원본 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b').toFastList()
 * // list.size == 2
 * // list[0] == 'a'
 * ```
 */
fun CharIterable.toFastList() = asIterable().toFastList()

/**
 * [CharIterable]을 UnifiedSet으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 값은 제거됩니다.
 *
 * ```kotlin
 * val set = charArrayListOf('a', 'a', 'b').toUnifiedSet()
 * // set.size == 2
 * // set.contains('a')
 * ```
 */
fun CharIterable.toUnifiedSet() = asIterable().toUnifiedSet()

/**
 * [CharIterable]을 FixedSizeList로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 크기는 입력 요소 수와 동일합니다.
 *
 * ```kotlin
 * val list = charArrayListOf('a', 'b').toFixedSizeList()
 * // list.size == 2
 * // list[1] == 'b'
 * ```
 */
fun CharIterable.toFixedSizeList() = asIterable().toFixedSizeList()

/**
 * [CharIterable]의 최댓값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [CharIterable.max] 값을 반환합니다.
 *
 * ```kotlin
 * // charArrayListOf('a', 'c').maxOrNull() == 'c'
 * // charArrayListOf().maxOrNull() == null
 * ```
 */
fun CharIterable.maxOrNull() = if (isEmpty) null else max()

/**
 * [CharIterable]의 최솟값을 nullable로 반환합니다.
 *
 * ## 동작/계약
 * - empty이면 null을 반환합니다.
 * - non-empty이면 [CharIterable.min] 값을 반환합니다.
 *
 * ```kotlin
 * // charArrayListOf('a', 'c').minOrNull() == 'a'
 * // charArrayListOf().minOrNull() == null
 * ```
 */
fun CharIterable.minOrNull() = if (isEmpty) null else min()

/**
 * [CharArray]를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 [FastList]를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = charArrayOf('a', 'b').toFastList()
 * // list.size == 2
 * // list[1] == 'b'
 * ```
 */
fun CharArray.toFastList(): FastList<Char> = asIterable().toFastList()
