package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Consolidated parameterized tests covering all structural array extension
 * behavior for the five primitive array variants: Byte, Int, Long, Float, Double.
 *
 * 각 타입별 중복 테스트를 제거하기 위해 [ArrayCase] sealed 계층 + JUnit 5
 * `@ParameterizedTest` + `@MethodSource` 조합을 사용합니다.
 */
class PrimitiveArraySupportTest {

    companion object: KLogging() {

        /**
         * Parameter source: each of the 5 primitive array cases under test.
         */
        @JvmStatic
        fun cases(): Stream<ArrayCase<*>> = Stream.of(
            ByteCase,
            IntCase,
            LongCase,
            FloatCase,
            DoubleCase,
        )
    }

    @ParameterizedTest(name = "index of element - {0}")
    @MethodSource("cases")
    fun `index of element`(case: ArrayCase<*>) {
        case.runIndexOfElement()
    }

    @ParameterizedTest(name = "index of sub-array - {0}")
    @MethodSource("cases")
    fun `index of sub-array`(case: ArrayCase<*>) {
        case.runIndexOfArray()
    }

    @ParameterizedTest(name = "lastIndex of element - {0}")
    @MethodSource("cases")
    fun `lastIndex of element`(case: ArrayCase<*>) {
        case.runLastIndexOfElement()
    }

    @ParameterizedTest(name = "lastIndex of sub-array - {0}")
    @MethodSource("cases")
    fun `lastIndex of sub-array`(case: ArrayCase<*>) {
        case.runLastIndexOfArray()
    }

    @ParameterizedTest(name = "ensure capacity - {0}")
    @MethodSource("cases")
    fun `ensure capacity`(case: ArrayCase<*>) {
        case.runEnsureCapacity()
    }

    @ParameterizedTest(name = "concat arrays - {0}")
    @MethodSource("cases")
    fun `concat arrays`(case: ArrayCase<*>) {
        case.runConcat()
    }

    @ParameterizedTest(name = "reverse empty array - {0}")
    @MethodSource("cases")
    fun `reverse empty array`(case: ArrayCase<*>) {
        case.runReverseEmpty()
    }

    @ParameterizedTest(name = "reverse array - {0}")
    @MethodSource("cases")
    fun `reverse array`(case: ArrayCase<*>) {
        case.runReverse()
    }

    @ParameterizedTest(name = "rotate empty array - {0}")
    @MethodSource("cases")
    fun `rotate empty array`(case: ArrayCase<*>) {
        case.runRotateEmpty()
    }

    @ParameterizedTest(name = "rotate array - {0}")
    @MethodSource("cases")
    fun `rotate array`(case: ArrayCase<*>) {
        case.runRotate()
    }
}

/**
 * 각 primitive 배열 타입의 공통 테스트 시나리오를 표현합니다.
 */
sealed class ArrayCase<A : Any>(private val name: String) {

    override fun toString(): String = name

    // --- Type-specific accessors each case must provide ---
    protected abstract fun sample(): A            // [1, 2, 3, 4, 5]
    protected abstract fun empty(): A
    protected abstract fun singleRepeat(): A       // [1, 2, 3, 4, 3]
    protected abstract fun subArray(): A           // [3, 4]
    protected abstract fun doubleSubArray(): A     // [1, 2, 3, 4, 3, 4, 2]
    protected abstract fun target3(): Any          // element at index 2
    protected abstract fun expectedReversed(): A   // [5, 4, 3, 2, 1]
    protected abstract fun expectedPartialReversed(): A  // [1, 4, 3, 2, 5]
    protected abstract fun expectedEnsure10(): A   // [1, 2, 3, 4, 5, 0, 0, 0, 0, 0]
    protected abstract fun expectedConcat(): A     // [1, 2, 3, 4, 5]
    protected abstract fun expectedConcatRev(): A  // [4, 5, 1, 2, 3]
    protected abstract fun expectedRotate2(): A    // [4, 5, 1, 2, 3]
    protected abstract fun expectedRotateMinus2(): A // [3, 4, 5, 1, 2]

