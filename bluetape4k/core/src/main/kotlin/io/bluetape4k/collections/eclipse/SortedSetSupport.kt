package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.SortedSets
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet
import org.eclipse.collections.api.set.sorted.MutableSortedSet
import java.util.stream.Stream

/**
 * 비어 있는 immutable sorted set을 생성합니다.
 *
 * ## 동작/계약
 * - [T]는 [Comparable] 구현이 필요합니다.
 * - 항상 새 immutable set을 allocate 하며 mutate 하지 않습니다.
 *
 * ```kotlin
 * val set = emptyImmutableSortedSet<Int>()
 * check(set.isEmpty)
 * check(set.toSet().isEmpty())
 * ```
 */
fun <T: Comparable<T>> emptyImmutableSortedSet(): ImmutableSortedSet<T> = SortedSets.immutable.empty<T>()

/**
 * 비어 있는 mutable sorted set을 생성합니다.
 *
 * ## 동작/계약
 * - [T]는 [Comparable] 구현이 필요합니다.
 * - 항상 새 mutable set을 allocate 하며 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val set = emptyMutableSortedSet<Int>()
 * set.add(1)
 * check(set.contains(1))
 * ```
 */
fun <T: Comparable<T>> emptyMutableSortedSet(): MutableSortedSet<T> = SortedSets.mutable.empty<T>()

/**
 * 가변 인자를 immutable sorted set으로 생성합니다.
 *
 * ## 동작/계약
 * - 입력이 empty면 [emptyImmutableSortedSet]을 반환합니다.
 * - 중복 요소는 제거되며 자연 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val set = immutableSortedSetOf(3, 1, 1)
 * check(set.size() == 2)
 * check(set.toList().first() == 1)
 * ```
 */
fun <T: Comparable<T>> immutableSortedSetOf(vararg elements: T): ImmutableSortedSet<T> =
    if (elements.isEmpty()) emptyImmutableSortedSet()
    else SortedSets.immutable.of<T>(*elements)

/**
 * 가변 인자를 mutable sorted set으로 생성합니다.
 *
 * ## 동작/계약
 * - 입력이 empty면 [emptyMutableSortedSet]을 반환합니다.
 * - 중복 요소는 제거되며 자연 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val set = mutableSortedSetOf(3, 1, 1)
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> mutableSortedSetOf(vararg elements: T): MutableSortedSet<T> =
    if (elements.isEmpty()) emptyMutableSortedSet()
    else SortedSets.mutable.of<T>(*elements)

/**
 * [builder]를 호출해 mutable sorted set을 생성합니다.
 *
 * ## 동작/계약
 * - [size] 횟수만큼 builder를 호출합니다.
 * - 같은 값이 생성되면 set semantics로 중복이 제거됩니다.
 * - 항상 새 mutable set을 allocate 합니다.
 *
 * ```kotlin
 * val set = mutableSortedSet(3) { 3 - it }
 * check(set.size() == 3)
 * check(set.first() == 1)
 * ```
 */
inline fun <T: Comparable<T>> mutableSortedSet(
    size: Int = 10,
    @BuilderInference builder: (Int) -> T,
): MutableSortedSet<T> =
    SortedSets.mutable.of<T>().apply {
        repeat(size) {
            add(builder(it))
        }
    }

/**
 * [Iterable]을 mutable sorted set으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 요소는 제거되며 자연 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val set = listOf(2, 1, 1).toMutableSortedSet()
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> Iterable<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this)

/**
 * [Sequence]를 mutable sorted set으로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 새 set을 allocate 하며 중복 제거/정렬을 수행합니다.
 *
 * ```kotlin
 * val set = sequenceOf(2, 1, 1).toMutableSortedSet()
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> Sequence<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

/**
 * [Iterator]를 mutable sorted set으로 변환합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비합니다.
 * - 새 set을 allocate 하며 중복 제거/정렬을 수행합니다.
 *
 * ```kotlin
 * val set = listOf(2, 1, 1).iterator().toMutableSortedSet()
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> Iterator<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

/**
 * 배열을 mutable sorted set으로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 set을 allocate 합니다.
 * - 중복 요소는 제거되며 자연 정렬 순서를 유지합니다.
 *
 * ```kotlin
 * val set = arrayOf(2, 1, 1).toMutableSortedSet()
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> Array<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

/**
 * Java [Stream]을 mutable sorted set으로 변환합니다.
 *
 * ## 동작/계약
 * - stream을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 set을 allocate 하며 중복 제거/정렬을 수행합니다.
 *
 * ```kotlin
 * val set = Stream.of(2, 1, 1).toMutableSortedSet()
 * check(set.size() == 2)
 * check(set.first() == 1)
 * ```
 */
fun <T: Comparable<T>> Stream<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())
