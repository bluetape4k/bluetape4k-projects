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
 *
 * ```kotlin
 * val iter: Iterator<String> = emptyIterator()
 * iter.hasNext() // false
 * ```
 *
 * @return 빈 [Iterator] 인스턴스
 */
fun <T> emptyIterator(): Iterator<T> = Collections.emptyIterator()

/**
 * 빈 [ListIterator] 를 반환합니다.
 *
 * ```kotlin
 * val iter: ListIterator<Int> = emptyListIterator()
 * iter.hasNext()     // false
 * iter.hasPrevious() // false
 * ```
 *
 * @return 빈 [ListIterator] 인스턴스
 */
fun <T> emptyListIterator(): ListIterator<T> = Collections.emptyListIterator()

/**
 * [Iterator] 를 [Iterable] 로 변환합니다.
 *
 * ```kotlin
 * val iter = listOf(1, 2, 3).iterator()
 * val iterable: Iterable<Int> = iter.asIterable()
 * iterable.toList() // [1, 2, 3]
 * ```
 *
 * @return [Iterable] 인스턴스
 */
fun <T> Iterator<T>.asIterable(): Iterable<T> = Iterable { this }

/**
 * [Iterator] 를 [List] 로 변환합니다.
 *
 * ```kotlin
 * val iter = listOf("a", "b", "c").iterator()
 * val list: List<String> = iter.toList() // ["a", "b", "c"]
 * ```
 *
 * @return [List] 인스턴스
 */
fun <T> Iterator<T>.toList(): List<T> = asIterable().toList()

/**
 * [Iterator] 를 [MutableList] 로 변환합니다.
 *
 * ```kotlin
 * val iter = listOf(10, 20, 30).iterator()
 * val mutable: MutableList<Int> = iter.toMutableList()
 * mutable.add(40)
 * mutable // [10, 20, 30, 40]
 * ```
 *
 * @return [MutableList] 인스턴스
 */
fun <T> Iterator<T>.toMutableList(): MutableList<T> = asIterable().toMutableList()

/**
 * [Iterable]의 size 를 반환합니다.
 * [Collection] 이면 O(1), 그 외에는 O(n) 으로 순회하여 개수를 셉니다.
 *
 * ```kotlin
 * listOf(1, 2, 3).size()              // 3
 * sequenceOf(1, 2, 3).asIterable().size() // 3
 * ```
 *
 * @return 요소의 개수
 */
fun <T> Iterable<T>.size(): Int = when (this) {
    is Collection<T> -> this.size
    else -> count()
}

/**
 * [predicate]를 만족하는 [Iterable]의 요소가 있는지 확인합니다.
 *
 * ```kotlin
 * listOf(1, 2, 3).exists { it > 2 } // true
 * listOf(1, 2, 3).exists { it > 5 } // false
 * ```
 *
 * @param predicate 조건 함수
 * @return 조건을 만족하는 요소가 있으면 `true`
 */
inline fun <T> Iterable<T>.exists(predicate: (T) -> Boolean): Boolean = any { predicate(it) }

/**
 * Iterable이 다른 iterable과 같은 요소들을 가졌는가를 검사합니다.
 *
 * ```kotlin
 * listOf(1, 2, 3) isSameElements listOf(1, 2, 3) // true
 * listOf(1, 2, 3) isSameElements listOf(3, 2, 1) // false (순서 다름)
 * listOf(1, 2)    isSameElements listOf(1, 2, 3) // false (크기 다름)
 * ```
 *
 * @param that 비교할 [Iterable]
 * @return 순서까지 포함하여 동일한 요소를 가지면 `true`
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
 * 변환할 수 없는 요소는 [dv] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf('A', 'B', 'C').asCharArray()           // charArrayOf('A', 'B', 'C')
 * listOf("X", null, 'Z').asCharArray(dv = '?')  // charArrayOf('X', '?', 'Z')
 * ```
 *
 * @param dv 변환 실패 시 대체할 기본값 (기본: `'\u0000'`)
 * @return [CharArray] 인스턴스
 */
fun Iterable<*>.asCharArray(dv: Char = '\u0000'): CharArray =
    map { it.asChar(dv) }.toCharArray()

/**
 * [Iterable] 을 [ByteArray]로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf(1, 2, 3).asByteArray()               // byteArrayOf(1, 2, 3)
 * listOf(1, null, 3).asByteArray(fallback = 0) // byteArrayOf(1, 0, 3)
 * ```
 *
 * @param fallback 변환 실패 시 대체할 기본값 (기본: `0`)
 * @return [ByteArray] 인스턴스
 */
