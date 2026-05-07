package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages

//
// ── IntArray ──────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @param expected 포함되어야 하는 값
 * @return receiver (체이닝 지원)
 */
fun IntArray?.shouldContain(expected: Int): IntArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @param expected 포함되지 않아야 하는 값
 * @return receiver (체이닝 지원)
 */
fun IntArray?.shouldNotContain(expected: Int): IntArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @param size 기대하는 크기
 * @return receiver (체이닝 지원)
 */
infix fun IntArray?.shouldHaveSize(size: Int): IntArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @return receiver (체이닝 지원)
 */
fun IntArray?.shouldBeEmpty(): IntArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Int>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @return receiver (체이닝 지원)
 */
fun IntArray?.shouldNotBeEmpty(): IntArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── LongArray ─────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun LongArray?.shouldContain(expected: Long): LongArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun LongArray?.shouldNotContain(expected: Long): LongArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun LongArray?.shouldHaveSize(size: Int): LongArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun LongArray?.shouldBeEmpty(): LongArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Long>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun LongArray?.shouldNotBeEmpty(): LongArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── DoubleArray ───────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun DoubleArray?.shouldContain(expected: Double): DoubleArray {
    if (this == null || this.none { it == expected }) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun DoubleArray?.shouldNotContain(expected: Double): DoubleArray? {
    if (this != null && this.any { it == expected }) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun DoubleArray?.shouldHaveSize(size: Int): DoubleArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun DoubleArray?.shouldBeEmpty(): DoubleArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Double>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun DoubleArray?.shouldNotBeEmpty(): DoubleArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── FloatArray ────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun FloatArray?.shouldContain(expected: Float): FloatArray {
    if (this == null || this.none { it == expected }) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun FloatArray?.shouldNotContain(expected: Float): FloatArray? {
    if (this != null && this.any { it == expected }) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun FloatArray?.shouldHaveSize(size: Int): FloatArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun FloatArray?.shouldBeEmpty(): FloatArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Float>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun FloatArray?.shouldNotBeEmpty(): FloatArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── ByteArray ─────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun ByteArray?.shouldContain(expected: Byte): ByteArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun ByteArray?.shouldNotContain(expected: Byte): ByteArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun ByteArray?.shouldHaveSize(size: Int): ByteArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun ByteArray?.shouldBeEmpty(): ByteArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Byte>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun ByteArray?.shouldNotBeEmpty(): ByteArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── ShortArray ────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun ShortArray?.shouldContain(expected: Short): ShortArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun ShortArray?.shouldNotContain(expected: Short): ShortArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun ShortArray?.shouldHaveSize(size: Int): ShortArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun ShortArray?.shouldBeEmpty(): ShortArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Short>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun ShortArray?.shouldNotBeEmpty(): ShortArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── CharArray ─────────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun CharArray?.shouldContain(expected: Char): CharArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun CharArray?.shouldNotContain(expected: Char): CharArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun CharArray?.shouldHaveSize(size: Int): CharArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun CharArray?.shouldBeEmpty(): CharArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Char>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun CharArray?.shouldNotBeEmpty(): CharArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── BooleanArray ──────────────────────────────────────────────────────────────
//

/**
 * 배열이 [expected] 값을 포함하는지 검증한다.
 */
fun BooleanArray?.shouldContain(expected: Boolean): BooleanArray {
    if (this == null || !this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열이 [expected] 값을 포함하지 않는지 검증한다.
 */
fun BooleanArray?.shouldNotContain(expected: Boolean): BooleanArray? {
    if (this != null && this.contains(expected)) {
        Failures.failComparison(
            Messages.expectedNotToBe("contain", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 배열의 크기가 [size]와 같은지 검증한다.
 */
infix fun BooleanArray?.shouldHaveSize(size: Int): BooleanArray {
    val arr = this
    val actualSize = arr?.size
    if (arr == null || actualSize != size) {
        Failures.failComparison(
            Messages.expectedToBe("have size", size, actualSize),
            size,
            actualSize
        )
    }
    return arr
}

/**
 * 배열이 비어있는지 검증한다.
 */
fun BooleanArray?.shouldBeEmpty(): BooleanArray? {
    if (this == null || this.isNotEmpty()) {
        Failures.failComparison(
            Messages.expectedToBe("be empty", emptyArray<Boolean>(), this),
            0,
            this?.size
        )
    }
    return this
}

/**
 * 배열이 비어있지 않은지 검증한다.
 */
fun BooleanArray?.shouldNotBeEmpty(): BooleanArray {
    val arr = this
    if (arr == null || arr.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was${if (arr == null) " null" else " empty"}.")
    }
    return arr
}

//
// ── shouldContentEqual ────────────────────────────────────────────────────────
//

/**
 * 두 IntArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 *
 * 두 배열이 모두 null이면 통과한다.
 *
 * @receiver 검증할 IntArray (nullable)
 * @param expected 기대하는 IntArray (nullable)
 * @return receiver (체이닝 지원)
 */
infix fun IntArray?.shouldContentEqual(expected: IntArray?): IntArray? {
    if (!intArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 LongArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun LongArray?.shouldContentEqual(expected: LongArray?): LongArray? {
    if (!longArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 DoubleArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 *
 * NaN 비교: NaN == NaN → true (비트 비교), -0.0 != 0.0 → 구분.
 */
infix fun DoubleArray?.shouldContentEqual(expected: DoubleArray?): DoubleArray? {
    if (!doubleArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 FloatArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 *
 * NaN 비교: NaN == NaN → true (비트 비교), -0.0 != 0.0 → 구분.
 */
infix fun FloatArray?.shouldContentEqual(expected: FloatArray?): FloatArray? {
    if (!floatArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 ByteArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun ByteArray?.shouldContentEqual(expected: ByteArray?): ByteArray? {
    if (!byteArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 ShortArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun ShortArray?.shouldContentEqual(expected: ShortArray?): ShortArray? {
    if (!shortArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 CharArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun CharArray?.shouldContentEqual(expected: CharArray?): CharArray? {
    if (!charArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 두 BooleanArray의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun BooleanArray?.shouldContentEqual(expected: BooleanArray?): BooleanArray? {
    if (!booleanArrayContentEquals(this, expected)) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

// ── Array<T> size / empty / shouldContainSame ─────────────────────────────────

/** Array<T>의 크기가 [size]와 같은지 검증한다. */
@Suppress("UNCHECKED_CAST")
infix fun <T> Array<T>?.shouldHaveSize(size: Int): Array<T> {
    val actual = this?.size ?: 0
    if (actual != size) {
        Failures.failComparison(
            "Expected array to have size $size, but had size $actual.",
            size, actual
        )
    }
    return this ?: (emptyArray<Any?>() as Array<T>)
}

/** Array<T>가 비어있는지 검증한다. */
fun <T> Array<T>?.shouldBeEmpty(): Array<T>? {
    if (this != null && this.isNotEmpty()) {
        Failures.fail("Expected array to be empty, but had ${this.size} elements.")
    }
    return this
}

/** Array<T>가 비어있지 않은지 검증한다. */
fun <T> Array<T>?.shouldNotBeEmpty(): Array<T> {
    if (this == null || this.isEmpty()) {
        Failures.fail("Expected array to not be empty, but was ${Messages.stringify(this)}.")
    }
    return this
}

/**
 * 두 Array<T>가 순서에 무관하게 동일한 원소(cardinality 포함)를 가지는지 검증한다.
 */
@Suppress("UNCHECKED_CAST")
infix fun <T> Array<T>?.shouldContainSame(expected: Array<T>?): Array<T> {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: (emptyArray<Any?>() as Array<T>)
}

/**
 * IntArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun IntArray?.shouldContainSame(expected: IntArray?): IntArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: intArrayOf()
}

/**
 * LongArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun LongArray?.shouldContainSame(expected: LongArray?): LongArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: longArrayOf()
}

/**
 * LongArray가 Iterable<Long>과 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun LongArray?.shouldContainSame(expected: Iterable<Long>?): LongArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: longArrayOf()
}

/**
 * ByteArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun ByteArray?.shouldContainSame(expected: ByteArray?): ByteArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: byteArrayOf()
}

/**
 * FloatArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun FloatArray?.shouldContainSame(expected: FloatArray?): FloatArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: floatArrayOf()
}

/**
 * DoubleArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun DoubleArray?.shouldContainSame(expected: DoubleArray?): DoubleArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: doubleArrayOf()
}

/**
 * ShortArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun ShortArray?.shouldContainSame(expected: ShortArray?): ShortArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: shortArrayOf()
}

/**
 * CharArray가 순서에 무관하게 동일한 원소를 가지는지 검증한다.
 */
infix fun CharArray?.shouldContainSame(expected: CharArray?): CharArray {
    this?.toList().shouldContainSame(expected?.toList())
    return this ?: charArrayOf()
}

/**
 * 두 Array<T>의 내용이 동일한지 검증한다 (순서 포함, null-safe).
 */
infix fun <T> Array<T>?.shouldContentEqual(expected: Array<T>?): Array<T>? {
    val equal = when {
        this === expected -> true
        this == null || expected == null -> false
        this.size != expected.size -> false
        else -> this.contentEquals(expected)
    }
    if (!equal) {
        Failures.failComparison(
            Messages.comparison(expected, this),
            expected,
            this
        )
    }
    return this
}

//
// ── Private helpers ───────────────────────────────────────────────────────────
//

private fun intArrayContentEquals(a: IntArray?, b: IntArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}

private fun longArrayContentEquals(a: LongArray?, b: LongArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}

/**
 * DoubleArray의 내용을 NaN-aware 비트 비교로 검증한다.
 *
 * - NaN == NaN → true (java.lang.Double.doubleToRawLongBits 사용)
 * - -0.0 != 0.0 → 구분
 */
private fun doubleArrayContentEquals(a: DoubleArray?, b: DoubleArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    a.size != b.size -> false
    else -> a.indices.all { i ->
        java.lang.Double.doubleToRawLongBits(a[i]) == java.lang.Double.doubleToRawLongBits(b[i])
    }
}

/**
 * FloatArray의 내용을 NaN-aware 비트 비교로 검증한다.
 *
 * - NaN == NaN → true (java.lang.Float.floatToRawIntBits 사용)
 * - -0.0f != 0.0f → 구분
 */
private fun floatArrayContentEquals(a: FloatArray?, b: FloatArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    a.size != b.size -> false
    else -> a.indices.all { i ->
        java.lang.Float.floatToRawIntBits(a[i]) == java.lang.Float.floatToRawIntBits(b[i])
    }
}

private fun byteArrayContentEquals(a: ByteArray?, b: ByteArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}

private fun shortArrayContentEquals(a: ShortArray?, b: ShortArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}

private fun charArrayContentEquals(a: CharArray?, b: CharArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}

private fun booleanArrayContentEquals(a: BooleanArray?, b: BooleanArray?): Boolean = when {
    a === b -> true
    a == null || b == null -> false
    else -> a.contentEquals(b)
}
