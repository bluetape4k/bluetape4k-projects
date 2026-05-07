package io.bluetape4k.assertions

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opentest4j.AssertionFailedError

class NumericalTest {

    // ── shouldBeGreaterThan ────────────────────────────────────────────────

    @Test
    fun `shouldBeGreaterThan passes when receiver is greater`() {
        5 shouldBeGreaterThan 3
        5L shouldBeGreaterThan 3L
        5.0 shouldBeGreaterThan 3.0
        5.0f shouldBeGreaterThan 3.0f
        'z' shouldBeGreaterThan 'a'
    }

    @Test
    fun `shouldBeGreaterThan fails when receiver is equal`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldBeGreaterThan 5
        }
    }

    @Test
    fun `shouldBeGreaterThan fails when receiver is less`() {
        assertFailsWith<AssertionFailedError> {
            3 shouldBeGreaterThan 5
        }
    }

    // ── shouldBeGreaterOrEqualTo ───────────────────────────────────────────

    @Test
    fun `shouldBeGreaterOrEqualTo passes when receiver is greater`() {
        5 shouldBeGreaterOrEqualTo 3
    }

    @Test
    fun `shouldBeGreaterOrEqualTo passes when receiver is equal`() {
        5 shouldBeGreaterOrEqualTo 5
        5L shouldBeGreaterOrEqualTo 5L
        5.0 shouldBeGreaterOrEqualTo 5.0
    }

    @Test
    fun `shouldBeGreaterOrEqualTo fails when receiver is less`() {
        assertFailsWith<AssertionFailedError> {
            3 shouldBeGreaterOrEqualTo 5
        }
    }

    // ── shouldBeLessThan ───────────────────────────────────────────────────

    @Test
    fun `shouldBeLessThan passes when receiver is less`() {
        3 shouldBeLessThan 5
        3L shouldBeLessThan 5L
        3.0 shouldBeLessThan 5.0
        'a' shouldBeLessThan 'z'
    }

    @Test
    fun `shouldBeLessThan fails when receiver is equal`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldBeLessThan 5
        }
    }

    @Test
    fun `shouldBeLessThan fails when receiver is greater`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldBeLessThan 3
        }
    }

    // ── shouldBeLessOrEqualTo ─────────────────────────────────────────────

    @Test
    fun `shouldBeLessOrEqualTo passes when receiver is less`() {
        3 shouldBeLessOrEqualTo 5
    }

    @Test
    fun `shouldBeLessOrEqualTo passes when receiver is equal`() {
        5 shouldBeLessOrEqualTo 5
        5L shouldBeLessOrEqualTo 5L
    }

    @Test
    fun `shouldBeLessOrEqualTo fails when receiver is greater`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldBeLessOrEqualTo 3
        }
    }

    // ── shouldNotBeGreaterThan ─────────────────────────────────────────────

    @Test
    fun `shouldNotBeGreaterThan passes when receiver is equal`() {
        5 shouldNotBeGreaterThan 5
    }

    @Test
    fun `shouldNotBeGreaterThan passes when receiver is less`() {
        3 shouldNotBeGreaterThan 5
    }

    @Test
    fun `shouldNotBeGreaterThan fails when receiver is greater`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeGreaterThan 3
        }
    }

    // ── shouldNotBeGreaterOrEqualTo ────────────────────────────────────────

    @Test
    fun `shouldNotBeGreaterOrEqualTo passes when receiver is less`() {
        3 shouldNotBeGreaterOrEqualTo 5
    }

    @Test
    fun `shouldNotBeGreaterOrEqualTo fails when receiver is equal`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeGreaterOrEqualTo 5
        }
    }

    @Test
    fun `shouldNotBeGreaterOrEqualTo fails when receiver is greater`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeGreaterOrEqualTo 3
        }
    }

    // ── shouldNotBeLessThan ────────────────────────────────────────────────

    @Test
    fun `shouldNotBeLessThan passes when receiver is equal`() {
        5 shouldNotBeLessThan 5
    }

    @Test
    fun `shouldNotBeLessThan passes when receiver is greater`() {
        5 shouldNotBeLessThan 3
    }

    @Test
    fun `shouldNotBeLessThan fails when receiver is less`() {
        assertFailsWith<AssertionFailedError> {
            3 shouldNotBeLessThan 5
        }
    }

    // ── shouldNotBeLessOrEqualTo ───────────────────────────────────────────

    @Test
    fun `shouldNotBeLessOrEqualTo passes when receiver is greater`() {
        5 shouldNotBeLessOrEqualTo 3
    }

    @Test
    fun `shouldNotBeLessOrEqualTo fails when receiver is equal`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeLessOrEqualTo 5
        }
    }

    @Test
    fun `shouldNotBeLessOrEqualTo fails when receiver is less`() {
        assertFailsWith<AssertionFailedError> {
            3 shouldNotBeLessOrEqualTo 5
        }
    }

    // ── chaining: comparison ───────────────────────────────────────────────

    @Test
    fun `comparison assertions support chaining`() {
        10
            .shouldBeGreaterThan(5)
            .shouldBeGreaterOrEqualTo(10)
            .shouldBeLessOrEqualTo(10)
            .shouldBeLessThan(20)
    }

    // ── shouldBePositive ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [1, 42, Int.MAX_VALUE])
    fun `Int shouldBePositive passes for positive values`(value: Int) {
        value.shouldBePositive()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, Int.MIN_VALUE])
    fun `Int shouldBePositive fails for zero or negative`(value: Int) {
        assertFailsWith<AssertionFailedError> {
            value.shouldBePositive()
        }
    }

    @Test
    fun `Long shouldBePositive passes`() {
        1L.shouldBePositive()
        Long.MAX_VALUE.shouldBePositive()
    }

    @Test
    fun `Long shouldBePositive fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0L.shouldBePositive()
        }
    }

    @Test
    fun `Long shouldBePositive fails for negative`() {
        assertFailsWith<AssertionFailedError> {
            (-1L).shouldBePositive()
        }
    }

    @Test
    fun `Double shouldBePositive passes`() {
        0.001.shouldBePositive()
        Double.MAX_VALUE.shouldBePositive()
    }

    @Test
    fun `Double shouldBePositive fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0.0.shouldBePositive()
        }
    }

    @Test
    fun `Float shouldBePositive passes`() {
        0.001f.shouldBePositive()
    }

    @Test
    fun `Float shouldBePositive fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0.0f.shouldBePositive()
        }
    }

    @Test
    fun `Byte shouldBePositive passes`() {
        1.toByte().shouldBePositive()
    }

    @Test
    fun `Byte shouldBePositive fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0.toByte().shouldBePositive()
        }
    }

    @Test
    fun `Short shouldBePositive passes`() {
        1.toShort().shouldBePositive()
    }

    @Test
    fun `Short shouldBePositive fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0.toShort().shouldBePositive()
        }
    }

    // ── shouldBeNegative ───────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = [-1, -42, Int.MIN_VALUE])
    fun `Int shouldBeNegative passes for negative values`(value: Int) {
        value.shouldBeNegative()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, Int.MAX_VALUE])
    fun `Int shouldBeNegative fails for zero or positive`(value: Int) {
        assertFailsWith<AssertionFailedError> {
            value.shouldBeNegative()
        }
    }

    @Test
    fun `Long shouldBeNegative passes`() {
        (-1L).shouldBeNegative()
    }

    @Test
    fun `Long shouldBeNegative fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0L.shouldBeNegative()
        }
    }

    @Test
    fun `Double shouldBeNegative passes`() {
        (-0.001).shouldBeNegative()
    }

    @Test
    fun `Double shouldBeNegative fails for zero`() {
        assertFailsWith<AssertionFailedError> {
            0.0.shouldBeNegative()
        }
    }

    @Test
    fun `Float shouldBeNegative passes`() {
        (-0.001f).shouldBeNegative()
    }

    @Test
    fun `Float shouldBeNegative fails for positive`() {
        assertFailsWith<AssertionFailedError> {
            1.0f.shouldBeNegative()
        }
    }

    @Test
    fun `Byte shouldBeNegative passes`() {
        (-1).toByte().shouldBeNegative()
    }

    @Test
    fun `Short shouldBeNegative passes`() {
        (-1).toShort().shouldBeNegative()
    }

    // ── shouldBeZero ───────────────────────────────────────────────────────

    @Test
    fun `Int shouldBeZero passes for zero`() {
        0.shouldBeZero()
    }

    @Test
    fun `Int shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            1.shouldBeZero()
        }
    }

    @Test
    fun `Long shouldBeZero passes`() {
        0L.shouldBeZero()
    }

    @Test
    fun `Long shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            1L.shouldBeZero()
        }
    }

    @Test
    fun `Double shouldBeZero passes`() {
        0.0.shouldBeZero()
    }

    @Test
    fun `Double shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            1.0.shouldBeZero()
        }
    }

    @Test
    fun `Float shouldBeZero passes`() {
        0.0f.shouldBeZero()
    }

    @Test
    fun `Float shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            0.1f.shouldBeZero()
        }
    }

    @Test
    fun `Byte shouldBeZero passes`() {
        0.toByte().shouldBeZero()
    }

    @Test
    fun `Byte shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            1.toByte().shouldBeZero()
        }
    }

    @Test
    fun `Short shouldBeZero passes`() {
        0.toShort().shouldBeZero()
    }

    @Test
    fun `Short shouldBeZero fails for nonzero`() {
        assertFailsWith<AssertionFailedError> {
            1.toShort().shouldBeZero()
        }
    }

    // ── shouldBeInRange / shouldNotBeInRange (ClosedRange) ─────────────────

    @Test
    fun `shouldBeInRange passes when value is inside closed range`() {
        5 shouldBeInRange 1..10
        5.0 shouldBeInRange 1.0..10.0
        'e' shouldBeInRange 'a'..'z'
    }

    @Test
    fun `shouldBeInRange passes at boundaries`() {
        1 shouldBeInRange 1..10
        10 shouldBeInRange 1..10
    }

    @Test
    fun `shouldBeInRange fails when value is outside range`() {
        assertFailsWith<AssertionFailedError> {
            0 shouldBeInRange 1..10
        }
    }

    @Test
    fun `shouldBeInRange fails when value is above range`() {
        assertFailsWith<AssertionFailedError> {
            11 shouldBeInRange 1..10
        }
    }

    @Test
    fun `shouldNotBeInRange passes when value is outside range`() {
        0 shouldNotBeInRange 1..10
        11 shouldNotBeInRange 1..10
    }

    @Test
    fun `shouldNotBeInRange fails when value is inside range`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeInRange 1..10
        }
    }

    @Test
    fun `shouldNotBeInRange fails at boundary`() {
        assertFailsWith<AssertionFailedError> {
            1 shouldNotBeInRange 1..10
        }
    }

    // ── shouldBeIn / shouldNotBeIn (ClosedRange) ───────────────────────────

    @Test
    fun `shouldBeIn with ClosedRange passes`() {
        5 shouldBeIn 1..10
        'e' shouldBeIn 'a'..'z'
    }

    @Test
    fun `shouldBeIn with ClosedRange fails for out-of-range value`() {
        assertFailsWith<AssertionFailedError> {
            11 shouldBeIn 1..10
        }
    }

    @Test
    fun `shouldNotBeIn with ClosedRange passes`() {
        11 shouldNotBeIn 1..10
        0 shouldNotBeIn 1..10
    }

    @Test
    fun `shouldNotBeIn with ClosedRange fails for in-range value`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeIn 1..10
        }
    }

    // ── shouldBeIn / shouldNotBeIn (OpenEndRange) ─────────────────────────

    @Test
    fun `shouldBeIn with OpenEndRange passes for values before end`() {
        5 shouldBeIn 1..<10
        9 shouldBeIn 1..<10
    }

    @Test
    fun `shouldBeIn with OpenEndRange fails at end exclusive boundary`() {
        assertFailsWith<AssertionFailedError> {
            10 shouldBeIn 1..<10
        }
    }

    @Test
    fun `shouldBeIn with OpenEndRange fails below start`() {
        assertFailsWith<AssertionFailedError> {
            0 shouldBeIn 1..<10
        }
    }

    @Test
    fun `shouldNotBeIn with OpenEndRange passes at end exclusive boundary`() {
        10 shouldNotBeIn 1..<10
        0 shouldNotBeIn 1..<10
    }

    @Test
    fun `shouldNotBeIn with OpenEndRange fails for value inside`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeIn 1..<10
        }
    }

    // ── shouldBeIn / shouldNotBeIn (IntRange) ─────────────────────────────

    @Test
    fun `Int shouldBeIn IntRange passes`() {
        5 shouldBeIn (1..10)
    }

    @Test
    fun `Int shouldBeIn IntRange fails`() {
        assertFailsWith<AssertionFailedError> {
            11 shouldBeIn (1..10)
        }
    }

    @Test
    fun `Int shouldNotBeIn IntRange passes`() {
        11 shouldNotBeIn (1..10)
    }

    @Test
    fun `Int shouldNotBeIn IntRange fails`() {
        assertFailsWith<AssertionFailedError> {
            5 shouldNotBeIn (1..10)
        }
    }

    // ── shouldBeIn / shouldNotBeIn (LongRange) ────────────────────────────

    @Test
    fun `Long shouldBeIn LongRange passes`() {
        5L shouldBeIn (1L..10L)
    }

    @Test
    fun `Long shouldBeIn LongRange fails`() {
        assertFailsWith<AssertionFailedError> {
            11L shouldBeIn (1L..10L)
        }
    }

    @Test
    fun `Long shouldNotBeIn LongRange passes`() {
        11L shouldNotBeIn (1L..10L)
    }

    @Test
    fun `Long shouldNotBeIn LongRange fails`() {
        assertFailsWith<AssertionFailedError> {
            5L shouldNotBeIn (1L..10L)
        }
    }

    // ── shouldBeIn / shouldNotBeIn (CharRange) ────────────────────────────

    @Test
    fun `Char shouldBeIn CharRange passes`() {
        'e' shouldBeIn ('a'..'z')
    }

    @Test
    fun `Char shouldBeIn CharRange fails`() {
        assertFailsWith<AssertionFailedError> {
            'A' shouldBeIn ('a'..'z')
        }
    }

    @Test
    fun `Char shouldNotBeIn CharRange passes`() {
        'A' shouldNotBeIn ('a'..'z')
    }

    @Test
    fun `Char shouldNotBeIn CharRange fails`() {
        assertFailsWith<AssertionFailedError> {
            'e' shouldNotBeIn ('a'..'z')
        }
    }

    // ── shouldBeIn / shouldNotBeIn (UIntRange) ────────────────────────────

    @Test
    fun `UInt shouldBeIn UIntRange passes`() {
        5u shouldBeIn (1u..10u)
    }

    @Test
    fun `UInt shouldBeIn UIntRange fails`() {
        assertFailsWith<AssertionFailedError> {
            11u shouldBeIn (1u..10u)
        }
    }

    @Test
    fun `UInt shouldNotBeIn UIntRange passes`() {
        11u shouldNotBeIn (1u..10u)
    }

    @Test
    fun `UInt shouldNotBeIn UIntRange fails`() {
        assertFailsWith<AssertionFailedError> {
            5u shouldNotBeIn (1u..10u)
        }
    }

    // ── shouldBeIn / shouldNotBeIn (ULongRange) ───────────────────────────

    @Test
    fun `ULong shouldBeIn ULongRange passes`() {
        5uL shouldBeIn (1uL..10uL)
    }

    @Test
    fun `ULong shouldBeIn ULongRange fails`() {
        assertFailsWith<AssertionFailedError> {
            11uL shouldBeIn (1uL..10uL)
        }
    }

    @Test
    fun `ULong shouldNotBeIn ULongRange passes`() {
        11uL shouldNotBeIn (1uL..10uL)
    }

    @Test
    fun `ULong shouldNotBeIn ULongRange fails`() {
        assertFailsWith<AssertionFailedError> {
            5uL shouldNotBeIn (1uL..10uL)
        }
    }

    // ── shouldBeNear (Double) ─────────────────────────────────────────────

    @Test
    fun `Double shouldBeNear passes when within tolerance`() {
        1.0.shouldBeNear(1.0, 1e-9)
        1.0.shouldBeNear(1.0000001, 1e-6)
    }

    @Test
    fun `Double shouldBeNear passes at exact tolerance boundary`() {
        // |1.0 - 1.5| = 0.5, tolerance = 0.5 → passes (<=)
        1.0.shouldBeNear(1.5, 0.5)
    }

    @Test
    fun `Double shouldBeNear fails when difference exceeds tolerance`() {
        assertFailsWith<AssertionFailedError> {
            1.0.shouldBeNear(2.0, 0.5)
        }
    }

    @Test
    fun `Double shouldBeNear fails when receiver is NaN`() {
        assertFailsWith<AssertionFailedError> {
            Double.NaN.shouldBeNear(1.0, 1e-6)
        }
    }

    @Test
    fun `Double shouldBeNear fails when expected is NaN`() {
        assertFailsWith<AssertionFailedError> {
            1.0.shouldBeNear(Double.NaN, 1e-6)
        }
    }

    @Test
    fun `Double shouldBeNear fails when both are NaN`() {
        assertFailsWith<AssertionFailedError> {
            Double.NaN.shouldBeNear(Double.NaN, 1e-6)
        }
    }

    // ── shouldBeNear (Float) ──────────────────────────────────────────────

    @Test
    fun `Float shouldBeNear passes when within tolerance`() {
        1.0f.shouldBeNear(1.0f, 1e-6f)
        1.0f.shouldBeNear(1.0000001f, 1e-6f)
    }

    @Test
    fun `Float shouldBeNear fails when difference exceeds tolerance`() {
        assertFailsWith<AssertionFailedError> {
            1.0f.shouldBeNear(2.0f, 0.5f)
        }
    }

    @Test
    fun `Float shouldBeNear fails when receiver is NaN`() {
        assertFailsWith<AssertionFailedError> {
            Float.NaN.shouldBeNear(1.0f, 1e-6f)
        }
    }

    @Test
    fun `Float shouldBeNear fails when expected is NaN`() {
        assertFailsWith<AssertionFailedError> {
            1.0f.shouldBeNear(Float.NaN, 1e-6f)
        }
    }

    // ── shouldNotBeNear (Double) ───────────────────────────────────────────

    @Test
    fun `Double shouldNotBeNear passes when difference exceeds delta`() {
        1.0.shouldNotBeNear(2.0, 0.5)
    }

    @Test
    fun `Double shouldNotBeNear fails when within delta`() {
        assertFailsWith<AssertionFailedError> {
            1.0.shouldNotBeNear(1.0, 1e-9)
        }
    }

    @Test
    fun `Double shouldNotBeNear passes when receiver is NaN`() {
        // NaN never equals anything, so shouldNotBeNear always passes with NaN
        Double.NaN.shouldNotBeNear(1.0, 1e-6)
    }

    @Test
    fun `Double shouldNotBeNear passes when expected is NaN`() {
        1.0.shouldNotBeNear(Double.NaN, 1e-6)
    }

    // ── shouldNotBeNear (Float) ────────────────────────────────────────────

    @Test
    fun `Float shouldNotBeNear passes when difference exceeds delta`() {
        1.0f.shouldNotBeNear(2.0f, 0.5f)
    }

    @Test
    fun `Float shouldNotBeNear fails when within delta`() {
        assertFailsWith<AssertionFailedError> {
            1.0f.shouldNotBeNear(1.0f, 1e-6f)
        }
    }

    @Test
    fun `Float shouldNotBeNear passes when receiver is NaN`() {
        Float.NaN.shouldNotBeNear(1.0f, 1e-6f)
    }

    // ── chaining: near ────────────────────────────────────────────────────

    @Test
    fun `shouldBeNear supports chaining`() {
        3.14159
            .shouldBeNear(3.14159, 1e-10)
            .shouldBeGreaterThan(3.0)
            .shouldBeLessThan(4.0)
    }
}