fun Iterable<*>.asByteArray(fallback: Byte = 0): ByteArray =
    map { it.asByte(fallback) }.toByteArray()

/**
 * [Iterable] 을 [IntArray]로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf(10, 20, 30).asIntArray()                // intArrayOf(10, 20, 30)
 * listOf(10, null, 30).asIntArray(fallback = -1) // intArrayOf(10, -1, 30)
 * ```
 *
 * @param fallback 변환 실패 시 대체할 기본값 (기본: `0`)
 * @return [IntArray] 인스턴스
 */
fun Iterable<*>.asIntArray(fallback: Int = 0): IntArray =
    map { it.asInt(fallback) }.toIntArray()

/**
 * [Iterable] 을 [LongArray]로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf(100L, 200L, 300L).asLongArray()                  // longArrayOf(100, 200, 300)
 * listOf(100L, null, 300L).asLongArray(fallback = -1L)    // longArrayOf(100, -1, 300)
 * ```
 *
 * @param fallback 변환 실패 시 대체할 기본값 (기본: `0`)
 * @return [LongArray] 인스턴스
 */
fun Iterable<*>.asLongArray(fallback: Long = 0): LongArray =
    map { it.asLong(fallback) }.toLongArray()

/**
 * [Iterable] 을 [FloatArray]로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf(1.0f, 2.5f, 3.0f).asFloatArray()                   // floatArrayOf(1.0f, 2.5f, 3.0f)
 * listOf(1.0f, null, 3.0f).asFloatArray(fallback = Float.NaN) // floatArrayOf(1.0f, NaN, 3.0f)
 * ```
 *
 * @param fallback 변환 실패 시 대체할 기본값 (기본: `0.0F`)
 * @return [FloatArray] 인스턴스
 */
fun Iterable<*>.asFloatArray(fallback: Float = 0.0F): FloatArray =
    map { it.asFloat(fallback) }.toFloatArray()

/**
 * [Iterable] 을 [DoubleArray]로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf(1.0, 2.5, 3.0).asDoubleArray()                       // doubleArrayOf(1.0, 2.5, 3.0)
 * listOf(1.0, null, 3.0).asDoubleArray(fallback = Double.NaN) // doubleArrayOf(1.0, NaN, 3.0)
 * ```
 *
 * @param fallback 변환 실패 시 대체할 기본값 (기본: `0.0`)
 * @return [DoubleArray] 인스턴스
 */
fun Iterable<*>.asDoubleArray(fallback: Double = 0.0): DoubleArray =
    map { it.asDouble(fallback) }.toDoubleArray()

/**
 * [Iterable] 을 [String] 배열로 변환합니다.
 * 변환할 수 없는 요소는 [fallback] 기본값으로 대체됩니다.
 *
 * ```kotlin
 * listOf("hello", 42, true).asStringArray()              // arrayOf("hello", "42", "true")
 * listOf("hello", null, true).asStringArray(fallback = "") // arrayOf("hello", "", "true")
 * ```
 *
 * @param fallback 변환 실패(null 포함) 시 대체할 기본값 (기본: 빈 문자열)
 * @return [Array]<[String]> 인스턴스
 */
fun Iterable<*>.asStringArray(fallback: String = EMPTY_STRING): Array<String> =
    map { it.asString(fallback) }.toTypedArray()

/**
 * [Iterable] 을 [T] 수형의 배열로 변환합니다.
 * 캐스팅에 실패한 요소는 `null` 로 포함됩니다.
 *
 * ```kotlin
 * val mixed: List<Any> = listOf("a", 1, "b", 2)
 * val strings: Array<String?> = mixed.asArray<String>() // ["a", null, "b", null]
 * ```
 *
 * @return 타입 캐스팅된 [Array]<[T]?> 인스턴스 (캐스팅 실패 시 null 포함)
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
@Deprecated("Use mapCatching", ReplaceWith("mapCatching(mapper)"))
inline fun <T, R> Iterable<T>.tryMap(mapper: (T) -> R): List<Result<R>> = mapCatching(mapper)

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
inline fun <T, R> Iterable<T>.mapCatching(mapper: (T) -> R): List<Result<R>> =
    map { runCatching { mapper(it) } }

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
inline fun <T> Iterable<T>.forEachCatching(action: (T) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * [mapper] 실행이 성공한 결과만 추출합니다.
 *
 * ```
 * val list = listOf(1, 2, 3)
 * val results = list.mapIfSuccess { it / 0 }
 * // results == []
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