    protected abstract fun indexOfElem(a: A, target: Any, start: Int, end: Int): Int
    protected abstract fun indexOfElemDefault(a: A, target: Any): Int
    protected abstract fun indexOfSub(a: A, target: A, start: Int, end: Int): Int
    protected abstract fun indexOfSubDefault(a: A, target: A): Int
    protected abstract fun lastIndexOfElem(a: A, target: Any, start: Int, end: Int): Int
    protected abstract fun lastIndexOfElemDefault(a: A, target: Any): Int
    protected abstract fun lastIndexOfSub(a: A, target: A, start: Int, end: Int): Int
    protected abstract fun lastIndexOfSubDefault(a: A, target: A): Int
    protected abstract fun ensureCapacity(a: A, minSize: Int, padding: Int): A
    protected abstract fun concatOp(a: A, b: A): A
    protected abstract fun reverseTo(a: A): A
    protected abstract fun reverseTo(a: A, start: Int, end: Int): A
    protected abstract fun reverseThis(a: A)
    protected abstract fun reverseThis(a: A, start: Int, end: Int)
    protected abstract fun rotateTo(a: A, positions: Int): A
    protected abstract fun rotateThis(a: A, positions: Int)
    protected abstract fun eq(actual: A, expected: A)

    // --- Shared scenarios ---
    fun runIndexOfElement() {
        val array = sample()
        indexOfElem(array, target3(), 0, sizeOf(array) - 1) shouldBeEqualTo 2
        indexOfElemDefault(empty(), target3()) shouldBeEqualTo -1
        assertFailsWith<IllegalArgumentException> { indexOfElem(array, target3(), -1, 1) }
        assertFailsWith<IllegalArgumentException> { indexOfElem(array, target3(), 1, sizeOf(array)) }
    }

    fun runIndexOfArray() {
        val array = sample()
        indexOfSub(array, subArray(), 0, sizeOf(array) - 1) shouldBeEqualTo 2
        indexOfSubDefault(empty(), subArray()) shouldBeEqualTo -1
        assertFailsWith<IllegalArgumentException> { indexOfSub(array, subArray(), -1, 1) }
        assertFailsWith<IllegalArgumentException> { indexOfSub(array, subArray(), 1, sizeOf(array) + 1) }
    }

    fun runLastIndexOfElement() {
        val array = singleRepeat()
        lastIndexOfElem(array, target3(), 0, sizeOf(array) - 1) shouldBeEqualTo 4
        lastIndexOfElemDefault(empty(), target3()) shouldBeEqualTo -1
        assertFailsWith<IllegalArgumentException> { lastIndexOfElem(array, target3(), -1, 1) }
        assertFailsWith<IllegalArgumentException> { lastIndexOfElem(array, target3(), 1, sizeOf(array) + 1) }
    }

    fun runLastIndexOfArray() {
        val array = doubleSubArray()
        lastIndexOfSub(array, subArray(), 0, sizeOf(array) - 1) shouldBeEqualTo 4
        lastIndexOfSubDefault(empty(), subArray()) shouldBeEqualTo -1
        assertFailsWith<IllegalArgumentException> { lastIndexOfSub(array, subArray(), -1, 1) }
        assertFailsWith<IllegalArgumentException> { lastIndexOfSub(array, subArray(), 1, sizeOf(array) + 1) }
    }

    fun runEnsureCapacity() {
        val array = sample()
        eq(ensureCapacity(array, sizeOf(array), 5), sample())
        eq(ensureCapacity(array, 10, 0), expectedEnsure10())
        assertFailsWith<IllegalArgumentException> { ensureCapacity(array, -1, 0) }
        assertFailsWith<IllegalArgumentException> { ensureCapacity(array, 0, -1) }
    }

    fun runConcat() {
        eq(concatOp(sliceFirst3(sample()), sliceLast2(sample())), expectedConcat())
        eq(concatOp(sliceLast2(sample()), sliceFirst3(sample())), expectedConcatRev())
    }

    fun runReverseEmpty() {
        val a = empty()
        eq(reverseTo(a), empty())
        eq(reverseTo(a, 0, 0), empty())
        reverseThis(a)
        eq(a, empty())
        reverseThis(a, 0, 0)
        eq(a, empty())
    }

