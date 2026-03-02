package io.bluetape4k.collections.eclipse.ranges

import io.bluetape4k.support.requireGt
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.list.primitive.IntInterval
import org.eclipse.collections.impl.list.primitive.LongInterval

/**
 * intIntervalOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::intIntervalOf
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
fun intIntervalOf(start: Int, endInclusive: Int, step: Int = 1): IntInterval =
    IntInterval.fromToBy(start, endInclusive, step)

/**
 * toIntArrayList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toIntArrayList
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
fun IntInterval.toIntArrayList(): IntArrayList =
    IntArrayList(size()).also { array ->
        forEach { element ->
            array.add(element)
        }
    }

/**
 * toLongArrayList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toLongArrayList
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
fun IntInterval.toLongArrayList(): LongArrayList =
    LongArrayList(size()).also { array ->
        forEach { element ->
            array.add(element.toLong())
        }
    }

/**
 * forEach 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::forEach
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun IntInterval.forEach(crossinline block: (Int) -> Unit) {
    this.each { block(it) }
}

/**
 * windowed 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::windowed
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun IntInterval.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> = sequence {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    val intervalSize = this@windowed.size()
    while (pos < intervalSize) {
        val remaining = intervalSize - pos
        val window = IntArrayList(minOf(size, remaining))
        repeat(size) {
            if (pos + it < intervalSize) {
                window.add(this@windowed[pos + it])
            }
        }
        when {
            window.size() == size -> yield(transform(window))
            partialWindows -> yield(transform(window))
        }
        pos += step
    }
}

/**
 * chunked 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::chunked
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun IntInterval.chunked(
    chunkSize: Int,
    partialWindows: Boolean = true,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> =
    windowed(chunkSize, chunkSize, partialWindows, transform)

/**
 * sliding 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::sliding
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun IntInterval.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (IntArrayList) -> IntArrayList = { it },
): Sequence<IntArrayList> =
    windowed(size, 1, partialWindows, transform)

/**
 * longIntervalOf 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::longIntervalOf
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
fun longIntervalOf(start: Long, endInclusive: Long, step: Long = 1): LongInterval =
    LongInterval.fromToBy(start, endInclusive, step)

/**
 * toLongArrayList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::toLongArrayList
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
fun LongInterval.toLongArrayList(): LongArrayList =
    LongArrayList(size()).also { array ->
        forEach {
            array.add(it)
        }
    }

/**
 * forEach 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::forEach
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun LongInterval.forEach(crossinline block: (Long) -> Unit) {
    this.each { block(it) }
}

/**
 * windowed 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::windowed
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun LongInterval.windowed(
    size: Int,
    step: Int = 1,
    partialWindows: Boolean = false,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> = sequence {
    size.requireGt(0, "size")
    step.requireGt(0, "step")

    var pos = 0
    val intervalSize = this@windowed.size()
    while (pos < intervalSize) {
        val remaining = intervalSize - pos
        val window = LongArrayList(minOf(size, remaining))
        repeat(size) {
            if (pos + it < intervalSize) {
                window.add(this@windowed[pos + it])
            }
        }
        when {
            window.size() == size -> yield(transform(window))
            partialWindows -> yield(transform(window))
        }
        pos += step
    }
}

/**
 * chunked 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::chunked
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun LongInterval.chunked(
    chunkSize: Int,
    partialWindows: Boolean = true,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> =
    windowed(chunkSize, chunkSize, partialWindows, transform)

/**
 * sliding 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::sliding
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun LongInterval.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (LongArrayList) -> LongArrayList = { it },
): Sequence<LongArrayList> =
    windowed(size, 1, partialWindows, transform)
