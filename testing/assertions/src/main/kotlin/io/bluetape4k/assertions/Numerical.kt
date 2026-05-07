package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import java.math.BigDecimal

const val DOUBLE_EPSILON: Double = 1e-9
const val FLOAT_EPSILON: Float = 1e-6f
val BIGDECIMAL_EPSILON: BigDecimal = BigDecimal("1E-9")

// ── Comparable 비교 ──────────────────────────────────────────────────────────

/**
 * receiver가 [expected]보다 큰지 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeGreaterThan(expected: T): T {
    if (this <= expected) {
        Failures.failComparison(
            Messages.expectedToBe("be greater than", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 크거나 같은지 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeGreaterOrEqualTo(expected: T): T {
    if (this < expected) {
        Failures.failComparison(
            Messages.expectedToBe("be greater than or equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 작은지 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeLessThan(expected: T): T {
    if (this >= expected) {
        Failures.failComparison(
            Messages.expectedToBe("be less than", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 작거나 같은지 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeLessOrEqualTo(expected: T): T {
    if (this > expected) {
        Failures.failComparison(
            Messages.expectedToBe("be less than or equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 크지 않은지 검증한다 (작거나 같음).
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeGreaterThan(expected: T): T {
    if (this > expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be greater than", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 크거나 같지 않은지 검증한다 (작음).
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeGreaterOrEqualTo(expected: T): T {
    if (this >= expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be greater than or equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 작지 않은지 검증한다 (크거나 같음).
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeLessThan(expected: T): T {
    if (this < expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be less than", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * receiver가 [expected]보다 작거나 같지 않은지 검증한다 (큼).
 *
 * @receiver 검증할 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeLessOrEqualTo(expected: T): T {
    if (this <= expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be less than or equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

// ── 부호 검증 (Byte) ─────────────────────────────────────────────────────────

/**
 * Byte 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Byte 값
 * @return receiver (체이닝 지원)
 */
fun Byte.shouldBePositive(): Byte {
    if (this <= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Byte 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Byte 값
 * @return receiver (체이닝 지원)
 */
fun Byte.shouldBeNegative(): Byte {
    if (this >= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Byte 값이 0인지 검증한다.
 *
 * @receiver 검증할 Byte 값
 * @return receiver (체이닝 지원)
 */
fun Byte.shouldBeZero(): Byte {
    if (this != 0.toByte()) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0, this),
            0,
            this
        )
    }
    return this
}

// ── 부호 검증 (Short) ────────────────────────────────────────────────────────

/**
 * Short 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Short 값
 * @return receiver (체이닝 지원)
 */
fun Short.shouldBePositive(): Short {
    if (this <= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Short 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Short 값
 * @return receiver (체이닝 지원)
 */
fun Short.shouldBeNegative(): Short {
    if (this >= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Short 값이 0인지 검증한다.
 *
 * @receiver 검증할 Short 값
 * @return receiver (체이닝 지원)
 */
fun Short.shouldBeZero(): Short {
    if (this != 0.toShort()) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0, this),
            0,
            this
        )
    }
    return this
}

// ── 부호 검증 (Int) ──────────────────────────────────────────────────────────

/**
 * Int 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Int 값
 * @return receiver (체이닝 지원)
 */
fun Int.shouldBePositive(): Int {
    if (this <= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Int 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Int 값
 * @return receiver (체이닝 지원)
 */
fun Int.shouldBeNegative(): Int {
    if (this >= 0) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Int 값이 0인지 검증한다.
 *
 * @receiver 검증할 Int 값
 * @return receiver (체이닝 지원)
 */
fun Int.shouldBeZero(): Int {
    if (this != 0) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0, this),
            0,
            this
        )
    }
    return this
}

// ── 부호 검증 (Long) ─────────────────────────────────────────────────────────

/**
 * Long 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Long 값
 * @return receiver (체이닝 지원)
 */
fun Long.shouldBePositive(): Long {
    if (this <= 0L) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Long 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Long 값
 * @return receiver (체이닝 지원)
 */
fun Long.shouldBeNegative(): Long {
    if (this >= 0L) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Long 값이 0인지 검증한다.
 *
 * @receiver 검증할 Long 값
 * @return receiver (체이닝 지원)
 */
fun Long.shouldBeZero(): Long {
    if (this != 0L) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0L, this),
            0L,
            this
        )
    }
    return this
}

// ── 부호 검증 (Float) ────────────────────────────────────────────────────────

/**
 * Float 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Float 값
 * @return receiver (체이닝 지원)
 */
fun Float.shouldBePositive(): Float {
    if (this <= 0.0f) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Float 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Float 값
 * @return receiver (체이닝 지원)
 */
fun Float.shouldBeNegative(): Float {
    if (this >= 0.0f) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Float 값이 0인지 검증한다.
 *
 * @receiver 검증할 Float 값
 * @return receiver (체이닝 지원)
 */
fun Float.shouldBeZero(): Float {
    if (this != 0.0f) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0.0f, this),
            0.0f,
            this
        )
    }
    return this
}