    fun runReverse() {
        val a = sample()
        eq(reverseTo(a, 0, sizeOf(a) - 1), expectedReversed())
        eq(reverseTo(a, 1, 3), expectedPartialReversed())

        val a1 = sample()
        reverseThis(a1)
        eq(a1, expectedReversed())

        val a2 = sample()
        reverseThis(a2, 1, 3)
        eq(a2, expectedPartialReversed())
    }

    fun runRotateEmpty() {
        val a = empty()
        eq(rotateTo(a, 2), empty())
        eq(rotateTo(a, -2), empty())
        rotateThis(a, 2)
        eq(a, empty())
        rotateThis(a, -2)
        eq(a, empty())
    }

    fun runRotate() {
        val a = sample()
        eq(rotateTo(a, 2), expectedRotate2())
        eq(rotateTo(a, -2), expectedRotateMinus2())

        val a1 = sample()
        rotateThis(a1, 2)
        eq(a1, expectedRotate2())

        val a2 = sample()
        rotateThis(a2, -2)
        eq(a2, expectedRotateMinus2())
    }

    // Helpers
    protected abstract fun sizeOf(a: A): Int
    protected abstract fun sliceFirst3(a: A): A
    protected abstract fun sliceLast2(a: A): A
}

// ----------------------- Concrete Cases -----------------------

private object ByteCase : ArrayCase<ByteArray>("ByteArray") {
    override fun sample() = byteArrayOf(1, 2, 3, 4, 5)
    override fun empty() = emptyByteArray
    override fun singleRepeat() = byteArrayOf(1, 2, 3, 4, 3)
    override fun subArray() = byteArrayOf(3, 4)
    override fun doubleSubArray() = byteArrayOf(1, 2, 3, 4, 3, 4, 2)
    override fun target3(): Byte = 3
    override fun expectedReversed() = byteArrayOf(5, 4, 3, 2, 1)
    override fun expectedPartialReversed() = byteArrayOf(1, 4, 3, 2, 5)
    override fun expectedEnsure10() = byteArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)
    override fun expectedConcat() = byteArrayOf(1, 2, 3, 4, 5)
    override fun expectedConcatRev() = byteArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotate2() = byteArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotateMinus2() = byteArrayOf(3, 4, 5, 1, 2)

    override fun indexOfElem(a: ByteArray, target: Any, start: Int, end: Int) =
        a.indexOf(target as Byte, start, end)

    override fun indexOfElemDefault(a: ByteArray, target: Any) = a.indexOf(target as Byte)
    override fun indexOfSub(a: ByteArray, target: ByteArray, start: Int, end: Int) =
        a.indexOf(target, start, end)

    override fun indexOfSubDefault(a: ByteArray, target: ByteArray) = a.indexOf(target)
    override fun lastIndexOfElem(a: ByteArray, target: Any, start: Int, end: Int) =
        a.lastIndexOf(target as Byte, start, end)

    override fun lastIndexOfElemDefault(a: ByteArray, target: Any) = a.lastIndexOf(target as Byte)
    override fun lastIndexOfSub(a: ByteArray, target: ByteArray, start: Int, end: Int) =
        a.lastIndexOf(target, start, end)

    override fun lastIndexOfSubDefault(a: ByteArray, target: ByteArray) = a.lastIndexOf(target)
    override fun ensureCapacity(a: ByteArray, minSize: Int, padding: Int) =
        a.ensureCapacity(minSize, padding)

    override fun concatOp(a: ByteArray, b: ByteArray) = concat(a, b)
    override fun reverseTo(a: ByteArray) = a.reverseTo()
    override fun reverseTo(a: ByteArray, start: Int, end: Int) = a.reverseTo(start, end)
    override fun reverseThis(a: ByteArray) = a.reverseThis()
    override fun reverseThis(a: ByteArray, start: Int, end: Int) = a.reverseThis(start, end)
    override fun rotateTo(a: ByteArray, positions: Int) = a.rotateTo(positions)
    override fun rotateThis(a: ByteArray, positions: Int) = a.rotateThis(positions)
    override fun eq(actual: ByteArray, expected: ByteArray) { actual shouldBeEqualTo expected }
    override fun sizeOf(a: ByteArray) = a.size
    override fun sliceFirst3(a: ByteArray) = a.copyOfRange(0, 3)
    override fun sliceLast2(a: ByteArray) = a.copyOfRange(3, 5)
}

