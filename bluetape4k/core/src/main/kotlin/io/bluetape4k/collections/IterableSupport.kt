package io.bluetape4k.collections

import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.asByte
import io.bluetape4k.support.asChar
import io.bluetape4k.support.asDouble
import io.bluetape4k.support.asFloat
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asLong
import io.bluetape4k.support.asString
import java.util.*

/**
 * 빈 [Iterator] 를 반환합니다.
 */
fun <T> emptyIterator(): Iterator<T> = Collections.emptyIterator()

/**
 * 빈 [ListIterator] 를 반환합니다.
 */
fun <T> emptyListIterator(): ListIterator<T> = Collections.emptyListIterator()

/**
 * [Iterable] 을 [Iterator] 로 변환합니다.
 */
fun <T> Iterator<T>.asIterable(): Iterable<T> = Iterable { this }

/**
 * [Iterable] 을 [List] 로 변환합니다.
 */
fun <T> Iterator<T>.toList(): List<T> =
    mutableListOf<T>().apply { addAll(this@toList.asIterable()) }

/**
 * [Iterable] 을 [MutableList] 로 변환합니다.
 */
fun <T> Iterator<T>.toMutableList(): MutableList<T> =
    mutableListOf<T>().apply { addAll(this@toMutableList.asIterable()) }

/**
 * [Iterable]의 size 를 반환합니다.
 */
fun <T> Iterable<T>.size(): Int = when (this) {
    is Collection<T> -> this.size
    else             -> count()
}

/**
 * [predicate]를 만족하는 [Iterable]의 요소가 있는지 확인합니다.
 */
inline fun <T> Iterable<T>.exists(predicate: (T) -> Boolean): Boolean = any { predicate(it) }

/**
 * Iterable이 다른 iterable과 같은 요소들을 가졌는가를 검사합니다.
 */
infix fun <T> Iterable<T>.isSameElements(that: Iterable<T>): Boolean {
    if (this is List<T> && that is List<T>) {
        if (this.size == that.size) {
            return this.indices.all { this[it] == that[it] }
        }
        return false
    }

    val left = this.iterator()
    val right = that.iterator()
    while (left.hasNext() && right.hasNext()) {
        if (left.next() != right.next()) {
            return false
        }
    }
    return !(left.hasNext() || right.hasNext())
}

/**
 * [Iterable] 을 [CharArray]로 변환합니다.
 */
fun Iterable<*>.asCharArray(dv: Char = '\u0000'): CharArray =
    map { it.asChar(dv) }.toCharArray()

/**
 * [Iterable] 을 [ByteArray]로 변환합니다.
 */
fun Iterable<*>.asByteArray(fallback: Byte = 0): ByteArray =
    map { it.asByte(fallback) }.toByteArray()

/**
 * [Iterable] 을 [IntArray]로 변환합니다.
 */
fun Iterable<*>.asIntArray(fallback: Int = 0): IntArray =
    map { it.asInt(fallback) }.toIntArray()

/**
 * [Iterable] 을 [LongArray]로 변환합니다.
 */
fun Iterable<*>.asLongArray(fallback: Long = 0): LongArray =
    map { it.asLong(fallback) }.toLongArray()

/**
 * [Iterable] 을 [FloatArray]로 변환합니다.
 */
fun Iterable<*>.asFloatArray(fallback: Float = 0.0F): FloatArray =
    map { it.asFloat(fallback) }.toFloatArray()

/**
 * [Iterable] 을 [DoubleArray]로 변환합니다.
 */
fun Iterable<*>.asDoubleArray(fallback: Double = 0.0): DoubleArray =
    map { it.asDouble(fallback) }.toDoubleArray()

/**
 * [Iterable] 을 [String] 배열로 변환합니다.
 */
fun Iterable<*>.asStringArray(fallback: String = EMPTY_STRING): Array<String> =
    map { it.asString(fallback) }.toTypedArray()

/**
 * [Iterable] 을 [T] 수형의 배열로 변환합니다.
 */
inline fun <reified T: Any> Iterable<*>.asArray(): Array<T?> =
    map { it as? T }.toTypedArray()