// ── 부호 검증 (Double) ───────────────────────────────────────────────────────

/**
 * Double 값이 양수인지 검증한다.
 *
 * @receiver 검증할 Double 값
 * @return receiver (체이닝 지원)
 */
fun Double.shouldBePositive(): Double {
    if (this <= 0.0) {
        Failures.failComparison(
            Messages.expectedToBe("be positive", "> 0", this),
            "> 0",
            this
        )
    }
    return this
}

/**
 * Double 값이 음수인지 검증한다.
 *
 * @receiver 검증할 Double 값
 * @return receiver (체이닝 지원)
 */
fun Double.shouldBeNegative(): Double {
    if (this >= 0.0) {
        Failures.failComparison(
            Messages.expectedToBe("be negative", "< 0", this),
            "< 0",
            this
        )
    }
    return this
}

/**
 * Double 값이 0인지 검증한다.
 *
 * @receiver 검증할 Double 값
 * @return receiver (체이닝 지원)
 */
fun Double.shouldBeZero(): Double {
    if (this != 0.0) {
        Failures.failComparison(
            Messages.expectedToBe("be", 0.0, this),
            0.0,
            this
        )
    }
    return this
}

// ── 범위 검증 (ClosedRange) ──────────────────────────────────────────────────

/**
 * receiver가 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (닫힌 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeInRange(range: ClosedRange<T>): T {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * receiver가 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (닫힌 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeInRange(range: ClosedRange<T>): T {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * receiver가 [range] (닫힌 범위) 안에 있는지 검증한다.
 *
 * [shouldBeInRange]의 별칭.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (닫힌 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeIn(range: ClosedRange<T>): T {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * receiver가 [range] (닫힌 범위) 안에 없는지 검증한다.
 *
 * [shouldNotBeInRange]의 별칭.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (닫힌 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeIn(range: ClosedRange<T>): T {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * receiver가 [range] (열린 범위, 끝 제외) 안에 있는지 검증한다.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (열린 끝 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldBeIn(range: OpenEndRange<T>): T {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * receiver가 [range] (열린 범위, 끝 제외) 안에 없는지 검증한다.
 *
 * @receiver 검증할 값
 * @param range 검증할 범위 (열린 끝 범위)
 * @return receiver (체이닝 지원)
 */