private object IntCase : ArrayCase<IntArray>("IntArray") {
    override fun sample() = intArrayOf(1, 2, 3, 4, 5)
    override fun empty() = emptyIntArray
    override fun singleRepeat() = intArrayOf(1, 2, 3, 4, 3)
    override fun subArray() = intArrayOf(3, 4)
    override fun doubleSubArray() = intArrayOf(1, 2, 3, 4, 3, 4, 2)
    override fun target3(): Int = 3
    override fun expectedReversed() = intArrayOf(5, 4, 3, 2, 1)
    override fun expectedPartialReversed() = intArrayOf(1, 4, 3, 2, 5)
    override fun expectedEnsure10() = intArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)
    override fun expectedConcat() = intArrayOf(1, 2, 3, 4, 5)
    override fun expectedConcatRev() = intArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotate2() = intArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotateMinus2() = intArrayOf(3, 4, 5, 1, 2)

    override fun indexOfElem(a: IntArray, target: Any, start: Int, end: Int) =
        a.indexOf(target as Int, start, end)

    override fun indexOfElemDefault(a: IntArray, target: Any) = a.indexOf(target as Int)
    override fun indexOfSub(a: IntArray, target: IntArray, start: Int, end: Int) =
        a.indexOf(target, start, end)

    override fun indexOfSubDefault(a: IntArray, target: IntArray) = a.indexOf(target)
    override fun lastIndexOfElem(a: IntArray, target: Any, start: Int, end: Int) =
        a.lastIndexOf(target as Int, start, end)

    override fun lastIndexOfElemDefault(a: IntArray, target: Any) = a.lastIndexOf(target as Int)
    override fun lastIndexOfSub(a: IntArray, target: IntArray, start: Int, end: Int) =
        a.lastIndexOf(target, start, end)

    override fun lastIndexOfSubDefault(a: IntArray, target: IntArray) = a.lastIndexOf(target)
    override fun ensureCapacity(a: IntArray, minSize: Int, padding: Int) =
        a.ensureCapacity(minSize, padding)

    override fun concatOp(a: IntArray, b: IntArray) = concat(a, b)
    override fun reverseTo(a: IntArray) = a.reverseTo()
    override fun reverseTo(a: IntArray, start: Int, end: Int) = a.reverseTo(start, end)
    override fun reverseThis(a: IntArray) = a.reverseThis()
    override fun reverseThis(a: IntArray, start: Int, end: Int) = a.reverseThis(start, end)
    override fun rotateTo(a: IntArray, positions: Int) = a.rotateTo(positions)
    override fun rotateThis(a: IntArray, positions: Int) = a.rotateThis(positions)
    override fun eq(actual: IntArray, expected: IntArray) { actual shouldBeEqualTo expected }
    override fun sizeOf(a: IntArray) = a.size
    override fun sliceFirst3(a: IntArray) = a.copyOfRange(0, 3)
    override fun sliceLast2(a: IntArray) = a.copyOfRange(3, 5)
}

