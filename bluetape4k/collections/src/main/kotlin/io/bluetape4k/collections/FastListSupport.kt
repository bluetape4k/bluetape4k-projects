package io.bluetape4k.collections

import io.bluetape4k.support.assertZeroOrPositiveNumber
import org.eclipse.collections.impl.list.mutable.FastList

fun <T> emptyFastList(): FastList<T> = FastList.newList()

inline fun <T> FastList(
    size: Int = 10,
    @BuilderInference initializer: (index: Int) -> T,
): FastList<T> {
    size.assertZeroOrPositiveNumber("size")

    val list = FastList.newList<T>(size)
    repeat(size) { index ->
        list.add(initializer(index))
    }
    return list
}

fun <T> FastList(initialCapacity: Int): FastList<T> = FastList.newList<T>(initialCapacity)
fun <T> fastListOf(vararg elements: T): FastList<T> = FastList.newListWith<T>(*elements)

fun <T> Iterable<T>.toFastList(): FastList<T> = FastList.newList(this)
fun <T> Sequence<T>.toFastList(): FastList<T> = FastList.newList(this.asIterable())
fun <T> Iterator<T>.toFastList(): FastList<T> = FastList.newList(this.asIterable())
fun <T> Array<T>.toFastList(): FastList<T> = FastList.newListWith(*this)
