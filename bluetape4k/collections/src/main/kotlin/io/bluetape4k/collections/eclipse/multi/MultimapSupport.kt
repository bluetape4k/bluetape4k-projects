package io.bluetape4k.collections.eclipse.multi

import io.bluetape4k.collections.eclipse.toTuplePair
import org.eclipse.collections.api.multimap.Multimap
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap
import org.eclipse.collections.api.multimap.list.MutableListMultimap
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap
import org.eclipse.collections.api.multimap.set.MutableSetMultimap
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap
import org.eclipse.collections.impl.multimap.list.FastListMultimap
import org.eclipse.collections.impl.multimap.set.UnifiedSetMultimap
import org.eclipse.collections.impl.tuple.Tuples

fun <K, V> emptyImmutableListMultimap(): ImmutableListMultimap<K, V> = Multimaps.immutable.list.empty<K, V>()
fun <K, V> emptyImmutableSetMultimap(): ImmutableSetMultimap<K, V> = Multimaps.immutable.set.empty<K, V>()
fun <K, V> emptyImmutableBagMultimap(): ImmutableBagMultimap<K, V> = Multimaps.immutable.bag.empty<K, V>()

fun <K, V> emptyMutableListMultimap(): MutableListMultimap<K, V> = Multimaps.mutable.list.empty<K, V>()
fun <K, V> emptyMutableSetMultimap(): MutableSetMultimap<K, V> = Multimaps.mutable.set.empty<K, V>()
fun <K, V> emptyMutableBagMultimap(): MutableBagMultimap<K, V> = Multimaps.mutable.bag.empty<K, V>()

fun <K, V> listMultimapOf(vararg pairs: Pair<K, V>): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap(pairs.map { it.toTuplePair() })

fun <K, V> setMultimapOf(vararg pairs: Pair<K, V>): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap(pairs.map { it.toTuplePair() })

fun <K, V> bagMultimapOf(vararg pairs: Pair<K, V>): HashBagMultimap<K, V> =
    HashBagMultimap.newMultimap(pairs.map { it.toTuplePair() })

fun <K, V> Map<K, V>.toImmutableListMultimap(): ImmutableListMultimap<K, V> =
    emptyImmutableListMultimap<K, V>().also { this.map { Tuples.pair(it.key, it.value) } }

fun <K, V> Map<K, V>.toListMultimap(): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap(this.map { Tuples.pair(it.key, it.value) })

fun <K, V> Map<K, V>.toImmutableSetMultimap(): ImmutableSetMultimap<K, V> =
    emptyImmutableSetMultimap<K, V>().also { this.map { Tuples.pair(it.key, it.value) } }

fun <K, V> Map<K, V>.toSetMultimap(): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap(this.map { Tuples.pair(it.key, it.value) })

fun <K, V> Map<K, V>.toImmutableBagMultimap(): ImmutableBagMultimap<K, V> =
    emptyImmutableBagMultimap<K, V>().also { this.map { Tuples.pair(it.key, it.value) } }

fun <K, V> Map<K, V>.toBagMultimap(): MutableBagMultimap<K, V> =
    HashBagMultimap.newMultimap(this.map { Tuples.pair(it.key, it.value) })

fun <K, V> Multimap<K, V>.filter(predicate: (K, Iterable<V>) -> Boolean): Multimap<K, V> =
    selectKeysMultiValues { key, values -> predicate(key, values) }

inline fun <K, V, R> Multimap<K, V>.toList(mapper: (K, Iterable<V>) -> R): List<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }

inline fun <K, V, R> Multimap<K, V>.toSet(mapper: (K, Iterable<V>) -> R): Set<R> =
    keyMultiValuePairsView().map { mapper(it.one, it.two) }.toSet()

inline fun <T, K, V> Iterable<T>.toListMultimap(mapper: (T) -> Pair<K, V>): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap(this.map { mapper(it).toTuplePair() })

inline fun <T, K, V> Iterable<T>.toSetMultimap(mapper: (T) -> Pair<K, V>): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap(this.map { mapper(it).toTuplePair() })

inline fun <T, K, V> Iterable<T>.toBagMultimap(mapper: (T) -> Pair<K, V>): MutableBagMultimap<K, V> =
    HashBagMultimap.newMultimap(this.map { mapper(it).toTuplePair() })

inline fun <T, K> Iterable<T>.groupByListMultimap(keySelector: (T) -> K): MutableListMultimap<K, T> =
    toListMultimap { keySelector(it) to it }

inline fun <T, K> Iterable<T>.groupBySetMultimap(keySelector: (T) -> K): MutableSetMultimap<K, T> =
    toSetMultimap { keySelector(it) to it }

inline fun <T, K> Iterable<T>.groupByBagMultimap(keySelector: (T) -> K): MutableBagMultimap<K, T> =
    toBagMultimap { keySelector(it) to it }

fun <K, V> Iterable<Pair<K, V>>.toListMultimap(): MutableListMultimap<K, V> =
    FastListMultimap.newMultimap(this.map { it.toTuplePair() })

fun <K, V> Iterable<Pair<K, V>>.toSetMultimap(): MutableSetMultimap<K, V> =
    UnifiedSetMultimap.newMultimap(this.map { it.toTuplePair() })

fun <K, V> Iterable<Pair<K, V>>.toBagMultimap(): MutableBagMultimap<K, V> =
    HashBagMultimap.newMultimap(this.map { it.toTuplePair() })
