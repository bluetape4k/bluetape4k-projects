package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.buildImmutableArray
import io.bluetape4k.support.requireGt

fun <T> ImmutableArray<T>.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): ImmutableArray<ImmutableArray<T>> =
    windowed(size, step, partialWindows) { it }

inline fun <T, R> ImmutableArray<T>.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (ImmutableArray<T>) -> R,
): ImmutableArray<R> = buildImmutableArray {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    while (pos < this@windowed.size) {
        val window = ImmutableArray.Builder<T>()
        for (i in 0 until size) {
            if (pos + i < this@windowed.size) {
                window.add(this@windowed[pos + i])
            }
        }
        val array = window.build()
        when {
            array.size == size -> add(transform(array))
            partialWindows     -> add(transform(array))
        }
        pos += step
    }
}

fun <T> ImmutableArray<T>.chunked(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableArray<T>> =
    windowed(size, size, partialWindows)

inline fun <T, R> ImmutableArray<T>.chunked(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableArray<T>) -> R,
): ImmutableArray<R> =
    windowed(size, size, partialWindows, transform)


fun <T> ImmutableArray<T>.sliding(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableArray<T>> =
    windowed(size, 1, partialWindows)


inline fun <T, R> ImmutableArray<T>.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableArray<T>) -> R,
): ImmutableArray<R> =
    windowed(size, 1, partialWindows, transform)
