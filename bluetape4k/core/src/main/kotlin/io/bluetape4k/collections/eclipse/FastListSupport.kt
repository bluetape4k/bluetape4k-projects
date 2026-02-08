@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.impl.list.mutable.FastList
import java.util.stream.Stream


inline fun <T> emptyFastList(): FastList<T> = FastList.newList<T>()

inline fun <T> fastList(
    size: Int = 16,
    @BuilderInference initializer: (index: Int) -> T,
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

fun <T> Iterable<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> {
    when (this) {
        is Collection -> {
            if (size == 1) {
                destination.add(if (this is List) get(0) else iterator().next())
            } else if (size > 1) {
                destination.addAll(this@toFastList)
            }
        }
        else -> {
            destination.addAll(this@toFastList)
        }
    }
    return destination
}

fun <T> Sequence<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

fun <T> Iterator<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

fun <T> Array<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

fun <T> Stream<T>.toFastList(destination: FastList<T> = FastList.newList()): FastList<T> =
    asIterable().toFastList(destination)

fun <T, R> Iterable<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = toFastList(destination).collect { transform(it) }

fun <T, R> Sequence<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)

fun <T, R> Iterator<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)

fun <T, R> Array<T>.fastMap(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMap(destination, transform)


fun <T, R: Any> Iterable<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    mapper: (T) -> R?,
): FastList<R> = toFastList(destination).collectIf<R>({ it != null }) { mapper(it) }

fun <T, R: Any> Sequence<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)

fun <T, R: Any> Iterator<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)

fun <T, R: Any> Array<T>.fastMapNotNull(
    destination: FastList<T> = FastList.newList(),
    transform: (T) -> R,
): FastList<R> = asIterable().fastMapNotNull(destination, transform)


fun <T> Iterable<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = toFastList(destination).select { predicate(it) }

fun <T> Sequence<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)

fun <T> Iterator<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)

fun <T> Array<T>.fastFilter(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilter(destination, predicate)


fun <T> Iterable<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = toFastList(destination).select { !predicate(it) }

fun <T> Sequence<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

fun <T> Iterator<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

fun <T> Array<T>.fastFilterNot(
    destination: FastList<T> = FastList.newList(),
    predicate: (T) -> Boolean,
): FastList<T> = asIterable().fastFilterNot(destination, predicate)

inline fun <T> FastList<T>?.orEmpty(): FastList<T> =
    this ?: emptyFastList()
