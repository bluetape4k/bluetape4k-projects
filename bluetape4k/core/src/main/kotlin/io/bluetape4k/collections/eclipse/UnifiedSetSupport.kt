package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.set.FixedSizeSet
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.set.mutable.UnifiedSet

fun <T> emptyFixedSet(): FixedSizeSet<T> = Sets.fixedSize.empty<T>()

fun <T> emptyUnifiedSet(): UnifiedSet<T> = UnifiedSet.newSet<T>()

inline fun <T> unifiedSet(
    size: Int = 16,
    @BuilderInference initializer: (Int) -> T,
): UnifiedSet<T> =
    UnifiedSet.newSet<T>(size).apply {
        repeat(size) {
            add(initializer(it))
        }
    }

fun <T> unifiedSetOf(vararg elements: T): UnifiedSet<T> =
    if (elements.isEmpty()) emptyUnifiedSet()
    else UnifiedSet.newSetWith<T>(*elements)

fun <T> unifiedSetOf(size: Int): UnifiedSet<T> = UnifiedSet.newSet<T>(size)

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

fun <T> Sequence<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)

fun <T> Iterator<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)

fun <T> Array<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet()): UnifiedSet<T> =
    this.asIterable().toUnifiedSet(destination)
