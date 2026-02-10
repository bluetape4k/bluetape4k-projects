package io.bluetape4k.support

/** Size가 0인 BooleanArray */
val emptyBooleanArray: BooleanArray = BooleanArray(0)

/** Size가 0인 CharArray */
val emptyCharArray: CharArray = CharArray(0)

/** Size가 0인 ByteArray */
val emptyByteArray: ByteArray = ByteArray(0)

/** Size가 0인 ShortArray */
val emptyShortArray: ShortArray = ShortArray(0)

/** Size가 0인 IntArray */
val emptyIntArray: IntArray = IntArray(0)

/** Size가 0인 LongArray */
val emptyLongArray: LongArray = LongArray(0)

/** Size가 0인 FloatArray */
val emptyFloatArray: FloatArray = FloatArray(0)

/** Size가 0인 DoubleArray */
val emptyDoubleArray: DoubleArray = DoubleArray(0)

fun BooleanArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun CharArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun ByteArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun ShortArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun IntArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun LongArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun FloatArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun DoubleArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()
fun <T> Array<T>?.isNullOrEmpty(): Boolean = this?.isEmpty() ?: true

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun BooleanArray.setAll(generator: (index: Int) -> Boolean): BooleanArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun CharArray.setAll(generator: (index: Int) -> Char): CharArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun ByteArray.setAll(generator: (index: Int) -> Byte): ByteArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun ShortArray.setAll(generator: (index: Int) -> Short): ShortArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun IntArray.setAll(generator: (index: Int) -> Int): IntArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun LongArray.setAll(generator: (index: Int) -> Long): LongArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun FloatArray.setAll(generator: (index: Int) -> Float): FloatArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * 배열의 각 요소를 인덱스를 인자로 받는 [generator] 함수의 반환값으로 설정합니다.
 *
 * 이 함수는 배열을 제자리(in-place)에서 변경하며,
 * 변경된 자기 자신(this)을 반환합니다.
 *
 * 배열이 비어 있는 경우 아무 동작도 하지 않습니다.
 *
 * @param generator 인덱스를 받아 요소 값을 생성하는 함수
 * @return 자기 자신(this)
 */
inline fun DoubleArray.setAll(generator: (index: Int) -> Double): DoubleArray = apply {
    indices.forEach { this[it] = generator(it) }
}

/**
 * Array의 첫번째 요소를 삭제한 새로운 Array를 반환합니다.
 */
fun <T> Array<T>.removeFirst(): Array<T> {
    check(isNotEmpty()) { "Array is empty." }
    return this.copyOfRange(1, size)
}

/**
 * Array의 마지막 요소를 삭제한 새로운 Array를 반환합니다.
 */
fun <T> Array<T>.removeLast(): Array<T> {
    check(isNotEmpty()) { "Array is empty." }
    return this.copyOfRange(0, size - 1)
}

/**
 * Array의 마지막 요소를 삭제한 새로운 Array를 반환합니다.
 */
@Deprecated("use removeLast() instead", ReplaceWith("removeLast()"))
fun <T> Array<T>.removeLastValue(): Array<T> = this.copyOfRange(0, size - 1)

/**
 * Array의 마지막 요소에 지정한 값을 설정합니다.
 *
 * @param value 설정할 값
 */
fun <T> Array<T>.setFirst(value: T) {
    check(isNotEmpty()) { "Array is empty." }
    this[0] = value
}

/**
 * Array의 마지막 요소에 지정한 값을 설정합니다.
 *
 * @param value 설정할 값
 */
