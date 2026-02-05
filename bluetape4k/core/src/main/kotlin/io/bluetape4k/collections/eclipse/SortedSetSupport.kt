package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.SortedSets
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet
import org.eclipse.collections.api.set.sorted.MutableSortedSet
import java.util.stream.Stream

fun <T: Comparable<T>> emptyImmutableSortedSet(): ImmutableSortedSet<T> = SortedSets.immutable.empty<T>()

fun <T: Comparable<T>> emptyMutableSortedSet(): MutableSortedSet<T> = SortedSets.mutable.empty<T>()

fun <T: Comparable<T>> immutableSortedSetOf(vararg elements: T): ImmutableSortedSet<T> =
    if (elements.isEmpty()) emptyImmutableSortedSet()
    else SortedSets.immutable.of<T>(*elements)

fun <T: Comparable<T>> mutableSortedSetOf(vararg elements: T): MutableSortedSet<T> =
    if (elements.isEmpty()) emptyMutableSortedSet()
    else SortedSets.mutable.of<T>(*elements)

inline fun <T: Comparable<T>> mutableSortedSet(
    size: Int = 10,
    @BuilderInference builder: (Int) -> T,
): MutableSortedSet<T> =
    SortedSets.mutable.of<T>().apply {
        repeat(size) {
            add(builder(it))
        }
    }

fun <T: Comparable<T>> Iterable<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this)

fun <T: Comparable<T>> Sequence<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

fun <T: Comparable<T>> Iterator<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

fun <T: Comparable<T>> Array<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())

fun <T: Comparable<T>> Stream<T>.toMutableSortedSet(): MutableSortedSet<T> =
    SortedSets.mutable.ofAll<T>(this.asIterable())