infix fun <T : Comparable<T>> T.shouldNotBeIn(range: OpenEndRange<T>): T {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

// ── 범위 검증 (Primitive ranges) ─────────────────────────────────────────────

/**
 * Int 값이 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 Int 값
 * @param range Int 범위
 * @return receiver (체이닝 지원)
 */
infix fun Int.shouldBeIn(range: IntRange): Int {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * Int 값이 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 Int 값
 * @param range Int 범위
 * @return receiver (체이닝 지원)
 */
infix fun Int.shouldNotBeIn(range: IntRange): Int {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * Long 값이 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 Long 값
 * @param range Long 범위
 * @return receiver (체이닝 지원)
 */
infix fun Long.shouldBeIn(range: LongRange): Long {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * Long 값이 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 Long 값
 * @param range Long 범위
 * @return receiver (체이닝 지원)
 */
infix fun Long.shouldNotBeIn(range: LongRange): Long {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * Char 값이 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 Char 값
 * @param range Char 범위
 * @return receiver (체이닝 지원)
 */
infix fun Char.shouldBeIn(range: CharRange): Char {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * Char 값이 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 Char 값
 * @param range Char 범위
 * @return receiver (체이닝 지원)
 */
infix fun Char.shouldNotBeIn(range: CharRange): Char {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * UInt 값이 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 UInt 값
 * @param range UInt 범위
 * @return receiver (체이닝 지원)
 */
infix fun UInt.shouldBeIn(range: UIntRange): UInt {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * UInt 값이 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 UInt 값
 * @param range UInt 범위
 * @return receiver (체이닝 지원)
 */
infix fun UInt.shouldNotBeIn(range: UIntRange): UInt {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

/**
 * ULong 값이 [range] 안에 있는지 검증한다.
 *
 * @receiver 검증할 ULong 값
 * @param range ULong 범위
 * @return receiver (체이닝 지원)
 */
infix fun ULong.shouldBeIn(range: ULongRange): ULong {
    if (this !in range) {
        Failures.fail("Expected <$this> to be in range $range, but was not.")
    }
    return this
}

/**
 * ULong 값이 [range] 안에 없는지 검증한다.
 *
 * @receiver 검증할 ULong 값
 * @param range ULong 범위
 * @return receiver (체이닝 지원)
 */
infix fun ULong.shouldNotBeIn(range: ULongRange): ULong {
    if (this in range) {
        Failures.fail("Expected <$this> to not be in range $range, but was.")
    }
    return this
}

// ── 근사값 검증 ───────────────────────────────────────────────────────────────

/**
 * Double 값이 [expected]와 [tolerance] 이내로 근사한지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않는다.
 *
 * @receiver 검증할 Double 값
 * @param expected 기대하는 근사 값
 * @param tolerance 허용 오차 (양수여야 함)
 * @return receiver (체이닝 지원)
 */
fun Double.shouldBeNear(expected: Double, tolerance: Double = DOUBLE_EPSILON): Double {
    if (this.isNaN() || expected.isNaN() || kotlin.math.abs(this - expected) > tolerance) {
        Failures.fail(
            "Expected <$this> to be near <$expected> within tolerance <$tolerance>, but was not."
        )
    }
    return this
}

/**
 * Float 값이 [expected]와 [tolerance] 이내로 근사한지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않는다.
 *
 * @receiver 검증할 Float 값
 * @param expected 기대하는 근사 값
 * @param tolerance 허용 오차 (양수여야 함)
 * @return receiver (체이닝 지원)
 */
fun Float.shouldBeNear(expected: Float, tolerance: Float = FLOAT_EPSILON): Float {
    if (this.isNaN() || expected.isNaN() || kotlin.math.abs(this - expected) > tolerance) {
        Failures.fail(
            "Expected <$this> to be near <$expected> within tolerance <$tolerance>, but was not."
        )
    }
    return this
}

/**
 * BigDecimal 값이 [expected]와 [tolerance] 이내로 근사한지 검증한다.
 *
 * @receiver 검증할 BigDecimal 값
 * @param expected 기대하는 근사 값
 * @param tolerance 허용 오차 (양수여야 함)
 * @return receiver (체이닝 지원)
 */
fun BigDecimal.shouldBeNear(expected: BigDecimal, tolerance: BigDecimal = BIGDECIMAL_EPSILON): BigDecimal {
    if ((this - expected).abs() > tolerance) {
        Failures.fail(
            "Expected <$this> to be near <$expected> within tolerance <$tolerance>, but was not."
        )
    }
    return this
}

/**
 * Double 값이 [expected]와 [delta] 이내로 근사하지 않는지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않으므로 이 검증은 항상 통과한다.
 *
 * @receiver 검증할 Double 값
 * @param expected 비교 기준 값
 * @param delta 허용 오차
 * @return receiver (체이닝 지원)
 */
fun Double.shouldNotBeNear(expected: Double, delta: Double): Double {
    if (!this.isNaN() && !expected.isNaN() && kotlin.math.abs(this - expected) <= delta) {
        Failures.fail(
            "Expected <$this> to not be near <$expected> within tolerance <$delta>, but was."
        )
    }
    return this
}

/**
 * Float 값이 [expected]와 [delta] 이내로 근사하지 않는지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않으므로 이 검증은 항상 통과한다.
 *
 * @receiver 검증할 Float 값
 * @param expected 비교 기준 값
 * @param delta 허용 오차
 * @return receiver (체이닝 지원)
 */
fun Float.shouldNotBeNear(expected: Float, delta: Float): Float {
    if (!this.isNaN() && !expected.isNaN() && kotlin.math.abs(this - expected) <= delta) {
        Failures.fail(
            "Expected <$this> to not be near <$expected> within tolerance <$delta>, but was."
        )
    }
    return this
}

/**
 * Double 값이 [expected]와 기본 허용 오차(1e-10) 이내로 근사하지 않는지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않으므로 이 검증은 항상 통과한다.
 *
 * @receiver 검증할 Double 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun Double.shouldNotBeNear(expected: Double): Double = shouldNotBeNear(expected, 1e-10)

/**
 * Float 값이 [expected]와 기본 허용 오차(1e-6f) 이내로 근사하지 않는지 검증한다.
 *
 * NaN은 어떤 값과도 근사하지 않으므로 이 검증은 항상 통과한다.
 *
 * @receiver 검증할 Float 값
 * @param expected 비교 기준 값
 * @return receiver (체이닝 지원)
 */
infix fun Float.shouldNotBeNear(expected: Float): Float = shouldNotBeNear(expected, 1e-6f)

//
// ── BigDecimal Equality (compareTo-based, scale-insensitive) ─────────────────
//

/** BigDecimal 수학적 동등성 검증 (scale 무관, compareTo 사용). */
infix fun BigDecimal?.shouldBeEqualTo(expected: BigDecimal?): BigDecimal? {
    if (this == null && expected == null) return null
    if (this == null || expected == null || this.compareTo(expected) != 0) {
        Failures.failComparison(Messages.expectedToBe("equal to", expected, this), expected, this)
    }
    return this
}

/** BigDecimal 수학적 비동등성 검증 (scale 무관, compareTo 사용). */
infix fun BigDecimal?.shouldNotBeEqualTo(expected: BigDecimal?): BigDecimal? {
    if (this != null && expected != null && this.compareTo(expected) == 0) {
        Failures.failComparison(Messages.expectedNotToBe("equal to", expected, this), expected, this)
    }
    return this
}