fun <T> Array<T>.setLast(value: T) {
    check(isNotEmpty()) { "Array is empty." }
    this[lastIndex] = value
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <T, R> Array<T>.mapCatching(transform: (T) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> CharArray.mapCatching(transform: (Char) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> ByteArray.mapCatching(transform: (Byte) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> ShortArray.mapCatching(transform: (Short) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> IntArray.mapCatching(transform: (Int) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> LongArray.mapCatching(transform: (Long) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> FloatArray.mapCatching(transform: (Float) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [transform] 함수를 적용하고,
 * 예외를 던지는 대신 [Result.failure] 로 감싸서 반환합니다.
 *
 * 변환은 순차적으로 수행되며 원본 배열의 순서를 유지합니다.
 * 특정 요소 처리 중 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <R> DoubleArray.mapCatching(transform: (Double) -> R): List<Result<R>> {
    return map { runCatching { transform(it) } }
}

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun <T> Array<T>.forEachCatching(action: (T) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun CharArray.forEachCatching(action: (Char) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun ByteArray.forEachCatching(action: (Byte) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun ShortArray.forEachCatching(action: (Short) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun IntArray.forEachCatching(action: (Int) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun LongArray.forEachCatching(action: (Long) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun FloatArray.forEachCatching(action: (Float) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 각 요소에 [action] 을 수행하며,
 * 발생한 예외를 던지지 않고 [Result] 로 감싸 반환합니다.
 *
 * - 모든 요소는 순차적으로 처리됩니다.
 * - 특정 요소에서 예외가 발생하더라도 나머지 요소 처리는 계속됩니다.
 * - 반환되는 [Result] 는 최초로 발생한 실패 또는 전체 성공을 나타냅니다.
 *
 * @return 각 요소 처리 결과를 담은 [Result] 리스트
 */
inline fun DoubleArray.forEachCatching(action: (Double) -> Unit): List<Result<Unit>> =
    map { runCatching { action(it) } }

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun CharArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.toChar()) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun ByteArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.toByte()) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun ShortArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.toShort()) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun IntArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun LongArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0L) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun FloatArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.0F) zc++
    return zc
}

/**
 * 배열의 시작 부분부터 연속으로 등장하는 0 값의 개수를 반환합니다.
 *
 * - 배열이 비어 있거나 첫 번째 요소가 0이 아니면 `0`을 반환합니다.
 * - 모든 요소가 0인 경우 배열의 크기를 반환합니다.
 *
 * 이 함수는 인덱스 `0`부터 순차적으로 탐색하며,
 * 처음으로 0이 아닌 값을 만나면 즉시 중단합니다.
 */
fun DoubleArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.0) zc++
    return zc
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채운 새 배열을 반환합니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 새로운 배열을 생성하지 않고 자기 자신을 그대로 반환합니다.
 *
 * @param newSize 최소로 보장할 배열 크기
 * @param padValue 부족한 요소를 채울 값
 * @return 패딩된 배열 또는 패딩이 필요 없는 경우 자기 자신
 */
inline fun <reified T> Array<T>.padTo(newSize: Int, padValue: T): Array<T> {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = Array(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}


/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun CharArray.padTo(newSize: Int, padValue: Char): CharArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = CharArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}


/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun ByteArray.padTo(newSize: Int, padValue: Byte): ByteArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = ByteArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun ShortArray.padTo(newSize: Int, padValue: Short): ShortArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = ShortArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun IntArray.padTo(newSize: Int, padValue: Int): IntArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = IntArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun LongArray.padTo(newSize: Int, padValue: Long): LongArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = LongArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun FloatArray.padTo(newSize: Int, padValue: Float): FloatArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = FloatArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}

/**
 * 배열의 크기가 [newSize] 가 될 때까지 [padValue] 로 뒤를 채웁니다.
 *
 * [newSize] 가 현재 배열 크기보다 작거나 같으면
 * 불필요한 복사를 피하기 위해 동일한 배열 인스턴스를 그대로 반환합니다.
 *
 * 이 동작은 성능 최적화를 위한 의도적인 설계입니다.
 */
fun DoubleArray.padTo(newSize: Int, padValue: Double): DoubleArray {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = DoubleArray(newSize) { padValue }
    copyInto(array, 0, 0, size)
    return array
}
