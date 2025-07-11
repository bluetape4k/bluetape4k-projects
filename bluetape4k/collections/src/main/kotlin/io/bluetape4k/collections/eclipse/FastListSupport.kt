package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.impl.list.mutable.FastList

fun <T> emptyFastList(): FastList<T> = FastList.newList()

inline fun <T> fastList(
    initialCapacity: Int = 10,
    initializer: (index: Int) -> T,
): FastList<T> =
    FastList.newList<T>(initialCapacity).apply {
        repeat(initialCapacity) { index ->
            add(initializer(index))
        }
    }

fun <T> fastListOf(vararg elements: T): FastList<T> = FastList.newListWith<T>(*elements)

fun <T> Iterable<T>.toFastList(): FastList<T> = when (this) {
    is FastList<T> -> this
    else -> FastList.newList(this)
}

fun <T> Sequence<T>.toFastList(): FastList<T> = this.asIterable().toFastList()
fun <T> Iterator<T>.toFastList(): FastList<T> = this.asIterable().toFastList()
fun <T> Array<T>.toFastList(): FastList<T> = this.asIterable().toFastList()