/**
 * [mapper] 실행의 [Result] 를 반환합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val results = list.tryMap { it / 0 }
 * results.forEach { result ->
 *    result.onSuccess { println("Success: $it") }
 *    result.onFailure { println("Failure: $it") }
 *    result.getOrNull() // null
 *    result.getOrElse { 0 } // 0
 *    result.getOrThrow() // throw ArithmeticException
 * }
 * ```
 *
 * @param mapper 변환 작업
 * @return [Result] 리스트
 * @see mapCatching
 */
inline fun <T, R> Iterable<T>.tryMap(mapper: (T) -> R): List<Result<R>> {
    return map { runCatching { mapper(it) } }
}

/**
 * [mapper] 실행의 [Result] 를 반환합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val results = list.mapCatching { it / 0 }
 * results.forEach { result ->
 *   result.onSuccess { println("Success: $it") }
 *   result.onFailure { println("Failure: $it") }
 *   result.getOrNull() // null
 * }
 * ```
 *
 * @param mapper 변환 작업
 * @return [Result] 리스트
 * @see tryMap
 */
inline fun <T, R> Iterable<T>.mapCatching(mapper: (T) -> R): List<Result<R>> {
    return map { runCatching { mapper(it) } }
}

/**
 * forEach 구문 실행 시 `runCatching` 구문으로 [action] 실행합니다. 예외를 무시합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * list.tryForEach { println(it / 0) }
 * ```
 *
 * @param action 실행할 작업
 * @see forEachCatching
 */
inline fun <T> Iterable<T>.tryForEach(action: (T) -> Unit) {
    forEach { runCatching { action(it) } }
}

/**
 * `runCatching` 구문으로 [action] 실행합니다. action 결과를 [Result]로 반환합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val results = list.forEachCatching { println(it / 0) }
 * results.forEach { result ->
 *   result.onSuccess { println("Success: $it") }
 *   result.onFailure { println("Failure: $it") }
 *   result.getOrNull() // null
 * }
 *
 * @param action 실행할 작업
 * @return [Result] 리스트
 *
 * @see tryForEach
 */
inline fun <T> Iterable<T>.forEachCatching(action: (T) -> Unit): List<Result<Unit>> {
    return map { runCatching { action(it) } }
}

/**
 * [mapper] 실행이 성공한 결과만 추출합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val results = list.mapIfSuccess { it / 0 }
 * println(results) // []
 * ```
 *
 * @param mapper 변환 작업
 * @return 성공한 결과 리스트
 */
inline fun <T, R: Any> Iterable<T>.mapIfSuccess(mapper: (T) -> R): List<R> =
    mapNotNull { runCatching { mapper(it) }.getOrNull() }


/**
 * 컬렉션의 요소를 [size]만큼의 켤렉션으로 묶어서 반환합니다. 마지막 켤렉션의 크기는 [size]보다 작을 수 있습니다.
 *
 * ```
 * val list = listOf(1, 2, 3, 4, 5)
 * val sliding = list.sliding(3)  // [[1, 2, 3], [2, 3, 4], [3, 4, 5], [4, 5], [5]]
 *
 * val slidingExact = list.sliding(3, partialWindows = false)  // [[1, 2, 3], [2, 3, 4], [3, 4, 5]]
 * ```
 *
 * @param size Sliding 요소의 수
 * @return Sliding 된 요소를 담은 컬렉션
 */
fun <T> Iterable<T>.sliding(size: Int, partialWindows: Boolean = true): List<List<T>> =
    windowed(size, 1, partialWindows)

/**
 * 컬렉션의 요소를 [size]만큼의 켤렉션으로 묶은 것을 [transform]으로 변환하여 반환합니다.
 *
 * ```
 * val list = listOf(1, 2, 3, 4, 5)
 * val sliding = list.sliding(3) { it.sum() }  // [6, 9, 12, 9, 5]
 * ```
 *
 * @param size Sliding 요소의 수
 * @param transform 변환 함수
 * @return Sliding 된 요소를 변환한 컬렉션
 */
inline fun <T, R> Iterable<T>.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (List<T>) -> R,
): List<R> =
    windowed(size, 1, partialWindows).map(transform)
