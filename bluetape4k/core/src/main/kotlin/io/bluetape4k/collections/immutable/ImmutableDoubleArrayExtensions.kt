package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.ImmutableDoubleArray
import com.danrusu.pods4k.immutableArrays.buildImmutableArray
import io.bluetape4k.support.requireGt


fun ImmutableDoubleArray.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): ImmutableArray<ImmutableDoubleArray> =
    windowed(size, step, partialWindows) { it }

inline fun <R> ImmutableDoubleArray.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (ImmutableDoubleArray) -> R,
): ImmutableArray<R> = buildImmutableArray {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    while (pos < this@windowed.size) {
        val window = ImmutableDoubleArray.Builder()
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

fun ImmutableDoubleArray.chunked(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableDoubleArray> =
    windowed(size, size, partialWindows)

inline fun <R> ImmutableDoubleArray.chunked(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableDoubleArray) -> R,
): ImmutableArray<R> =
    windowed(size, size, partialWindows, transform)


fun ImmutableDoubleArray.sliding(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableDoubleArray> =
    windowed(size, 1, partialWindows)


inline fun <R> ImmutableDoubleArray.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableDoubleArray) -> R,
): ImmutableArray<R> =
    windowed(size, 1, partialWindows, transform)
