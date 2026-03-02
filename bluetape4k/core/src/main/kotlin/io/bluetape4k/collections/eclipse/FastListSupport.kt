@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.impl.list.mutable.FastList
import java.util.stream.Stream

/**
 * 비어 있는 [FastList]를 생성합니다.
 *
 * ## 동작/계약
 * - null 입력은 없으며 항상 non-null 리스트를 반환합니다.
 * - 수신 객체를 mutate 하지 않고 새 리스트를 allocate 합니다.
 * - 결과 리스트는 mutable 입니다.
 *
 * ```kotlin
 * val list = emptyFastList<Int>()
 * // list.isEmpty()
 * // list is MutableList<Int>
 * ```
 */
inline fun <T> emptyFastList(): FastList<T> = FastList.newList<T>()

/**
 * 지정한 [size] 크기의 [FastList]를 생성하고 [initializer]로 채웁니다.
 *
 * ## 동작/계약
 * - [size]가 0이면 빈 리스트를 반환합니다.
 * - 항상 새 [FastList]를 allocate 하며 기존 컬렉션은 mutate 하지 않습니다.
 * - [initializer]는 인덱스 `0 until size`에 대해 순서대로 호출됩니다.
 *
 * ```kotlin
 * val list = fastList(3) { it * 2 }
 * // list == listOf(0, 2, 4)
 * // list.size == 3
 * ```
 *
 * @param size 생성할 리스트 크기
 * @param initializer 각 인덱스의 요소를 생성하는 함수
 */
inline fun <T> fastList(
    size: Int = 16,
    @BuilderInference initializer: (index: Int) -> T,
): FastList<T> =
    FastList.newList<T>(size).apply {
        repeat(size) { index ->
            add(initializer(index))
        }
    }

/**
 * 가변 인자를 [FastList]로 변환합니다.
 *
 * ## 동작/계약
 * - [elements]가 empty이면 빈 [FastList]를 반환합니다.
 * - 항상 새 리스트를 allocate 하며 입력 배열은 mutate 하지 않습니다.
 * - 요소 순서는 입력 순서를 유지합니다.
 *
 * ```kotlin
 * val list = fastListOf(1, 2, 3)
 * // list == listOf(1, 2, 3)
 * // list.size == 3
 * ```
 *
 * @param elements 리스트에 담을 요소
 */
fun <T> fastListOf(vararg elements: T): FastList<T> {
    return if (elements.isEmpty()) FastList.newList<T>()
    else FastList.newListWith<T>(*elements)
}

/**
 * [Iterable]을 [FastList]로 복사합니다.
 *
 * ## 동작/계약
 * - null 입력은 허용하지 않습니다.
 * - [destination]을 직접 mutate 하며, 결과로 같은 인스턴스를 반환합니다.
 * - `Collection` 입력은 크기 분기 최적화를 적용하고, 일반 `Iterable`은 순회 복사합니다.
 *
 * ```kotlin
 * val dest = emptyFastList<Int>()
 * val out = listOf(1, 2, 3).toFastList(dest)
 * // out === dest
 * ```
 *
 * @param destination 요소를 누적할 대상 리스트
 */
fun <T> Iterable<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> {
    when (this) {
        is Collection -> {
            if (size == 1) {
                destination.add(if (this is List) get(0) else iterator().next())
            } else if (size > 1) {
                destination.addAll(this@toFastList)
            }
        }
        else -> {
            destination.addAll(this@toFastList)
        }
    }
    return destination
}

/**
 * [Sequence]를 [FastList]로 복사합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 순회하여 [destination]을 mutate 합니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 * - 지연 시퀀스 특성상 소비 후 재사용 가능성에 주의하세요.
 *
 * ```kotlin
 * val list = sequenceOf(1, 2, 3).toFastList()
 * // list == listOf(1, 2, 3)
 * // list is FastList<Int>
 * ```
 */
fun <T> Sequence<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

/**
 * [Iterator]를 [FastList]로 복사합니다.
 *
 * ## 동작/계약
 * - 이터레이터를 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 * - 이미 소비된 이터레이터는 추가 요소를 제공하지 않습니다.
 *
 * ```kotlin
 * val list = listOf(1, 2).iterator().toFastList()
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T> Iterator<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

/**
 * 배열을 [FastList]로 복사합니다.
 *
 * ## 동작/계약
 * - 입력 배열은 mutate 하지 않습니다.
 * - [destination]을 mutate 하며 결과로 같은 인스턴스를 반환합니다.
 * - 요소 순서는 배열 인덱스 순서를 유지합니다.
 *
 * ```kotlin
 * val list = arrayOf("a", "b").toFastList()
 * // list == listOf("a", "b")
 * // list.size == 2
 * ```
 */
