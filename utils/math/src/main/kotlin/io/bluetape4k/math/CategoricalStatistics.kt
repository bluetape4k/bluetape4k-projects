package io.bluetape4k.math

/**
 * 각 요소의 `keySelector`로 추출한 Key 값의 빈도 수를 계산합니다.
 *
 * ```kotlin
 * val data = sequenceOf("apple", "banana", "apple", "cherry")
 * val result = data.countBy { it }
 * // result == {"apple" -> 2, "banana" -> 1, "cherry" -> 1}
 * ```
 *
 * @param T 컬렉션의 요소 타입
 * @param K Key 타입
 * @param keySelector 요소의 구분을 위한 key selector
 * @return Key의 빈도 수
 */
inline fun <T: Any, K: Any> Sequence<T>.countBy(crossinline keySelector: (T) -> K): Map<K, Int> =
    groupingCount(keySelector)

/**
 * 각 요소의 `keySelector`로 추출한 Key 값의 빈도 수를 계산합니다.
 *
 * ```kotlin
 * val data = listOf("apple", "banana", "apple", "cherry")
 * val result = data.countBy { it }
 * // result == {"apple" -> 2, "banana" -> 1, "cherry" -> 1}
 * ```
 *
 * @param T 컬렉션의 요소 타입
 * @param K Key 타입
 * @param keySelector 요소의 구분을 위한 key selector
 * @return Key의 빈도 수
 */
inline fun <T: Any, K: Any> Iterable<T>.countBy(crossinline keySelector: (T) -> K): Map<K, Int> =
    asSequence().countBy(keySelector)

/**
 * 각 요소의 빈도 수를 계산합니다.
 *
 * ```kotlin
 * val data = sequenceOf("a", "b", "a", "c")
 * val result = data.countBy()
 * // result == {"a" -> 2, "b" -> 1, "c" -> 1}
 * ```
 */
fun <T: Any> Sequence<T>.countBy(): Map<T, Int> = countBy { it }

/**
 * 각 요소의 빈도 수를 계산합니다.
 *
 * ```kotlin
 * val data = listOf("a", "b", "a", "c")
 * val result = data.countBy()
 * // result == {"a" -> 2, "b" -> 1, "c" -> 1}
 * ```
 */
fun <T: Any> Iterable<T>.countBy(): Map<T, Int> = countBy { it }


/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val data = sequenceOf(1, 2, 2, 3, 3, 3)
 * val result = data.mode().toList()
 * // result == [3]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T: Any> Sequence<T>.mode(): Sequence<T> =
    countBy()
        .entries
        .sortedByDescending { it.value }
        .let { list ->
            list.asSequence()
                .takeWhile { list[0].value == it.value }
                .map { it.key }
        }

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2, 2, 3, 3, 3).mode().toList()
 * // result == [3]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T: Any> Iterable<T>.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = arrayOf(1, 2, 2, 3, 3, 3).mode().toList()
 * // result == [3]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T: Any> Array<out T>.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = intArrayOf(1, 2, 2, 3, 3, 3).mode().toList()
 * // result == [3]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun IntArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = longArrayOf(1L, 2L, 2L, 3L, 3L, 3L).mode().toList()
 * // result == [3L]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun LongArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = floatArrayOf(1f, 2f, 2f, 3f, 3f, 3f).mode().toList()
 * // result == [3f]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun FloatArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 2.0, 3.0, 3.0, 3.0).mode().toList()
 * // result == [3.0]
 * ```
 *
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun DoubleArray.mode() = asSequence().mode()
