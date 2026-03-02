package io.bluetape4k.collections.eclipse.ranges

import org.eclipse.collections.impl.list.mutable.primitive.CharArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList

/**
 * toCharArrayList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = ('a'..'c').toCharArrayList()
 * // result contains [a, b, c]
 * ```
 */
fun CharProgression.toCharArrayList(): CharArrayList =
    CharArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }

/**
 * toIntArrayList 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = (1..3).toIntArrayList()
 * // result contains [1, 2, 3]
 * ```
 */
fun IntProgression.toIntArrayList(): IntArrayList =
    IntArrayList().also { array ->
        forEach {
            array.add(it)
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
 * val result = (1L..3L).toLongArrayList()
 * // result contains [1, 2, 3]
 * ```
 */
fun LongProgression.toLongArrayList(): LongArrayList =
    LongArrayList().also { array ->
        forEach {
            array.add(it)
        }
    }
