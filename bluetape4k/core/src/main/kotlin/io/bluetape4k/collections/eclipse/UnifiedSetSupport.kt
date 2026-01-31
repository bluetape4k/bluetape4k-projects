package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Sets
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.set.mutable.UnifiedSet

fun <T> emptyUnifiedSet(): ImmutableSet<T> = Sets.immutable.empty()

inline fun <T> unifiedSet(size: Int, initializer: (Int) -> T): UnifiedSet<T> =
    UnifiedSet.newSet<T>(size).apply {
        repeat(size) {
            add(initializer(it))
        }
    }

fun <T> unifiedSetOf(vararg elements: T): UnifiedSet<T> =
    if (elements.isEmpty()) UnifiedSet.newSet()
    else UnifiedSet.newSetWith<T>(*elements)

fun <T> unifiedSetOf(size: Int): UnifiedSet<T> = UnifiedSet.newSet<T>(size)

fun <T> Iterable<T>.toUnifiedSet(): UnifiedSet<T> = UnifiedSet.newSet(this)
fun <T> Sequence<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Iterator<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Array<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
