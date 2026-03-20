package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.internal.AllBitsSet
import io.bluetape4k.idgenerators.ulid.utils.FullBytes
import io.bluetape4k.idgenerators.ulid.utils.MaxTimestamp
import io.bluetape4k.idgenerators.ulid.utils.MaxTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.MinTimestamp
import io.bluetape4k.idgenerators.ulid.utils.MinTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.MockRandom
import io.bluetape4k.idgenerators.ulid.utils.PastTimestamp
import io.bluetape4k.idgenerators.ulid.utils.PastTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.PatternBytes
import io.bluetape4k.idgenerators.ulid.utils.PatternLeastSignificantBits
import io.bluetape4k.idgenerators.ulid.utils.PatternMostSignificantBits
import io.bluetape4k.idgenerators.ulid.utils.ZeroBytes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class ULIDFactoryTest : AbstractULIDTest() {
    companion object : KLogging()

    @Nested
    inner class RandomULID {
        @RepeatedTest(REPEAT_SIZE)
        fun `randomULID with valid timestamp`() {
            val result = ULID.randomULID()
            assertValidParts(result)
        }

        @Test
        fun `randomULID with random 0`() {
            val random = MockRandom(0)
            val factory = ULID.factory(random)

            val result = factory.randomULID()
            random.nextLong() shouldBeEqualTo 0
            assertValidParts(result)
        }

        @Test
        fun `randomULID with random -1`() {
            val random = MockRandom(-1)
            val factory = ULID.factory(random)

            val result = factory.randomULID()
            random.nextLong() shouldBeEqualTo -1L
            assertValidParts(result)
        }

        @Test
        fun `random ULID with invalid timestamp`() {
            assertFailsWith<IllegalArgumentException> {
                ULID.randomULID(0x0001000000000000L)
            }
        }
    }

    @Nested
    inner class NextULID {
        @RepeatedTest(REPEAT_SIZE)
        fun `nextULID - valid timestamp`() {
            val result = ULID.nextULID().toString()
            assertValidParts(result)
        }

        @Test
        fun `nextULID with random 0`() {
            val random = MockRandom(0)
            val factory = ULID.factory(random)

            val result = factory.nextULID().toString()
            random.nextLong() shouldBeEqualTo 0
            assertValidParts(result)
        }

        @Test
        fun `nextULID with random -1`() {
            val random = MockRandom(-1)
            val factory = ULID.factory(random)

            val result = factory.nextULID().toString()
            random.nextLong() shouldBeEqualTo -1L
            assertValidParts(result)
        }

        @Test
        fun `nextULID with invalid timestamp`() {
            assertFailsWith<IllegalArgumentException> {
                ULID.nextULID(0x0001000000000000L)
            }
        }
    }

    @Nested
    inner class ToByteArray {
        inner class Input(
            val data: ByteArray,
            val mostSignificantBits: Long,
            val leastSignificantBits: Long,
        )

        private val inputs
            get() =
                listOf(
                    Input(ZeroBytes, 0L, 0L),
                    Input(FullBytes, AllBitsSet, AllBitsSet),
                    Input(PatternBytes, PatternMostSignificantBits, PatternLeastSignificantBits)
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `fromByteArray - parse ulid`(input: Input) {
            val ulid = ULID.fromByteArray(input.data)
            ulid.mostSignificantBits shouldBeEqualTo input.mostSignificantBits
            ulid.leastSignificantBits shouldBeEqualTo input.leastSignificantBits
        }

        @Test
        fun `fromByteArray with invalid length bytes`() {
            assertFailsWith<IllegalArgumentException> {
                ULID.fromByteArray(ByteArray(15))
            }

            assertFailsWith<IllegalArgumentException> {
                ULID.fromByteArray(ByteArray(17))
            }
        }
    }

    @Nested
    inner class ParseULID {
        inner class Input(
            val ulidString: String,
            val expectedTimestamp: Long,
        )

        private val inputs
            get() =
                listOf(
                    Input(PastTimestampPart + "0000000000000000", PastTimestamp),
                    Input(PastTimestampPart + "ZZZZZZZZZZZZZZZZ", PastTimestamp),
                    Input(PastTimestampPart + "123456789ABCDEFG", PastTimestamp),
                    Input(PastTimestampPart + "1000000000000000", PastTimestamp),
                    Input(PastTimestampPart + "1000000000000001", PastTimestamp),
                    Input(PastTimestampPart + "0001000000000001", PastTimestamp),
                    Input(PastTimestampPart + "0100000000000001", PastTimestamp),
                    Input(PastTimestampPart + "0000000000000001", PastTimestamp),
                    Input(MinTimestampPart + "123456789ABCDEFG", MinTimestamp),
                    Input(MaxTimestampPart + "123456789ABCDEFG", MaxTimestamp)
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `parseULID and toString`(input: Input) {
            val ulid = ULID.parseULID(input.ulidString)
            ulid.toString() shouldBeEqualTo input.ulidString
            ulid.timestamp shouldBeEqualTo input.expectedTimestamp
        }

        inner class InvalidInput(
            val ulidString: String,
            val expectedString: String,
            val expectedTimestamp: Long,
        )

        val invalidInputs
            get() =
                listOf(
                    InvalidInput(
                        PastTimestampPart + "0l00000000000000",
                        PastTimestampPart + "0100000000000000",
                        PastTimestamp
                    ),
                    InvalidInput(
                        PastTimestampPart + "0L00000000000000",
                        PastTimestampPart + "0100000000000000",
                        PastTimestamp
                    ),
                    InvalidInput(
                        PastTimestampPart + "0i00000000000000",
                        PastTimestampPart + "0100000000000000",
                        PastTimestamp
                    ),
                    InvalidInput(
                        PastTimestampPart + "0I00000000000000",
                        PastTimestampPart + "0100000000000000",
                        PastTimestamp
                    ),
                    InvalidInput(
                        PastTimestampPart + "0o00000000000000",
                        PastTimestampPart + "0000000000000000",
                        PastTimestamp
                    ),
                    InvalidInput(
                        PastTimestampPart + "0O00000000000000",
                        PastTimestampPart + "0000000000000000",
                        PastTimestamp
                    )
                )

        @ParameterizedTest
        @MethodSource("getInvalidInputs")
        fun `parseULID with invalid ulid`(invalid: InvalidInput) {
            val ulid = ULID.parseULID(invalid.ulidString)
            ulid.toString() shouldBeEqualTo invalid.expectedString
            ulid.timestamp shouldBeEqualTo invalid.expectedTimestamp
        }

        @Test
        fun `parseULID with invalid input`() {
            val inputs =
                listOf(
                    "0000000000000000000000000",
                    "000000000000000000000000000",
                    "80000000000000000000000000"
                )
            inputs.forEach { input ->
                assertFailsWith<IllegalArgumentException> {
                    ULID.parseULID(input)
                }
            }
        }
    }
}