private object LongCase : ArrayCase<LongArray>("LongArray") {
    override fun sample() = longArrayOf(1, 2, 3, 4, 5)
    override fun empty() = emptyLongArray
    override fun singleRepeat() = longArrayOf(1, 2, 3, 4, 3)
    override fun subArray() = longArrayOf(3, 4)
    override fun doubleSubArray() = longArrayOf(1, 2, 3, 4, 3, 4, 2)
    override fun target3(): Long = 3L
    override fun expectedReversed() = longArrayOf(5, 4, 3, 2, 1)
    override fun expectedPartialReversed() = longArrayOf(1, 4, 3, 2, 5)
    override fun expectedEnsure10() = longArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)
    override fun expectedConcat() = longArrayOf(1, 2, 3, 4, 5)
    override fun expectedConcatRev() = longArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotate2() = longArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotateMinus2() = longArrayOf(3, 4, 5, 1, 2)

    override fun indexOfElem(a: LongArray, target: Any, start: Int, end: Int) =
        a.indexOf(target as Long, start, end)

    override fun indexOfElemDefault(a: LongArray, target: Any) = a.indexOf(target as Long)
    override fun indexOfSub(a: LongArray, target: LongArray, start: Int, end: Int) =
        a.indexOf(target, start, end)

    override fun indexOfSubDefault(a: LongArray, target: LongArray) = a.indexOf(target)
    override fun lastIndexOfElem(a: LongArray, target: Any, start: Int, end: Int) =
        a.lastIndexOf(target as Long, start, end)

    override fun lastIndexOfElemDefault(a: LongArray, target: Any) = a.lastIndexOf(target as Long)
    override fun lastIndexOfSub(a: LongArray, target: LongArray, start: Int, end: Int) =
        a.lastIndexOf(target, start, end)

    override fun lastIndexOfSubDefault(a: LongArray, target: LongArray) = a.lastIndexOf(target)
    override fun ensureCapacity(a: LongArray, minSize: Int, padding: Int) =
        a.ensureCapacity(minSize, padding)

    override fun concatOp(a: LongArray, b: LongArray) = concat(a, b)
    override fun reverseTo(a: LongArray) = a.reverseTo()
    override fun reverseTo(a: LongArray, start: Int, end: Int) = a.reverseTo(start, end)
    override fun reverseThis(a: LongArray) = a.reverseThis()
    override fun reverseThis(a: LongArray, start: Int, end: Int) = a.reverseThis(start, end)
    override fun rotateTo(a: LongArray, positions: Int) = a.rotateTo(positions)
    override fun rotateThis(a: LongArray, positions: Int) = a.rotateThis(positions)
    override fun eq(actual: LongArray, expected: LongArray) { actual shouldBeEqualTo expected }
    override fun sizeOf(a: LongArray) = a.size
    override fun sliceFirst3(a: LongArray) = a.copyOfRange(0, 3)
    override fun sliceLast2(a: LongArray) = a.copyOfRange(3, 5)
}

private object FloatCase : ArrayCase<FloatArray>("FloatArray") {
    override fun sample() = floatArrayOf(1, 2, 3, 4, 5)
    override fun empty() = emptyFloatArray
    override fun singleRepeat() = floatArrayOf(1, 2, 3, 4, 3)
    override fun subArray() = floatArrayOf(3, 4)
    override fun doubleSubArray() = floatArrayOf(1, 2, 3, 4, 3, 4, 2)
    override fun target3(): Float = 3f
    override fun expectedReversed() = floatArrayOf(5, 4, 3, 2, 1)
    override fun expectedPartialReversed() = floatArrayOf(1, 4, 3, 2, 5)
    override fun expectedEnsure10() = floatArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)
    override fun expectedConcat() = floatArrayOf(1, 2, 3, 4, 5)
    override fun expectedConcatRev() = floatArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotate2() = floatArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotateMinus2() = floatArrayOf(3, 4, 5, 1, 2)

    override fun indexOfElem(a: FloatArray, target: Any, start: Int, end: Int) =
        a.indexOf(target as Float, start, end)

    override fun indexOfElemDefault(a: FloatArray, target: Any) = a.indexOf(target as Float)
    override fun indexOfSub(a: FloatArray, target: FloatArray, start: Int, end: Int) =
        a.indexOf(target, start, end)

    override fun indexOfSubDefault(a: FloatArray, target: FloatArray) = a.indexOf(target)
    override fun lastIndexOfElem(a: FloatArray, target: Any, start: Int, end: Int) =
        a.lastIndexOf(target as Float, start, end)

    override fun lastIndexOfElemDefault(a: FloatArray, target: Any) = a.lastIndexOf(target as Float)
    override fun lastIndexOfSub(a: FloatArray, target: FloatArray, start: Int, end: Int) =
        a.lastIndexOf(target, start, end)

    override fun lastIndexOfSubDefault(a: FloatArray, target: FloatArray) = a.lastIndexOf(target)
    override fun ensureCapacity(a: FloatArray, minSize: Int, padding: Int) =
        a.ensureCapacity(minSize, padding)

    override fun concatOp(a: FloatArray, b: FloatArray) = concat(a, b)
    override fun reverseTo(a: FloatArray) = a.reverseTo()
    override fun reverseTo(a: FloatArray, start: Int, end: Int) = a.reverseTo(start, end)
    override fun reverseThis(a: FloatArray) = a.reverseThis()
    override fun reverseThis(a: FloatArray, start: Int, end: Int) = a.reverseThis(start, end)
    override fun rotateTo(a: FloatArray, positions: Int) = a.rotateTo(positions)
    override fun rotateThis(a: FloatArray, positions: Int) = a.rotateThis(positions)
    override fun eq(actual: FloatArray, expected: FloatArray) { actual shouldBeEqualTo expected }
    override fun sizeOf(a: FloatArray) = a.size
    override fun sliceFirst3(a: FloatArray) = a.copyOfRange(0, 3)
    override fun sliceLast2(a: FloatArray) = a.copyOfRange(3, 5)
}

