package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Sets
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.set.mutable.UnifiedSet

fun <T> emptyUnifiedSet(): ImmutableSet<T> = Sets.immutable.empty()

inline fun <T> UnifiedSet(size: Int, initializer: (Int) -> T): UnifiedSet<T> =
    UnifiedSet.newSet<T>(size).apply {
        forEachIndexed { index, _ ->
            add(initializer(index))
        }
    }

fun <T> unifiedSetOf(vararg elements: T): UnifiedSet<T> = UnifiedSet.newSetWith<T>(*elements)
fun <T> unifiedSetOf(size: Int): UnifiedSet<T> = UnifiedSet.newSet<T>(size)

fun <T> Iterable<T>.toUnifiedSet(): UnifiedSet<T> = when (this) {
    is UnifiedSet<T> -> this
    else -> UnifiedSet.newSet(this)
}

fun <T> Sequence<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Iterator<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Array<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
