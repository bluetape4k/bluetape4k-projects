package io.bluetape4k.collections

import io.bluetape4k.support.asByte
import io.bluetape4k.support.asChar
import io.bluetape4k.support.asDouble
import io.bluetape4k.support.asFloat
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asLong
import io.bluetape4k.support.asString
import io.bluetape4k.support.requireLe


/**
 * [start] 부터 [endInclusive] 까지의 [Char] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = charSequenceOf('a', 'z')  // a, b, c, ..., z
 * ```
 *
 * @param start 시작 문자
 * @param endInclusive 종료 문자
 * @param step 증가 값 (기본값: 1)
 * @return 생성된 `Sequence<Char>` 객체
 */
fun charSequenceOf(start: Char, endInclusive: Char, step: Int = 1): Sequence<Char> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = current + step
        if (next <= endInclusive) next else null
    }
}

/**
 * [start] 부터 [endInclusive] 까지의 [Byte] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = byteSequenceOf(0, 10)  // 0, 1, 2, ..., 10
 * ```
 *
 * @param start 시작 바이트
 * @param endInclusive 종료 바이트
 * @param step 증가 값 (기본값: 1)
 * @return 생성된 `Sequence<Byte>` 객체
 */
fun byteSequenceOf(start: Byte, endInclusive: Byte, step: Byte = 1): Sequence<Byte> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = (current + step).toByte()
        if (next <= endInclusive) next else null
    }
}

/**
 * [start] 부터 [endInclusive] 까지의 [Int] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = intSequenceOf(0, 10)  // 0, 1, 2, ..., 10
 * ```
 *
 * @param start 시작 정수
 * @param endInclusive 종료 정수
 * @param step 증가 값 (기본값: 1)
 * @return 생성된 `Sequence<Int>` 객체
 */
fun intSequenceOf(start: Int, endInclusive: Int, step: Int = 1): Sequence<Int> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = current + step
        if (next <= endInclusive) next else null
    }
}

/**
 * [start] 부터 [endInclusive] 까지의 [Long] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = longSequenceOf(0L, 10L)  // 0, 1, 2, ..., 10
 * ```
 *
 * @param start 시작 정수
 * @param endInclusive 종료 정수
 * @param step 증가 값 (기본값: 1)
 * @return 생성된 `Sequence<Long>` 객체
 */
fun longSequenceOf(start: Long, endInclusive: Long, step: Long = 1L): Sequence<Long> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = current + step
        if (next <= endInclusive) next else null
    }
}

/**
 * [start] 부터 [endInclusive] 까지의 [Float] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = floatSequenceOf(0.0F, 10.0F)  // 0.0, 1.0, 2.0, ..., 10.0
 * ```
 *
 * @param start 시작 실수
 * @param endInclusive 종료 실수
 * @param step 증가 값 (기본값: 1.0)
 * @return 생성된 `Sequence<Float>` 객체
 */
fun floatSequenceOf(start: Float, endInclusive: Float, step: Float = 1.0F): Sequence<Float> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = current + step
        if (next <= endInclusive) next else null
    }
}

/**
 * [start] 부터 [endInclusive] 까지의 [Double] 수형의 [Sequence]를 생성합니다.
 *
 * ```
 * val sequence = doubleSequenceOf(0.0, 10.0)  // 0.0, 1.0, 2.0, ..., 10.0
 * ```
 *
 * @param start 시작 실수
 * @param endInclusive 종료 실수
 * @param step 증가 값 (기본값: 1.0)
 * @return 생성된 `Sequence<Double>` 객체
 */
fun doubleSequenceOf(start: Double, endInclusive: Double, step: Double = 1.0): Sequence<Double> {
    start.requireLe(endInclusive, "start")

    return generateSequence(start) { current ->
        val next = current + step
        if (next <= endInclusive) next else null
    }
}

/**
 * Char Sequence 를 [CharArray]로 변환합니다.
 */
