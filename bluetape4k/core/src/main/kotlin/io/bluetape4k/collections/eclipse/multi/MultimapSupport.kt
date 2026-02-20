package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.eclipse.toTuplePair
import io.bluetape4k.collections.eclipse.toUnifiedSet
import org.eclipse.collections.api.multimap.Multimap
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap
import org.eclipse.collections.api.multimap.list.MutableListMultimap
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap
import org.eclipse.collections.api.multimap.set.MutableSetMultimap
import org.eclipse.collections.api.multimap.sortedbag.ImmutableSortedBagMultimap
import org.eclipse.collections.api.multimap.sortedset.ImmutableSortedSetMultimap
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap
import org.eclipse.collections.impl.multimap.list.FastListMultimap
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap

fun <K, V> emptyImmutableListMultimap(): ImmutableListMultimap<K, V> = Multimaps.immutable.list.empty<K, V>()
fun <K, V> emptyImmutableSetMultimap(): ImmutableSetMultimap<K, V> = Multimaps.immutable.set.empty<K, V>()
fun <K, V> emptyImmutableBagMultimap(): ImmutableBagMultimap<K, V> = Multimaps.immutable.bag.empty<K, V>()

fun <K, V: Comparable<V>> emptyImmutableSortedSetMultimap(): ImmutableSortedSetMultimap<K, V> =
    Multimaps.immutable.sortedSet.of<K, V>(Comparator<V>.naturalOrder())

fun <K, V: Comparable<V>> emptyImmutableSortedBagMultimap(): ImmutableSortedBagMultimap<K, V> =
    Multimaps.immutable.sortedBag.of<K, V>(Comparator<V>.naturalOrder())

fun <K, V> emptyMutableListMultimap(): MutableListMultimap<K, V> = Multimaps.mutable.list.empty<K, V>()
fun <K, V> emptyMutableSetMultimap(): MutableSetMultimap<K, V> = Multimaps.mutable.set.empty<K, V>()
fun <K, V> emptyMutableBagMultimap(): MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty<K, V>()


fun <K, V> listMultimapOf(vararg pairs: Pair<K, V>): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

fun <K, V> setMultimapOf(vararg pairs: Pair<K, V>): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

fun <K, V> bagMultimapOf(vararg pairs: Pair<K, V>): HashBagMultimap<K, V> =
    HashBagMultimap.newMultimap<K, V>().also { multimap ->
        pairs.forEach { (key, value) -> multimap.put(key, value) }
    }

fun <K, V> Map<K, V>.toImmutableListMultimap(): ImmutableListMultimap<K, V> =
    toListMultimap().toImmutable()

fun <K, V> Map<K, V>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty<K, V>(),
): MutableListMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

fun <K, V> Map<K, V>.toImmutableSetMultimap(): ImmutableSetMultimap<K, V> =
    toSetMultimap().toImmutable()

fun <K, V> Map<K, V>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty<K, V>(),
): MutableSetMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

fun <K, V> Map<K, V>.toImmutableBagMultimap(): ImmutableBagMultimap<K, V> =
    toBagMultimap().toImmutable()

fun <K, V> Map<K, V>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty<K, V>(),
): MutableBagMultimap<K, V> {
    forEach { (key, value) ->
        destination.put(key, value)
    }
    return destination
}

fun <K, V> Multimap<K, V>.filter(predicate: (K, Iterable<V>) -> Boolean): Multimap<K, V> =
    selectKeysMultiValues { key, values -> predicate(key, values) }

inline fun <K, V, R> Multimap<K, V>.toList(mapper: (K, Iterable<V>) -> R): List<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }

inline fun <K, V, R> Multimap<K, V>.toSet(mapper: (K, Iterable<V>) -> R): Set<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }.toUnifiedSet()

inline fun <T, K, V> Iterable<T>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableListMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

inline fun <T, K, V> Iterable<T>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableSetMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

inline fun <T, K, V> Iterable<T>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty(),
    mapper: (T) -> Pair<K, V>,
): MutableBagMultimap<K, V> {
    forEach {
        destination.add(mapper(it).toTuplePair())
    }
    return destination
}

inline fun <T, K> Iterable<T>.groupByListMultimap(
    destination: MutableListMultimap<K, T> = Multimaps.mutable.list.empty(),
    keySelector: (T) -> K,
): MutableListMultimap<K, T> {
    return toListMultimap(destination) { keySelector(it) to it }
}

inline fun <T, K> Iterable<T>.groupBySetMultimap(
    destination: MutableSetMultimap<K, T> = Multimaps.mutable.set.empty(),
    keySelector: (T) -> K,
): MutableSetMultimap<K, T> {
    return toSetMultimap(destination) { keySelector(it) to it }
}

inline fun <T, K> Iterable<T>.groupByBagMultimap(
    destination: MutableBagMultimap<K, T> = Multimaps.mutable.bag.empty(),
    keySelector: (T) -> K,
): MutableBagMultimap<K, T> {
    return toBagMultimap(destination) { keySelector(it) to it }
}

fun <K, V> Iterable<Pair<K, V>>.toListMultimap(
    destination: MutableListMultimap<K, V> = Multimaps.mutable.list.empty(),
): MutableListMultimap<K, V> {
    return toListMultimap(destination) { it }
}

fun <K, V> Iterable<Pair<K, V>>.toSetMultimap(
    destination: MutableSetMultimap<K, V> = Multimaps.mutable.set.empty(),
): MutableSetMultimap<K, V> {
    return toSetMultimap(destination) { it }
}

fun <K, V> Iterable<Pair<K, V>>.toBagMultimap(
    destination: MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty(),
): MutableBagMultimap<K, V> {
    return toBagMultimap(destination) { it }
}
