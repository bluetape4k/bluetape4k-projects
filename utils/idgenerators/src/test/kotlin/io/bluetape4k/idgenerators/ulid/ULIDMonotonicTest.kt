package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.internal.ULIDValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ULIDMonotonicTest: AbstractULIDTest() {
    companion object: KLogging()

    @Nested
    inner class NextULID {
        inner class Input(
            val previousValue: ULID,
            val expectedResult: ULID,
        )

        private val inputs
            get() =
                listOf(
                    Input(ULIDValue(0, 0), ULIDValue(0, 1)),
                    Input(ULIDValue(0, -0x2L), ULIDValue(0, -0x1L)),
                    Input(ULIDValue(0, -0x1L), ULIDValue(1, 0)),
                    Input(ULIDValue(0xFFFFL, -0x1L), ULIDValue(0, 0))
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `nextULID with zero timestamp`(input: Input) {
            val nextULID = ULID.Monotonic.nextULID(input.previousValue, 0L)
            nextULID shouldBeEqualTo input.expectedResult
        }

        @Test
        fun `nextULID when different timestamp`() {
            val previousValue = ULIDValue(0, 0)

            val nextULID1 = ULID.Monotonic.nextULID(previousValue, 1)
            nextULID1.timestamp shouldBeEqualTo 1

            val nextULID2 = ULID.Monotonic.nextULID(nextULID1)
            nextULID2.timestamp shouldBeGreaterThan 0L
        }
    }

    @Nested
    inner class NextULIDStrict {
        inner class Input(
            val previousValue: ULID,
            val expectedResult: ULID?,
        )

        private val inputs
            get() =
                listOf(
                    Input(ULIDValue(0, 0), ULIDValue(0, 1)),
                    Input(ULIDValue(0, -0x2L), ULIDValue(0, -0x1L)),
                    Input(ULIDValue(0, -0x1L), ULIDValue(1L, 0L)),
                    Input(ULIDValue(0xFFFFL, -0x1L), null)
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `nextULIDStrict with zero timestamp`(input: Input) {
            val nextULID = ULID.Monotonic.nextULIDStrict(input.previousValue, 0L)
            nextULID shouldBeEqualTo input.expectedResult
        }

        @Test
        fun `nextULIDStrict when different timestamp`() {
            val previousValue = ULIDValue(0, 0)

            val nextULID1 = ULID.Monotonic.nextULIDStrict(previousValue, 1).shouldNotBeNull()
            nextULID1.timestamp shouldBeEqualTo 1

            val nextULID2 = ULID.Monotonic.nextULIDStrict(previousValue).shouldNotBeNull()
            nextULID2.timestamp shouldBeGreaterThan 0
        }
    }

    @Test
    fun `nextULID factory`() {
        val monotonic = ULID.monotonic(factory = ULID.factory())
        val prevValue = ULIDValue(0, 0)
        val result = monotonic.nextULID(prevValue).toString()

        assertValidParts(result)
    }
}
