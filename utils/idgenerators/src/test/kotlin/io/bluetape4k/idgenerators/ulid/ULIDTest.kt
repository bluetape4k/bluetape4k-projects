package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.idgenerators.ulid.internal.AllBitsSet
import io.bluetape4k.idgenerators.ulid.internal.ULIDValue
import io.bluetape4k.idgenerators.ulid.utils.FullBytes
import io.bluetape4k.idgenerators.ulid.utils.PatternBytes
import io.bluetape4k.idgenerators.ulid.utils.PatternLeastSignificantBits
import io.bluetape4k.idgenerators.ulid.utils.PatternMostSignificantBits
import io.bluetape4k.idgenerators.ulid.utils.ZeroBytes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ULIDTest : AbstractULIDTest() {
    companion object : KLogging()

    @Nested
    inner class ToByteArray {
        inner class Input(
            val mostSignificantBits: Long,
            val leastSignificantBits: Long,
            val expectedData: ByteArray,
        )

        private val inputs
            get() =
                listOf(
                    Input(0L, 0L, ZeroBytes),
                    Input(AllBitsSet, AllBitsSet, FullBytes),
                    Input(PatternMostSignificantBits, PatternLeastSignificantBits, PatternBytes)
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `ULID toByteArray`(input: Input) {
            with(input) {
                val ulidValue = ULIDValue(mostSignificantBits, leastSignificantBits)
                val bytes = ulidValue.toByteArray()
                bytes shouldBeEqualTo expectedData
            }
        }
    }

    @Nested
    inner class Comparable {
        inner class Input(
            val mostSignificantBits1: Long,
            val leastSignificantBits1: Long,
            val mostSignificantBits2: Long,
            val leastSignificantBits2: Long,
            val compare: Int,
        )

        private val inputs
            get() =
                listOf(
                    Input(0L, 0L, 0L, 0L, 0),
                    Input(AllBitsSet, AllBitsSet, AllBitsSet, AllBitsSet, 0),
                    Input(
                        PatternMostSignificantBits,
                        PatternLeastSignificantBits,
                        PatternMostSignificantBits,
                        PatternLeastSignificantBits,
                        0
                    ),
                    Input(0L, 1L, 0L, 0L, 1),
                    Input(1 shl 16, 0L, 0L, 0L, 1)
                )

        @ParameterizedTest
        @MethodSource("getInputs")
        fun `ULID compareTo`(input: Input) {
            with(input) {
                val value1 = ULIDValue(mostSignificantBits1, leastSignificantBits1)
                val value2 = ULIDValue(mostSignificantBits2, leastSignificantBits2)

                val equals12 = value1 == value2
                val equals21 = value2 == value1
                val compare12 = value1.compareTo(value2)
                val compare21 = value2.compareTo(value1)
                val hash1 = value1.hashCode()
                val hash2 = value2.hashCode()

                equals21 shouldBeEqualTo equals12
                compare12 shouldBeEqualTo compare21 * -1

                when (compare) {
                    0 -> {
                        hash2 shouldBeEqualTo hash1
                    }
                    else -> {
                        compare12 shouldBeEqualTo compare
                        equals12.shouldBeFalse()
                    }
                }
            }
        }

        @Test
        fun `compare unsigned edge cases`() {
            // LSB sign bit: 0x8000000000000000 (Long.MIN_VALUE) should be > 0x7FFFFFFFFFFFFFFF (Long.MAX_VALUE) unsigned
            val a = ULIDValue(100L, Long.MAX_VALUE)
            val b = ULIDValue(100L, Long.MIN_VALUE)
            a shouldBeLessThan b
            b shouldBeGreaterThan a

            // MSB sign bit
            val c = ULIDValue(Long.MAX_VALUE, 0L)
            val d = ULIDValue(Long.MIN_VALUE, 0L)
            c shouldBeLessThan d
            d shouldBeGreaterThan c

            // Both bits set
            val e = ULIDValue(Long.MIN_VALUE, Long.MAX_VALUE)
            val f = ULIDValue(Long.MIN_VALUE, Long.MIN_VALUE)
            f shouldBeGreaterThan e

            // All bits set should be the maximum
            val max = ULIDValue(AllBitsSet, AllBitsSet)
            val almostMax = ULIDValue(AllBitsSet, AllBitsSet - 1L)
            max shouldBeGreaterThan almostMax
        }
    }

    @Test
    fun `increment ulid value`() {
        // Basic increment
        val a = ULIDValue(0L, 0L)
        val b = a.increment()
        b shouldBeEqualTo ULIDValue(0L, 1L)
    }

    @Test
    fun `nextULIDStrict with lsb max value`() {
        // When LSB = Long.MAX_VALUE and timestamp matches, increment gives LSB = Long.MIN_VALUE
        // which should be considered greater (unsigned), so nextULIDStrict should return non-null
        val previous = ULIDValue(0L, Long.MAX_VALUE)
        val result = ULID.Monotonic.nextULIDStrict(previous, 0).shouldNotBeNull()
        result shouldBeEqualTo ULIDValue(0, Long.MIN_VALUE)
        result shouldBeGreaterThan previous
    }

    @Nested
    inner class Encode {
        private val values: List<ULIDValue>
            get() =
                listOf(
                    ULIDValue(0L, 0L),
                    ULIDValue(AllBitsSet, AllBitsSet),
                    ULIDValue(PatternMostSignificantBits, PatternLeastSignificantBits),
                    ULIDValue(Long.MAX_VALUE, Long.MIN_VALUE),
                    ULIDValue(Long.MIN_VALUE, Long.MAX_VALUE)
                )

        @ParameterizedTest
        @MethodSource("getValues")
        fun `roundtrip crockford encode decode`(original: ULIDValue) {
            val encoded = original.toString()
            val decoded = ULID.parseULID(encoded)
            decoded shouldBeEqualTo original
        }
    }
}