private object DoubleCase : ArrayCase<DoubleArray>("DoubleArray") {
    override fun sample() = doubleArrayOf(1, 2, 3, 4, 5)
    override fun empty() = emptyDoubleArray
    override fun singleRepeat() = doubleArrayOf(1, 2, 3, 4, 3)
    override fun subArray() = doubleArrayOf(3, 4)
    override fun doubleSubArray() = doubleArrayOf(1, 2, 3, 4, 3, 4, 2)
    override fun target3(): Double = 3.0
    override fun expectedReversed() = doubleArrayOf(5, 4, 3, 2, 1)
    override fun expectedPartialReversed() = doubleArrayOf(1, 4, 3, 2, 5)
    override fun expectedEnsure10() = doubleArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)
    override fun expectedConcat() = doubleArrayOf(1, 2, 3, 4, 5)
    override fun expectedConcatRev() = doubleArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotate2() = doubleArrayOf(4, 5, 1, 2, 3)
    override fun expectedRotateMinus2() = doubleArrayOf(3, 4, 5, 1, 2)

    override fun indexOfElem(a: DoubleArray, target: Any, start: Int, end: Int) =
        a.indexOf(target as Double, start, end)

    override fun indexOfElemDefault(a: DoubleArray, target: Any) = a.indexOf(target as Double)
    override fun indexOfSub(a: DoubleArray, target: DoubleArray, start: Int, end: Int) =
        a.indexOf(target, start, end)

    override fun indexOfSubDefault(a: DoubleArray, target: DoubleArray) = a.indexOf(target)
    override fun lastIndexOfElem(a: DoubleArray, target: Any, start: Int, end: Int) =
        a.lastIndexOf(target as Double, start, end)

    override fun lastIndexOfElemDefault(a: DoubleArray, target: Any) = a.lastIndexOf(target as Double)
    override fun lastIndexOfSub(a: DoubleArray, target: DoubleArray, start: Int, end: Int) =
        a.lastIndexOf(target, start, end)

    override fun lastIndexOfSubDefault(a: DoubleArray, target: DoubleArray) = a.lastIndexOf(target)
    override fun ensureCapacity(a: DoubleArray, minSize: Int, padding: Int) =
        a.ensureCapacity(minSize, padding)

    override fun concatOp(a: DoubleArray, b: DoubleArray) = concat(a, b)
    override fun reverseTo(a: DoubleArray) = a.reverseTo()
    override fun reverseTo(a: DoubleArray, start: Int, end: Int) = a.reverseTo(start, end)
    override fun reverseThis(a: DoubleArray) = a.reverseThis()
    override fun reverseThis(a: DoubleArray, start: Int, end: Int) = a.reverseThis(start, end)
    override fun rotateTo(a: DoubleArray, positions: Int) = a.rotateTo(positions)
    override fun rotateThis(a: DoubleArray, positions: Int) = a.rotateThis(positions)
    override fun eq(actual: DoubleArray, expected: DoubleArray) { actual shouldBeEqualTo expected }
    override fun sizeOf(a: DoubleArray) = a.size
    override fun sliceFirst3(a: DoubleArray) = a.copyOfRange(0, 3)
    override fun sliceLast2(a: DoubleArray) = a.copyOfRange(3, 5)
}
