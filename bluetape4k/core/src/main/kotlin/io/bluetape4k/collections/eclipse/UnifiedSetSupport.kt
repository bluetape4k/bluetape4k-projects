package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.set.FixedSizeSet
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import java.util.stream.Stream

/**
 * 비어 있는 fixed-size set을 생성합니다.
 *
 * ## 동작/계약
 * - 항상 새 fixed-size set을 allocate 합니다.
 * - 고정 크기 특성으로 add/remove가 제한될 수 있습니다.
 *
 * ```kotlin
 * val set = emptyFixedSet<Int>()
 * // set.isEmpty
 * // set.toSet().isEmpty()
 * ```
 */
fun <T> emptyFixedSet(): FixedSizeSet<T> = Sets.fixedSize.empty<T>()

/**
 * 비어 있는 [UnifiedSet]을 생성합니다.
 *
 * ## 동작/계약
 * - 항상 새 mutable set을 allocate 합니다.
 * - 중복 요소는 set semantics로 제거됩니다.
 *
 * ```kotlin
 * val set = emptyUnifiedSet<Int>()
 * set.add(1)
 * // set.contains(1)
 * ```
 */
fun <T> emptyUnifiedSet(): UnifiedSet<T> = UnifiedSet.newSet<T>()

/**
 * [builder]를 호출해 [UnifiedSet]을 생성합니다.
 *
 * ## 동작/계약
 * - [size] 횟수만큼 builder를 호출합니다.
 * - 동일 값이 생성되면 중복이 제거됩니다.
 * - 항상 새 set을 allocate 합니다.
 *
 * ```kotlin
 * val set = unifiedSet(3) { it % 2 }
 * // set.size == 2
 * // set.contains(0)
 * ```
 */
inline fun <T> unifiedSet(
    size: Int = 16,
    builder: (Int) -> T,
): UnifiedSet<T> =
    UnifiedSet.newSet<T>(size).apply {
        repeat(size) {
            add(builder(it))
        }
    }

/**
 * 가변 인자로 [UnifiedSet]을 생성합니다.
 *
 * ## 동작/계약
 * - 입력이 empty이면 [emptyUnifiedSet]을 반환합니다.
 * - 중복 요소는 제거되며 항상 새 set을 allocate 합니다.
 *
 * ```kotlin
 * val set = unifiedSetOf(1, 1, 2)
 * // set.size == 2
 * // set.contains(2)
 * ```
 */
fun <T> unifiedSetOf(vararg elements: T): UnifiedSet<T> =
    if (elements.isEmpty()) emptyUnifiedSet()
    else UnifiedSet.newSetWith<T>(*elements)

/**
 * 초기 용량 힌트를 사용해 빈 [UnifiedSet]을 생성합니다.
 *
 * ## 동작/계약
 * - [size]는 내부 capacity 힌트이며 실제 요소 수가 아닙니다.
 * - 항상 새 set을 allocate 합니다.
 *
 * ```kotlin
 * val set = unifiedSetOf<Int>(16)
 * // set.isEmpty()
 * // set is UnifiedSet<Int>
 * ```
 */
fun <T> unifiedSetOf(size: Int): UnifiedSet<T> = UnifiedSet.newSet<T>(size)

/**
 * [Iterable]을 [UnifiedSet]으로 복사합니다.
 *
 * ## 동작/계약
 * - [destination]을 직접 mutate 하고 같은 인스턴스를 반환합니다.
 * - 중복 요소는 제거됩니다.
 * - 입력이 `Collection`인 경우 크기 분기 최적화를 적용합니다.
 *
 * ```kotlin
 * val out = listOf(1, 1, 2).toUnifiedSet()
 * // out.size == 2
 * // out.contains(1)
 * ```
 */
fun <T> Iterable<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> {
    when (this) {
        is Collection -> {
            if (size == 1) {
                destination.add(if (this@toUnifiedSet is List) get(0) else iterator().next())
            } else if (size > 1) {
                destination.addAll(this@toUnifiedSet)
            }
        }
        else -> {
            destination.addAll(this@toUnifiedSet)
        }
    }
    return destination
}

/**
 * [Sequence]를 [UnifiedSet]으로 복사합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비하며 [destination]을 mutate 합니다.
 * - 중복 요소는 제거됩니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = sequenceOf(1, 1, 2).toUnifiedSet()
 * // out.size == 2
 * // out.contains(2)
 * ```
 */
fun <T> Sequence<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)

/**
 * [Iterator]를 [UnifiedSet]으로 복사합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 중복 요소는 제거됩니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = listOf(1, 1, 2).iterator().toUnifiedSet()
 * // out.size == 2
 * // out.contains(1)
 * ```
 */
fun <T> Iterator<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)

/**
 * 배열을 [UnifiedSet]으로 복사합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 [destination]만 mutate 합니다.
 * - 중복 요소는 제거됩니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = arrayOf(1, 1, 2).toUnifiedSet()
 * // out.size == 2
 * // out.contains(2)
 * ```
 */
fun <T> Array<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)

/**
 * Java [Stream]을 [UnifiedSet]으로 복사합니다.
 *
 * ## 동작/계약
 * - stream을 한 번 소비하며 재사용할 수 없습니다.
 * - [destination]을 mutate 하고 중복 요소를 제거합니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val out = Stream.of(1, 1, 2).toUnifiedSet()
 * // out.size == 2
 * // out.contains(1)
 * ```
 */
fun <T> Stream<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)