fun <T> Array<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

/**
 * Java [Stream]을 [FastList]로 복사합니다.
 *
 * ## 동작/계약
 * - 스트림을 끝까지 소비하며 [destination]을 mutate 합니다.
 * - 결과로 [destination] 인스턴스를 반환합니다.
 * - 스트림은 1회성 소비이므로 호출 후 재사용할 수 없습니다.
 *
 * ```kotlin
 * val list = Stream.of(1, 2, 3).toFastList()
 * // list == listOf(1, 2, 3)
 * // list.size == 3
 * ```
 */
fun <T> Stream<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

/**
 * [Iterable] 요소를 [transform]으로 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - null 입력은 허용하지 않습니다.
 * - 입력은 [destination]으로 복사된 뒤 `collect`로 변환되어 새 결과 리스트를 allocate 할 수 있습니다.
 * - 입력 순서를 유지합니다.
 *
 * ```kotlin
 * val mapped = listOf(1, 2, 3).fastMap { it * 10 }
 * // mapped == listOf(10, 20, 30)
 * // mapped.size == 3
 * ```
 *
 * @param destination 중간 복사에 사용할 대상 리스트
 * @param transform 요소 변환 함수
 */
fun <T, R> Iterable<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = toFastList(destination).collect { transform(it) }

/**
 * [Sequence] 요소를 [transform]으로 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 소비해 변환합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastMap]에 위임합니다.
 *
 * ```kotlin
 * val mapped = sequenceOf(1, 2).fastMap { "#$it" }
 * // mapped == listOf("#1", "#2")
 * // mapped.size == 2
 * ```
 */
fun <T, R> Sequence<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)

/**
 * [Iterator] 요소를 [transform]으로 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 이터레이터를 끝까지 소비합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastMap]에 위임합니다.
 *
 * ```kotlin
 * val mapped = listOf(1, 2).iterator().fastMap { it + 1 }
 * // mapped == listOf(2, 3)
 * // mapped.size == 2
 * ```
 */
fun <T, R> Iterator<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)

/**
 * 배열 요소를 [transform]으로 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 입력 배열은 mutate 하지 않습니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastMap]에 위임합니다.
 *
 * ```kotlin
 * val mapped = arrayOf("a", "bb").fastMap { it.length }
 * // mapped == listOf(1, 2)
 * // mapped.size == 2
 * ```
 */
fun <T, R> Array<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)


/**
 * [Iterable] 요소를 null 제외 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - [mapper] 결과가 null인 요소는 결과 리스트에서 제외됩니다.
 * - [destination]을 중간 복사 버퍼로 사용하며 결과 리스트는 별도로 allocate 될 수 있습니다.
 * - 수신 iterable 자체는 mutate 하지 않습니다.
 *
 * ```kotlin
 * val list = listOf("1", "x").fastMapNotNull { it.toIntOrNull() }
 * // list == listOf(1)
 * // list.size == 1
 * ```
 *
 * @param destination 중간 복사에 사용할 대상 리스트
 * @param mapper 요소 변환 함수(null 반환 허용)
 */
fun <T, R: Any> Iterable<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    mapper: (T) -> R?,
): FastList<R> = toFastList(destination).collectIf<R>({ it != null }) { mapper(it) }