fun Sequence<Char>.toCharArray(): CharArray = toList().toCharArray()

/**
 * Byte Sequence 를 [ByteArray]로 변환합니다.
 */
fun Sequence<Byte>.toByteArray(): ByteArray = toList().toByteArray()

/**
 * Short Sequence 를 [ShortArray]로 변환합니다.
 */
fun Sequence<Short>.toShortArray(): ShortArray = toList().toShortArray()

/**
 * Int Sequence 를 [IntArray]로 변환합니다.
 */
fun Sequence<Int>.toIntArray(): IntArray = toList().toIntArray()

/**
 * Long Sequence 를 [LongArray]로 변환합니다.
 */
fun Sequence<Long>.toLongArray(): LongArray = toList().toLongArray()

/**
 * Float Sequence 를 [FloatArray]로 변환합니다.
 */
fun Sequence<Float>.toFloatArray(): FloatArray = toList().toFloatArray()

/**
 * Double Sequence 를 [DoubleArray]로 변환합니다.
 */
fun Sequence<Double>.toDoubleArray(): DoubleArray = toList().toDoubleArray()

/**
 * Sequence의 요소를 [Char]로 변환하여 [CharArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf('a', 'b', 'c')
 * val charArray = sequence.asCharArray()  // ['a', 'b', 'c']
 * ```
 */
fun Sequence<*>.asCharArray(dv: Char = '\u0000'): CharArray = map { it.asChar(dv) }.toCharArray()

/**
 * Sequence의 요소를 [Byte]로 변환하여 [ByteArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val byteArray = sequence.asByteArray()  // [0, 1, 2]
 * ```
 */
fun Sequence<*>.asByteArray(fallback: Byte = 0): ByteArray = map { it.asByte(fallback) }.toByteArray()

/**
 * Sequence의 요소를 [Short]로 변환하여 [ShortArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val shortArray = sequence.asShortArray()  // [0, 1, 2]
 * ```
 */
fun Sequence<*>.asIntArray(fallback: Int = 0): IntArray = map { it.asInt(fallback) }.toIntArray()

/**
 * Sequence의 요소를 [Long]로 변환하여 [LongArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val longArray = sequence.asLongArray()  // [0, 1, 2]
 * ```
 */
fun Sequence<*>.asLongArray(fallback: Long = 0): LongArray = map { it.asLong(fallback) }.toLongArray()

/**
 * Sequence의 요소를 [Float]로 변환하여 [FloatArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0.0, 1.0, 2.0)
 * val floatArray = sequence.asFloatArray()  // [0.0, 1.0, 2.0]
 * ```
 */
fun Sequence<*>.asFloatArray(fallback: Float = 0.0F): FloatArray = map { it.asFloat(fallback) }.toFloatArray()

/**
 * Sequence의 요소를 [Double]로 변환하여 [DoubleArray] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0.0, 1.0, 2.0)
 * val doubleArray = sequence.asDoubleArray()  // [0.0, 1.0, 2.0]
 * ```
 */
fun Sequence<*>.asDoubleArray(fallback: Double = 0.0): DoubleArray = map { it.asDouble(fallback) }.toDoubleArray()

/**
 * Sequence의 요소를 [String]으로 변환하여 [Array] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val stringArray = sequence.asStringArray()  // ["0", "1", "2"]
 * ```
 */
fun Sequence<*>.asStringArray(fallback: String = ""): Array<String> =
    map { it.asString(fallback) }.toList().toTypedArray()

/**
 * Sequence의 요소를 [T]로 변환하여 [Array] 로 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val array: Array<Int> = sequence.asArray<Int>()  // [0, 1, 2]
 * ```
 */
inline fun <reified T: Any> Sequence<*>.asArray(): Array<T?> = map { it as? T }.toList().toTypedArray()

/**
 * [mapper] 실행의 [Result] 를 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val result = sequence.tryMap { it / it }  // [Failure(ArithmeticException), Success(1), Success(1)]
 * ```
 *
 * @param mapper 변환 작업
 * @return 변환된 결과를 담은 `Sequence<Result<R>>` 객체
 */
