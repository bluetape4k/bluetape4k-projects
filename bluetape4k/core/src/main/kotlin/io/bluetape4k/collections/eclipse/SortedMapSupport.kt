package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.SortedMaps
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap
import org.eclipse.collections.api.map.sorted.MutableSortedMap

fun <K: Comparable<K>, V> emptyImmutableSortedMap(): ImmutableSortedMap<K, V> = SortedMaps.immutable.empty<K, V>()

fun <K: Comparable<K>, V> emptyMutableSortedMap(): MutableSortedMap<K, V> = SortedMaps.mutable.empty<K, V>()

inline fun <K: Comparable<K>, V> mutableSortedMap(
    size: Int,
    @BuilderInference builder: (Int) -> Pair<K, V>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>().apply {
        repeat(size) { index ->
            add(builder(index).toTuplePair())
        }
    }

fun <K: Comparable<K>, V> mutableSortedMapOf(
    vararg pairs: Pair<K, V>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>().apply {
        pairs.forEach { pair ->
            add(pair.toTuplePair())
        }
    }

fun <K: Comparable<K>, V> mutableSortedMapOf(
    vararg pairs: Pair<K, V>,
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    SortedMaps.mutable.of<K, V>(comparator).apply {
        pairs.forEach { pair ->
            add(pair.toTuplePair())
        }
    }

fun <K: Comparable<K>, V> Map<K, V>.toMutableSortedMap(): MutableSortedMap<K, V> = when (this) {
    is MutableSortedMap<K, V> -> this
    else -> SortedMaps.mutable.ofSortedMap<K, V>(this)
}

@JvmName("toMutableSortedMapFromIterablePairToDest")
fun <K: Comparable<K>, V> Iterable<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> {
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

@JvmName("toMutableSortedMapFromIterablePairWithComparator")
fun <K: Comparable<K>, V> Iterable<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> {
    val destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(comparator)
    forEach {
        destination[it.first] = it.second
    }
    return destination
}

@JvmName("toMutableSortedMapFromSequencePairToDest")
fun <K: Comparable<K>, V> Sequence<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

@JvmName("toMutableSortedMapFromSequencePairWithComparator")
fun <K: Comparable<K>, V> Sequence<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)

@JvmName("toMutableSortedMapFromSequencePairToDest")
fun <K: Comparable<K>, V> Iterator<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

@JvmName("toMutableSortedMapFromIteratorPairWithComparator")
fun <K: Comparable<K>, V> Iterator<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)

@JvmName("toMutableSortedMapFromArrayPairToDest")
fun <K: Comparable<K>, V> Array<Pair<K, V>>.toMutableSortedMap(
    destination: MutableSortedMap<K, V> = SortedMaps.mutable.of(),
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(destination)

@JvmName("toMutableSortedMapFromArrayPairWithComparator")
fun <K: Comparable<K>, V> Array<Pair<K, V>>.toMutableSortedMap(
    comparator: Comparator<K>,
): MutableSortedMap<K, V> =
    asIterable().toMutableSortedMap(comparator)