/**
 * [Sequence]를 null 제외 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 소비합니다.
 * - [transform] 시그니처가 non-null 반환형이므로 실제 결과에는 null이 포함되지 않습니다.
 * - 내부 동작은 [Iterable.fastMapNotNull]에 위임합니다.
 *
 * ```kotlin
 * val list = sequenceOf("a", "bb").fastMapNotNull { it.length }
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T, R: Any> Sequence<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)

/**
 * [Iterator]를 null 제외 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 이터레이터를 끝까지 소비합니다.
 * - [transform] 시그니처가 non-null 반환형이므로 실제 결과에는 null이 포함되지 않습니다.
 * - 내부 동작은 [Iterable.fastMapNotNull]에 위임합니다.
 *
 * ```kotlin
 * val list = listOf("a", "bb").iterator().fastMapNotNull { it.length }
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T, R: Any> Iterator<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)

/**
 * 배열을 null 제외 변환하여 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 입력 배열은 mutate 하지 않습니다.
 * - [transform] 시그니처가 non-null 반환형이므로 실제 결과에는 null이 포함되지 않습니다.
 * - 내부 동작은 [Iterable.fastMapNotNull]에 위임합니다.
 *
 * ```kotlin
 * val list = arrayOf("a", "bb").fastMapNotNull { it.length }
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T, R: Any> Array<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)


/**
 * [Iterable]에서 [predicate]를 만족하는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - null 입력은 허용하지 않습니다.
 * - [destination]을 중간 버퍼로 사용하며 결과 리스트는 별도로 allocate 될 수 있습니다.
 * - 입력 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3, 4).fastFilter { it % 2 == 0 }
 * // list == listOf(2, 4)
 * // list.size == 2
 * ```
 */
fun <T> Iterable<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = toFastList(destination).select { predicate(it) }

/**
 * [Sequence]에서 [predicate]를 만족하는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 소비합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilter]에 위임합니다.
 *
 * ```kotlin
 * val list = sequenceOf(1, 2, 3).fastFilter { it >= 2 }
 * // list == listOf(2, 3)
 * // list.size == 2
 * ```
 */
fun <T> Sequence<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)

/**
 * [Iterator]에서 [predicate]를 만족하는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 이터레이터를 끝까지 소비합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilter]에 위임합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3).iterator().fastFilter { it > 1 }
 * // list == listOf(2, 3)
 * // list.size == 2
 * ```
 */
fun <T> Iterator<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)

/**
 * 배열에서 [predicate]를 만족하는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 입력 배열은 mutate 하지 않습니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilter]에 위임합니다.
 *
 * ```kotlin
 * val list = arrayOf(1, 2, 3).fastFilter { it != 2 }
 * // list == listOf(1, 3)
 * // list.size == 2
 * ```
 */
fun <T> Array<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)


/**
 * [Iterable]에서 [predicate]를 만족하지 않는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - null 입력은 허용하지 않습니다.
 * - [destination]을 중간 버퍼로 사용하며 결과 리스트는 별도로 allocate 될 수 있습니다.
 * - 입력 순서를 유지합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3).fastFilterNot { it == 2 }
 * // list == listOf(1, 3)
 * // list.size == 2
 * ```
 */
fun <T> Iterable<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = toFastList(destination).select { !predicate(it) }

/**
 * [Sequence]에서 [predicate]를 만족하지 않는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 시퀀스를 한 번 소비합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilterNot]에 위임합니다.
 *
 * ```kotlin
 * val list = sequenceOf(1, 2, 3).fastFilterNot { it == 1 }
 * // list == listOf(2, 3)
 * // list.size == 2
 * ```
 */
fun <T> Sequence<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

/**
 * [Iterator]에서 [predicate]를 만족하지 않는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 이터레이터를 끝까지 소비합니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilterNot]에 위임합니다.
 *
 * ```kotlin
 * val list = listOf(1, 2, 3).iterator().fastFilterNot { it > 2 }
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T> Iterator<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

/**
 * 배열에서 [predicate]를 만족하지 않는 요소만 [FastList]로 반환합니다.
 *
 * ## 동작/계약
 * - 입력 배열은 mutate 하지 않습니다.
 * - 입력 순서를 유지합니다.
 * - 내부 동작은 [Iterable.fastFilterNot]에 위임합니다.
 *
 * ```kotlin
 * val list = arrayOf(1, 2, 3).fastFilterNot { it == 3 }
 * // list == listOf(1, 2)
 * // list.size == 2
 * ```
 */
fun <T> Array<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

/**
 * nullable [FastList]가 null이면 빈 [FastList]를 반환합니다.
 *
 * ## 동작/계약
 * - 수신 값이 null이면 새 빈 리스트를 allocate 합니다.
 * - 수신 값이 non-null이면 allocate 없이 그대로 반환합니다.
 * - 수신 리스트를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val a: FastList<Int>? = null
 * val b = a.orEmpty()
 * // b.isEmpty()
 * ```
 */
inline fun <T> FastList<T>?.orEmpty(): FastList<T> =
    this ?: emptyFastList()
