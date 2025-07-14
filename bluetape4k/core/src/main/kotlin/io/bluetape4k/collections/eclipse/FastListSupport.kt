package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.impl.list.mutable.FastList

fun <T> emptyFastList(): FastList<T> = FastList.newList()

inline fun <T> fastList(
    size: Int = 10,
    initializer: (index: Int) -> T,
): FastList<T> =
    FastList.newList<T>(size).apply {
        repeat(size) { index ->
            add(initializer(index))
        }
    }

fun <T> fastListOf(vararg elements: T): FastList<T> = FastList.newListWith<T>(*elements)

fun <T> Iterable<T>.toFastList(): FastList<T> = FastList.newList(this)
fun <T> Sequence<T>.toFastList(): FastList<T> = this.asIterable().toFastList()
fun <T> Iterator<T>.toFastList(): FastList<T> = this.asIterable().toFastList()
fun <T> Array<T>.toFastList(): FastList<T> = this.asIterable().toFastList()

fun <T> Iterable<T>.asFastList(): FastList<T> = when (this) {
    is FastList -> this
    else -> toFastList()
}

fun <T, R> Iterable<T>.fastMap(transform: (T) -> R): FastList<R> =
    toFastList().collect { transform(it) }
