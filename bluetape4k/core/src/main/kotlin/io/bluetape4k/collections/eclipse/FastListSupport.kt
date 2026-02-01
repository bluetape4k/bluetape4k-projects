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

fun <T> fastListOf(vararg elements: T): FastList<T> {
    return if (elements.isEmpty()) FastList.newList<T>()
    else FastList.newListWith<T>(*elements)
}

fun <T> Iterable<T>.toFastList(): FastList<T> {
    if (this is Collection) {
        return when (size) {
            0 -> emptyFastList()
            1 -> fastList(1) { if (this is List) get(0) else iterator().next() }
            else -> FastList.newList(this)
        }
    }
    return FastList.newList<T>(this@toFastList)
}

fun <T> Sequence<T>.toFastList(): FastList<T> = asIterable().toFastList()
fun <T> Iterator<T>.toFastList(): FastList<T> = asIterable().toFastList()
fun <T> Array<T>.toFastList(): FastList<T> = asIterable().toFastList()

fun <T, R> Iterable<T>.fastMap(transform: (T) -> R): FastList<R> = toFastList().collect { transform(it) }
fun <T, R> Sequence<T>.fastMap(transform: (T) -> R): FastList<R> = asIterable().fastMap(transform)
fun <T, R> Iterator<T>.fastMap(transform: (T) -> R): FastList<R> = asIterable().fastMap(transform)
fun <T, R> Array<T>.fastMap(transform: (T) -> R): FastList<R> = asIterable().fastMap(transform)


fun <T, R: Any> Iterable<T>.fastMapNotNull(mapper: (T) -> R?): FastList<R> =
    toFastList().collectIf<R>({ it != null }) { mapper(it) }

fun <T, R: Any> Sequence<T>.fastMapNotNull(transform: (T) -> R): FastList<R> = asIterable().fastMapNotNull(transform)
fun <T, R: Any> Iterator<T>.fastMapNotNull(transform: (T) -> R): FastList<R> = asIterable().fastMapNotNull(transform)
fun <T, R: Any> Array<T>.fastMapNotNull(transform: (T) -> R): FastList<R> = asIterable().fastMapNotNull(transform)


fun <T> Iterable<T>.fastFilter(predicate: (T) -> Boolean): FastList<T> = toFastList().select { predicate(it) }
fun <T> Sequence<T>.fastFilter(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilter(predicate)
fun <T> Iterator<T>.fastFilter(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilter(predicate)
fun <T> Array<T>.fastFilter(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilter(predicate)


fun <T> Iterable<T>.fastFilterNot(predicate: (T) -> Boolean): FastList<T> = toFastList().select { !predicate(it) }
fun <T> Sequence<T>.fastFilterNot(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilterNot(predicate)
fun <T> Iterator<T>.fastFilterNot(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilterNot(predicate)
fun <T> Array<T>.fastFilterNot(predicate: (T) -> Boolean): FastList<T> = asIterable().fastFilterNot(predicate)
