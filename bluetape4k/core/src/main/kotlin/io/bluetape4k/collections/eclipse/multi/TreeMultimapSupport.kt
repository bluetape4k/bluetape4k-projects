package io.bluetape4k.collections.eclipse.multi

import org.eclipse.collections.impl.multimap.set.sorted.TreeSortedSetMultimap

typealias TreeMultimap<K, V> = TreeSortedSetMultimap<K, V>

inline fun <K: Comparable<K>, V> Iterable<V>.toTreeMultimap(
    destination: TreeMultimap<K, V> = TreeMultimap.newMultimap<K, V>(),
    @BuilderInference keySelector: (V) -> K,
): TreeMultimap<K, V> {
    this@toTreeMultimap.forEach { element ->
        destination.put(keySelector(element), element)
    }
    return destination
}

inline fun <V, K: Comparable<K>> Iterable<V>.toTreeMultimap(
    comparator: Comparator<V>,
    @BuilderInference keySelector: (V) -> K,
): TreeMultimap<K, V> =
    TreeMultimap.newMultimap<K, V>(comparator).also { treeMap ->
        this@toTreeMultimap.forEach { element ->
            treeMap.put(keySelector(element), element)
        }
    }

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
