package io.bluetape4k.collections.pod4k

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.buildImmutableArray
import io.bluetape4k.support.requireGt


/**
 * 요소를 지정된 크기로 나누고, 지정된 간격으로 이동하는 윈도우된 [ImmutableArray]를 생성합니다.
 *
 * @param size 윈도우의 크기
 * @param step 윈도우의 이동 간격
 * @param partialWindows 마지막 윈도우가 size 보다 작을 경우에도 포함할지 여부
 */
fun <T> ImmutableArray<T>.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
): ImmutableArray<ImmutableArray<T>> =
    windowed(size, step, partialWindows) { it }

/**
 * 요소를 지정된 크기로 나누고, 지정된 간격으로 이동하는 윈도우된 [ImmutableArray]를 생성합니다.
 */
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
        repeat(size) {
            if (pos + it < this@windowed.size) {
                window.add(this@windowed[pos + it])
            }
        }
        val array = window.build()
        when {
            array.size == size -> add(transform(array))
            partialWindows -> add(transform(array))
        }
        pos += step
    }
}

/**
 * 요소를 지정된 크기로 나누어 새로운 [ImmutableArray]의 [ImmutableArray]를 생성합니다.
 *
 * @param size 윈도우의 크기
 * @param partialWindows 마지막 윈도우가 size 보다 작을 경우에도 포함할지 여부
 */
fun <T> ImmutableArray<T>.chunked(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableArray<T>> =
    windowed(size, size, partialWindows)

/**
 * 요소를 지정된 크기로 나누어 새로운 [ImmutableArray]를 생성합니다.
 */
inline fun <T, R> ImmutableArray<T>.chunked(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableArray<T>) -> R,
): ImmutableArray<R> =
    windowed(size, size, partialWindows, transform)


/**
 * 지정된 크기로 나누고, 한 요소씩 이동하며 새로운 [ImmutableArray]의 [ImmutableArray]를 생성합니다.
 *
 * @param size 윈도우의 크기
 * @param partialWindows 마지막 윈도우가 size 보다 작을 경우에도 포함할지 여부
 */
fun <T> ImmutableArray<T>.sliding(size: Int, partialWindows: Boolean = true): ImmutableArray<ImmutableArray<T>> =
    windowed(size, 1, partialWindows)

/**
 * 지정된 크기로 나누고, 한 요소씩 이동하며 새로운 [ImmutableArray]를 생성합니다.
 */
inline fun <T, R> ImmutableArray<T>.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (ImmutableArray<T>) -> R,
): ImmutableArray<R> =
    windowed(size, 1, partialWindows, transform)
