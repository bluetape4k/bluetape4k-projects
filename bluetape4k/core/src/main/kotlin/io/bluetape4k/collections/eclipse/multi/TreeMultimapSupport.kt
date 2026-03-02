package io.bluetape4k.collections.eclipse.multi

import org.eclipse.collections.impl.multimap.set.sorted.TreeSortedSetMultimap

/**
 * TreeMultimap 타입을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val type = TreeMultimap::class
 * println(type.simpleName)
 * check(type.simpleName != null)
 * ```
 */
typealias TreeMultimap<K, V> = TreeSortedSetMultimap<K, V>

/**
 * toTreeMultimap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toTreeMultimap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K: Comparable<K>, V> Iterable<V>.toTreeMultimap(
    destination: TreeMultimap<K, V> = TreeMultimap.newMultimap<K, V>(),
    @BuilderInference keySelector: (V) -> K,
): TreeMultimap<K, V> {
    this@toTreeMultimap.forEach { element ->
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * toTreeMultimap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toTreeMultimap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <V, K: Comparable<K>> Iterable<V>.toTreeMultimap(
    comparator: Comparator<V>,
    @BuilderInference keySelector: (V) -> K,
): TreeMultimap<K, V> =
    TreeMultimap.newMultimap<K, V>(comparator).also { treeMap ->
        this@toTreeMultimap.forEach { element ->
            treeMap.put(keySelector(element), element)
        }
    }

/**
 * toTreeMultimap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toTreeMultimap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <E, K: Comparable<K>, V> Iterable<E>.toTreeMultimap(
    @BuilderInference keySelector: (E) -> K,
    @BuilderInference valueSelector: (E) -> V,
    destination: TreeMultimap<K, V> = TreeMultimap(),
): TreeMultimap<K, V> {
    this@toTreeMultimap.forEach { element ->
        destination.put(keySelector(element), valueSelector(element))
    }
    return destination
}

/**
 * toTreeMultimap 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toTreeMultimap
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <E, K, V: Comparable<V>> Iterable<E>.toTreeMultimap(
    comparator: Comparator<V>,
    @BuilderInference keySelector: (E) -> K,
    @BuilderInference valueSelector: (E) -> V,
): TreeMultimap<K, V> =
    TreeMultimap.newMultimap<K, V>(comparator).also { treeMap ->
        this@toTreeMultimap.forEach { element ->
            treeMap.put(keySelector(element), valueSelector(element))
        }
    }