inline fun <T, R> Sequence<T>.tryMap(crossinline mapper: (T) -> R): Sequence<Result<R>> =
    map { runCatching { mapper(it) } }

/**
 * [mapper] 실행이 성공한 결과만 추출합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val result = sequence.mapIfSuccess { it / it }  // [1, 1]
 * ```
 *
 * @param mapper 변환 작업
 * @return 변환된 결과를 담은 `Sequence<R>` 객체
 */
inline fun <T, R: Any> Sequence<T>.mapIfSuccess(crossinline mapper: (T) -> R): Sequence<R> =
    mapNotNull { runCatching { mapper(it) }.getOrNull() }

/**
 * [action] 실행을 [runCatching]으로 감싸서 수행합니다. 예외가 발생해도 다음 요소를 계속 수행합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * sequence.tryForEach { println(10 / it) }  // err, 10, 5
 * ```
 */
inline fun <T> Sequence<T>.tryForEach(action: (T) -> Unit) {
    forEach { runCatching { action(it) } }
}

/**
 * [mapper] 실행의 [Result] 를 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * val result = sequence.mapCatching { it / it }  // [Failure(ArithmeticException), Success(1), Success(1)]
 * ```
 *
 * @param mapper 변환 작업
 * @return 변환된 결과를 담은 `Sequence<Result<R>>` 객체
 *
 * @see tryMap
 * @see forEachCatching
 */
inline fun <T, R> Sequence<T>.mapCatching(crossinline mapper: (T) -> R): Sequence<Result<R>> =
    map { runCatching { mapper(it) } }

/**
 * [action] 실행을 [runCatching]으로 감싸서 수행합니다. 예외가 발생해도 다음 요소를 계속 수행합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2)
 * sequence.forEachCatching { println(10 / it) }  // err, 10, 5
 * ```
 *
 * @param action 수행 작업
 * @return 수행 결과를 담은 `Sequence<Result<Unit>>` 객체
 *
 * @see mapCatching
 */
inline fun <T> Sequence<T>.forEachCatching(crossinline action: (T) -> Unit): Sequence<Result<Unit>> {
    return map { runCatching { action(it) } }
}


/**
 * 컬렉션의 요소를 [size]만큼의 켤렉션으로 묶어서 반환합니다. 마지막 켤렉션의 크기는 [size]보다 작을 수 있습니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2, 3, 4)
 * val sliding = sequence.sliding(3)  // [[0, 1, 2], [1, 2, 3], [2, 3, 4], [3, 4], [4]]
 * val slidingNotPartial = sequence.sliding(3, partialWindows = false)  // [[0, 1, 2], [1, 2, 3], [2, 3, 4]]
 * ```
 *
 * @param size Sliding 요소의 수
 * @param partialWindows 마지막 요소가 [size]보다 작을 경우 포함 여부
 * @return Sliding 된 요소를 담은 컬렉션
 */
fun <T> Sequence<T>.sliding(size: Int, partialWindows: Boolean = true): Sequence<List<T>> =
    windowed(size, 1, partialWindows)

/**
 * 컬렉션의 요소를 [size]만큼의 켤렉션으로 묶은 것을 [transform]으로 변환하여 반환합니다.
 *
 * ```
 * val sequence = sequenceOf(0, 1, 2, 3, 4)
 * val sliding = sequence.sliding(3) { it.sum() }  // [3, 6, 9, 7, 4]
 * ```
 *
 * @param size Sliding 요소의 수
 * @param transform 변환 함수
 * @return Sliding 된 요소를 변환한 컬렉션
 */
inline fun <T, R> Sequence<T>.sliding(
    size: Int,
    partialWindows: Boolean = true,
    crossinline transform: (List<T>) -> R,
): Sequence<R> = windowed(size, 1, partialWindows) { transform(it) }
