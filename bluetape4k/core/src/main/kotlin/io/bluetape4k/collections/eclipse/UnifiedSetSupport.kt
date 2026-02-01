package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.impl.set.mutable.UnifiedSet

fun <T> emptyUnifiedSet(): UnifiedSet<T> = UnifiedSet.newSet<T>()

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

fun <T> Iterable<T>.toUnifiedSet(): UnifiedSet<T> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyUnifiedSet()
            1 -> unifiedSet(1) { if (this is List) get(0) else iterator().next() }
            else -> UnifiedSet.newSet(this)
        }
    }
    return UnifiedSet.newSet<T>(this@toUnifiedSet)
}

fun <T> Sequence<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Iterator<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
fun <T> Array<T>.toUnifiedSet(): UnifiedSet<T> = this.asIterable().toUnifiedSet()
