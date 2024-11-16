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
 * Array의 요소에 지정한 [setter]를 통해 값을 설정합니다.
 *
 * ```
 * val array = arrayOf(1, 2, 3, 4, 5)
 * array.setAll { it * 2 }
 * println(array.contentToString()) // [0, 2, 4, 6, 8]
 * ```
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun BooleanArray.setAll(setter: (index: Int) -> Boolean): BooleanArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun CharArray.setAll(setter: (index: Int) -> Char): CharArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun ByteArray.setAll(setter: (index: Int) -> Byte): ByteArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun ShortArray.setAll(setter: (index: Int) -> Short): ShortArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun IntArray.setAll(setter: (index: Int) -> Int): IntArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun LongArray.setAll(setter: (index: Int) -> Long): LongArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun FloatArray.setAll(setter: (index: Int) -> Float): FloatArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 요소에 지정한 `supplier`를 통해 값을 설정합니다.
 *
 * @param setter 요소의 값을 제공하는 함수
 * @return
 */
inline fun DoubleArray.setAll(setter: (index: Int) -> Double): DoubleArray = apply {
    indices.forEach { this[it] = setter(it) }
}

/**
 * Array의 첫번째 요소를 삭제한 새로운 Array를 반환합니다.
 */
fun <T> Array<T>.removeFirst(): Array<T> = this.copyOfRange(1, size)

/**
 * Array의 마지막 요소를 삭제한 새로운 Array를 반환합니다.
 */
fun <T> Array<T>.removeLast(): Array<T> = this.copyOfRange(0, size - 1)

/**
 * Array의 마지막 요소를 삭제한 새로운 Array를 반환합니다.
 */
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

inline fun <T, R> Array<T>.mapCatching(mapper: (T) -> R): List<Result<R>> {
    return map { runCatching { mapper(it) } }
}

inline fun <T> Array<T>.forEachCatching(action: (T) -> Unit): List<Result<Unit>> {
    return map { runCatching { action(it) } }
}

/**
 * Array의 요소가 `0` 이 아닌 첫번째 인덱스를 반환합니다.
 */
fun ByteArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.toByte()) zc++
    return zc
}

/**
 * Array의 요소가 `0` 이 아닌 첫번째 인덱스를 반환합니다.
 */
fun ShortArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0.toShort()) zc++
    return zc
}

/**
 * Array의 요소가 `0` 이 아닌 첫번째 인덱스를 반환합니다.
 */
fun IntArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0) zc++
    return zc
}

/**
 * Array의 요소가 `0` 이 아닌 첫번째 인덱스를 반환합니다.
 */
fun LongArray.leadingZeros(): Int {
    var zc = 0
    while (zc < size && this[zc] == 0L) zc++
    return zc
}

/**
 * 현 Array 항목 수가 지정한 항목 수보다 작다면, 새로운 Array을 만들고, 기존 요소는 복사하고, `item` 값을 새롭게 할당된 공간에 추가합니다.
 *
 * @param newSize 새로운 Array의 크기
 * @param item 새롭게 추가될 요소의 값
 * @return 새로운 아이템이 추가된 Array
 */
inline fun <reified T> Array<T>.padTo(newSize: Int, item: T): Array<T> {
    val remains = newSize - this.size
    if (remains <= 0) {
        return this
    }

    val array = Array(newSize) { item }
    this.copyInto(array, 0, 0, this.size)
    return array
}
