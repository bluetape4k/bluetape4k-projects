package io.bluetape4k.math


/**
 * 각 요소의 `keySelector`로 추출한 Key 값의 빈도 수를 계산합니다.
 *
 * @param T 컬렉션의 요소 타입
 * @param K Key 타입
 * @param keySelector 요소의 구분을 위한 key selector
 * @return Key의 빈도 수
 */
inline fun <T, K> Sequence<T>.countBy(crossinline keySelector: (T) -> K): Map<K, Int> =
    groupingCount(keySelector)

/**
 * 각 요소의 `keySelector`로 추출한 Key 값의 빈도 수를 계산합니다.
 *
 * @param T 컬렉션의 요소 타입
 * @param K Key 타입
 * @param keySelector 요소의 구분을 위한 key selector
 * @return Key의 빈도 수
 */
inline fun <T, K> Iterable<T>.countBy(crossinline keySelector: (T) -> K): Map<K, Int> =
    asSequence().countBy(keySelector)

/**
 *  각 요소의 빈도 수를 계산합니다.
 */
fun <T> Sequence<T>.countBy(): Map<T, Int> = countBy { it }

/**
 *  각 요소의 빈도 수를 계산합니다.
 */
fun <T> Iterable<T>.countBy(): Map<T, Int> = countBy { it }


/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 *
 * @param T
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T> Sequence<T>.mode(): Sequence<T> =
    countBy()
        .entries
        .sortedByDescending { it.value }
        .toList()
        .let { list ->
            list.asSequence()
                .takeWhile { list[0].value == it.value }
                .map { it.key }
        }

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T> Iterable<T>.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun <T> Array<out T>.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun IntArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun LongArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun FloatArray.mode() = asSequence().mode()

/**
 * 가장 많은 빈도를 나타내는 요소를 추출합니다.
 * @return 컬렉션 요소 중 빈도수가 가장 높은 요소들을 추출합니다.
 */
fun DoubleArray.mode() = asSequence().mode()
