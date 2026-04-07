package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.utils.MaxTimestamp
import io.bluetape4k.idgenerators.ulid.utils.MaxTimestampPart
import io.bluetape4k.idgenerators.ulid.utils.PastTimestamp
import io.bluetape4k.idgenerators.ulid.utils.PastTimestampPart
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import kotlin.test.assertFailsWith

class CrockfordTest {
    data class Input(
        val inputValue: Long,
        val bufferSize: Int,
        val length: Int,
        val offset: Int,
        val expectedResult: String,
    ): Serializable

    private val inputs
        get() =
            listOf(
                Input(0L, 1, 1, 0, "0"),
                Input(1L, 1, 1, 0, "1"),
                Input(2L, 1, 1, 0, "2"),
                Input(3L, 1, 1, 0, "3"),
                Input(4L, 1, 1, 0, "4"),
                Input(5L, 1, 1, 0, "5"),
                Input(6L, 1, 1, 0, "6"),
                Input(7L, 1, 1, 0, "7"),
                Input(8L, 1, 1, 0, "8"),
                Input(9L, 1, 1, 0, "9"),
                Input(10L, 1, 1, 0, "A"),
                Input(11L, 1, 1, 0, "B"),
                Input(12L, 1, 1, 0, "C"),
                Input(13L, 1, 1, 0, "D"),
                Input(14L, 1, 1, 0, "E"),
                Input(15L, 1, 1, 0, "F"),
                Input(16L, 1, 1, 0, "G"),
                Input(17L, 1, 1, 0, "H"),
                Input(18L, 1, 1, 0, "J"),
                Input(19L, 1, 1, 0, "K"),
                Input(20L, 1, 1, 0, "M"),
                Input(21L, 1, 1, 0, "N"),
                Input(22L, 1, 1, 0, "P"),
                Input(23L, 1, 1, 0, "Q"),
                Input(24L, 1, 1, 0, "R"),
                Input(25L, 1, 1, 0, "S"),
                Input(26L, 1, 1, 0, "T"),
                Input(27L, 1, 1, 0, "V"),
                Input(28L, 1, 1, 0, "W"),
                Input(29L, 1, 1, 0, "X"),
                Input(30L, 1, 1, 0, "Y"),
                Input(31L, 1, 1, 0, "Z"),
                Input(32L, 1, 1, 0, "0"),
                Input(32L, 2, 2, 0, "10"),
                Input(0L, 0, 0, 0, ""),
                Input(0L, 2, 0, 0, "##"),
                Input(0L, 13, 13, 0, "0000000000000"),
                Input(194L, 2, 2, 0, "62"),
                Input(45_678L, 4, 4, 0, "1CKE"),
                Input(393_619L, 4, 4, 0, "C0CK"),
                Input(398_373L, 4, 4, 0, "C515"),
                Input(421_562L, 4, 4, 0, "CVNT"),
                Input(456_789L, 4, 4, 0, "DY2N"),
                Input(519_571L, 4, 4, 0, "FVCK"),
                Input(3_838_385_658_376_483L, 11, 11, 0, "3D2ZQ6TVC93"),
                Input(0x1FL, 1, 1, 0, "Z"),
                Input(0x1FL shl 5, 1, 1, 0, "0"),
                Input(0x1FL shl 5, 2, 2, 0, "Z0"),
                Input(0x1FL shl 10, 1, 1, 0, "0"),
                Input(0x1FL shl 10, 2, 2, 0, "00"),
                Input(0x1FL shl 10, 3, 3, 0, "Z00"),
                Input(0x1FL shl 15, 3, 3, 0, "000"),
                Input(0x1FL shl 15, 4, 4, 0, "Z000"),
                Input(0x1FL shl 55, 13, 13, 0, "0Z00000000000"),
                Input(0x1FL shl 60, 13, 13, 0, "F000000000000"),
                Input(AllBitsSet, 13, 13, 0, "FZZZZZZZZZZZZ"),
                Input(PastTimestamp, 10, 10, 0, PastTimestampPart),
                Input(MaxTimestamp, 10, 10, 0, MaxTimestampPart),
                Input(45_678L, 8, 4, 3, "###1CKE#"),
                Input(45_678L, 8, 4, 4, "####1CKE")
            )

    @ParameterizedTest
    @MethodSource("getInputs")
    fun `write crockford`(input: Input) {
        with(input) {
            val buffer = CharArray(bufferSize) { '#' }
            buffer.write(inputValue, length, offset)
            val result = buffer.concatToString()
            result shouldBeEqualTo expectedResult
        }
    }

    class ParseInput(
        val string: String,
        val expectedResult: Long,
    )

    private val parseInputs
        get() =
            listOf(
                ParseInput("0", 0L),
                ParseInput("O", 0L),
                ParseInput("o", 0L),
                ParseInput("1", 1L),
                ParseInput("i", 1L),
                ParseInput("I", 1L),
                ParseInput("l", 1L),
                ParseInput("L", 1L),
                ParseInput("2", 2L),
                ParseInput("3", 3L),
                ParseInput("4", 4L),
                ParseInput("5", 5L),
                ParseInput("6", 6L),
                ParseInput("7", 7L),
                ParseInput("8", 8L),
                ParseInput("9", 9L),
                ParseInput("A", 10L),
                ParseInput("a", 10L),
                ParseInput("B", 11L),
                ParseInput("b", 11L),
                ParseInput("C", 12L),
                ParseInput("c", 12L),
                ParseInput("D", 13L),
                ParseInput("d", 13L),
                ParseInput("E", 14L),
                ParseInput("e", 14L),
                ParseInput("F", 15L),
                ParseInput("f", 15L),
                ParseInput("G", 16L),
                ParseInput("g", 16L),
                ParseInput("H", 17L),
                ParseInput("h", 17L),
                ParseInput("J", 18L),
                ParseInput("j", 18L),
                ParseInput("K", 19L),
                ParseInput("k", 19L),
                ParseInput("M", 20L),
                ParseInput("m", 20L),
                ParseInput("N", 21L),
                ParseInput("n", 21L),
                ParseInput("P", 22L),
                ParseInput("p", 22L),
                ParseInput("Q", 23L),
                ParseInput("q", 23L),
                ParseInput("R", 24L),
                ParseInput("r", 24L),
                ParseInput("S", 25L),
                ParseInput("s", 25L),
                ParseInput("T", 26L),
                ParseInput("t", 26L),
                ParseInput("V", 27L),
                ParseInput("v", 27L),
                ParseInput("W", 28L),
                ParseInput("w", 28L),
                ParseInput("X", 29L),
                ParseInput("x", 29L),
                ParseInput("Y", 30L),
                ParseInput("y", 30L),
                ParseInput("Z", 31L),
                ParseInput("z", 31L),
                ParseInput("EDNA3444", 0x73_6AA1_9084L),
                ParseInput("ZZZZZZZZZZZZ", 0xFFF_FFFF_FFFF_FFFFL),
                ParseInput(PastTimestampPart, PastTimestamp),
                ParseInput("", 0L)
            )

    @ParameterizedTest
    @MethodSource("getParseInputs")
    fun `parse crockford`(input: ParseInput) {
        val result = input.string.parseCrockford()
        result shouldBeEqualTo input.expectedResult
    }

    @Test
    fun `parse crockford with illegal arguments`() {
        assertFailsWith<IllegalArgumentException> {
            "0000000000000".parseCrockford()
        }

        assertFailsWith<IllegalArgumentException> {
            "{".parseCrockford()
        }
    }
}
