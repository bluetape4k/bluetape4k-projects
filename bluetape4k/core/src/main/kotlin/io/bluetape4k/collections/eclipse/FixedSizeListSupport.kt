package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.asIterable
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.list.FixedSizeList
import java.util.stream.Stream

fun <T> emptyFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.empty<T>()

inline fun <T> fixedSizeList(
    size: Int = 16,
    @BuilderInference builder: (index: Int) -> T,
): FixedSizeList<T> =
    fastList(size, builder).toFixedSizeList()

fun <T> fixedSizeListOf(vararg elements: T): FixedSizeList<T> {
    return if (elements.isEmpty()) Lists.fixedSize.empty<T>()
    else Lists.fixedSize.of<T>(*elements)
}

fun <T> Iterable<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this)
fun <T> Sequence<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())
fun <T> Iterator<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())
fun <T> Array<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())
fun <T> Stream<T>.toFixedSizeList(): FixedSizeList<T> = Lists.fixedSize.ofAll<T>(this.asIterable())
