package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.list.FixedSizeList
import java.util.stream.Stream

/**
 * 비어 있는 [FixedSizeList]를 생성합니다.
 *
 * ## 동작/계약
 * - 항상 새 고정 크기 리스트를 allocate 합니다.
 * - 크기 변경 연산(add/remove)은 지원되지 않습니다.
 *
 * ```kotlin
 * val list = emptyFixedSizeList<Int>()
 * // list.isEmpty
 * // list.toList().isEmpty()
 * ```
 */
fun <T> emptyFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.empty<T>()

/**
 * [builder]를 호출해 고정 크기 리스트를 생성합니다.
 *
 * ## 동작/계약
 * - [size] 횟수만큼 builder를 호출합니다.
 * - 내부적으로 `fastList(...).toFixedSizeList()`를 사용해 새 리스트를 allocate 합니다.
 * - 결과는 고정 크기이므로 크기 변경은 허용되지 않습니다.
 *
 * ```kotlin
 * val list = fixedSizeList(3) { it + 1 }
 * // list.size() == 3
 * // list[0] == 1
 * ```
 */
inline fun <T> fixedSizeList(
    size: Int = 16,
    builder: (index: Int) -> T,
): FixedSizeList<T> =
    fastList(size, builder).toFixedSizeList()

/**
 * 가변 인자 요소로 [FixedSizeList]를 생성합니다.
 *
 * ## 동작/계약
 * - 입력이 empty이면 빈 고정 크기 리스트를 반환합니다.
 * - 항상 새 리스트를 allocate 하며 요소 순서를 유지합니다.
 * - 결과 리스트는 크기 변경이 제한됩니다.
 *
 * ```kotlin
 * val list = fixedSizeListOf(1, 2, 3)
 * // list.size() == 3
 * // list[2] == 3
 * ```
 */
fun <T> fixedSizeListOf(vararg elements: T): FixedSizeList<T> {
    return if (elements.isEmpty()) Lists.fixedSize.empty<T>()
    else Lists.fixedSize.of<T>(*elements)
}

/**
 * [Iterable]을 [FixedSizeList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 iterable은 mutate 하지 않고 새 고정 크기 리스트를 allocate 합니다.
 * - 요소 순서는 순회 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2).toFixedSizeList()
 * // list.size() == 2
 * // list[1] == 2
 * ```
 */
fun <T> Iterable<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this)

/**
 * [Sequence]를 [FixedSizeList]로 변환합니다.
 *
 * ## 동작/계약
 * - sequence를 한 번 소비합니다.
 * - 새 고정 크기 리스트를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = sequenceOf(1, 2).toFixedSizeList()
 * // list.size() == 2
 * // list[0] == 1
 * ```
 */
fun <T> Sequence<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())

/**
 * [Iterator]를 [FixedSizeList]로 변환합니다.
 *
 * ## 동작/계약
 * - iterator를 끝까지 소비합니다.
 * - 새 고정 크기 리스트를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2).iterator().toFixedSizeList()
 * // list.size() == 2
 * // list[1] == 2
 * ```
 */
fun <T> Iterator<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())

/**
 * 배열을 [FixedSizeList]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 배열은 mutate 하지 않고 새 고정 크기 리스트를 allocate 합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = arrayOf(1, 2).toFixedSizeList()
 * // list.size() == 2
 * // list[0] == 1
 * ```
 */
fun <T> Array<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())

/**
 * Java [Stream]을 [FixedSizeList]로 변환합니다.
 *
 * ## 동작/계약
 * - stream을 한 번 소비하며 재사용할 수 없습니다.
 * - 새 고정 크기 리스트를 allocate 하며 요소 순서를 유지합니다.
 *
 * ```kotlin
 * val list = Stream.of(1, 2).toFixedSizeList()
 * // list.size() == 2
 * // list[1] == 2
 * ```
 */
fun <T> Stream<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())
