package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.fastListOf
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap

typealias TreeMultimap<K, V> = TreeSortedMap<K, MutableList<V>>

fun <K, V> TreeMultimap<K, V>.valueSize(): Int = this.valuesView().sumOf { it.size }

inline fun <K: Comparable<K>, V> Iterable<V>.toTreeMultimap(keySelector: (V) -> K): TreeMultimap<K, V> =
    TreeMultimap<K, V>().also { map ->
        forEach { element ->
            map.getIfAbsentPut(keySelector(element), fastListOf()).add(element)
        }
    }

inline fun <K: Comparable<K>, V> Iterable<V>.toTreeMultimap(
    comparator: Comparator<K>, keySelector: (V) -> K,
): TreeMultimap<K, V> =
    TreeMultimap<K, V>(comparator).also { map ->
        forEach { element ->
            map.getIfAbsentPut(keySelector(element), fastListOf()).add(element)
        }
    }

inline fun <E, K: Comparable<K>, V> Iterable<E>.toTreeMultimap(
    keySelector: (E) -> K,
    valueSelector: (E) -> V,
): TreeMultimap<K, V> =
    TreeMultimap<K, V>().also { map ->
        forEach { element ->
            map.getIfAbsentPut(keySelector(element), fastListOf()).add(valueSelector(element))
        }
    }


inline fun <E, K: Comparable<K>, V> Iterable<E>.toTreeMultimap(
    comparator: Comparator<K>,
    keySelector: (E) -> K,
    valueSelector: (E) -> V,
): TreeMultimap<K, V> =
    TreeMultimap<K, V>(comparator).also { map ->
        forEach { element ->
            map.getIfAbsentPut(keySelector(element), fastListOf()).add(valueSelector(element))
        }
    }
