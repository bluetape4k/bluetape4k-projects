package io.bluetape4k.idgenerators.hashids

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotEndWith
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.absoluteValue

@RandomizedTest
class HashidsTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    private val hashids = Hashids(salt = "great korea")

    @ParameterizedTest(name = "encode number: {0}")
    @ValueSource(longs = [0, 1, 1014, Hashids.MAX_NUMBER])
    fun `encode number in valid range`(number: Long) {
        val encoded = hashids.encode(number)
        encoded shouldNotEndWith Hashids.LARGE_NUMBER_SUFFIX

        val decoded = hashids.decode(encoded)

        log.debug { "encoded=$encoded, decoded=${decoded.joinToString()}" }
        decoded.first() shouldBeEqualTo number
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode random valid number array`(@RandomValue(type = Long::class, size = 3) randomNumbers: List<Long>) {
        val numbers = randomNumbers.map { it.absoluteValue % Hashids.MAX_NUMBER }.toLongArray()
        log.debug { "numbers=${numbers.joinToString()}" }

        val encoded = hashids.encode(*numbers)
        log.debug { "Encoded=$encoded" }
        val decoded = hashids.decode(encoded)
        log.debug { "Decoded=${decoded.joinToString()}" }

        decoded.size shouldBeEqualTo numbers.size
        decoded shouldContainSame numbers
    }

    @Test
    fun `0 ~ MAX_NUMBER 범위를 벗어난 양의 수의 배열로 인코딩하기`() {
        val numbers = longArrayOf(Hashids.MAX_NUMBER + 3, Hashids.MAX_NUMBER + 1004)

        numbers.forEach { num ->
            val encoded = hashids.encode(num)
            log.debug { "Encode number=$num, encoded=$encoded" }
        }

        val encoded = hashids.encode(*numbers)
        log.debug { "Encoded=$encoded" }

        val decoded = hashids.decode(encoded)
        log.debug { "Decoded=${decoded.joinToString()}" }

        decoded.size shouldBeEqualTo numbers.size
        decoded shouldContainSame numbers
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode random number array`(@RandomValue(type = Long::class, size = 3) randomNumbers: List<Long>) {
        val numbers = randomNumbers.toLongArray()
        log.debug { "numbers=${numbers.joinToString()}" }

        val encoded = hashids.encode(*numbers)
        log.debug { "Encoded=$encoded" }
        val decoded = hashids.decode(encoded)
        log.debug { "Decoded=${decoded.joinToString()}" }

        decoded.size shouldBeEqualTo numbers.size
        decoded shouldContainSame numbers
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode random number`(@RandomValue(type = Long::class) numbers: List<Long>) {
        numbers.forEach { number ->
            val encoded = hashids.encode(number)
            val decoded = hashids.decode(encoded)
            log.debug { "number=$number, encoded=$encoded, decoded=${decoded.joinToString()}" }

            decoded.size shouldBeEqualTo 1
            decoded[0] shouldBeEqualTo number
        }
    }

    @Test
    fun `decode 실패 시 empty array를 반환`() {
        val pepperedHashids = Hashids("this is my pepper")
        val decoded = pepperedHashids.decode("NkK9")
        decoded.isEmpty().shouldBeTrue()
    }

    @Test
    fun `should allow to specify custom alphabet`() {
        val hashids = Hashids("this is my salt", customAlphabet = "01223456789abcdef")
        val expectedHashid = "b332db5"
        val numberToHash = 1234567L

        val encoded = hashids.encode(numberToHash)
        encoded shouldBeEqualTo expectedHashid

        val decoded = hashids.decode(encoded)
        decoded[0] shouldBeEqualTo numberToHash
    }

    @Test
    fun `should allow to specify custom hash length`() {
        val expectedHashid = "gB0NV05e"
        val numberToHash = 1L
        val hashids = Hashids("this is my salt", 8)

        val encoded = hashids.encode(numberToHash)
        encoded shouldBeEqualTo expectedHashid

        val decoded = hashids.decode(encoded)
        decoded[0] shouldBeEqualTo 1L
    }

    @Test
    fun `should encode and decode the same numbers`() {
        val expected = "EWh0hghy"
        val numberToHash = longArrayOf(5L, 5L, 5L, 5L)

        val encoded = hashids.encode(*numberToHash)
        encoded shouldBeEqualTo expected

        val decoded = hashids.decode(expected)
        decoded shouldContainSame numberToHash
    }

    @Test
    fun `should encode and decode array of incrementing numbers`() {
        val expected = "pGHrfQT5CrhZInuecOUj"
        val numberToHash = List(10) { it + 1L }.toLongArray()

        val encoded = hashids.encode(*numberToHash)
        encoded shouldBeEqualTo expected

        val decoded = hashids.decode(encoded)
        decoded shouldContainSame numberToHash
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `should encode and decode random numbers`(@RandomValue(type = Long::class) numbers: List<Long>) {

        val actual = numbers.map { minOf(it.absoluteValue, Hashids.MAX_NUMBER) }
        val encoded = hashids.encode(*actual.toLongArray())

        val decoded = hashids.decode(encoded)
        decoded shouldContainSame actual
    }

    @Test
    fun `should encode and decode incrementing numbers`() {
        hashids.encode(1L) shouldBeEqualTo "Vp"
        hashids.encode(2L) shouldBeEqualTo "n9"
        hashids.encode(3L) shouldBeEqualTo "GJ"
        hashids.encode(4L) shouldBeEqualTo "OR"
        hashids.encode(5L) shouldBeEqualTo "wo"
    }

    @Test
    fun `should encode numbers bigger than max integer value`() {
        hashids.encode(9876543210123L) shouldBeEqualTo "5rdJmxRkk"
        hashids.encode(Int.MAX_VALUE.toLong()) shouldBeEqualTo "G8xee2O"
    }

    @Test
    fun `encode decode with 0`() {
        hashids.encode(0L) shouldBeEqualTo "Y"
        val decoded = hashids.decode("Y")
        decoded[0] shouldBeEqualTo 0

        val numbersToHash = longArrayOf(1L, 2L, 0L, 3L)
        hashids.encode(*numbersToHash) shouldBeEqualTo "aoHafsB"
        hashids.decode("aoHafsB") shouldBeEqualTo numbersToHash
    }

    @Test
    fun `encode negative numbers`() {
        val numbers = longArrayOf(1, 2, -3)

        val encoded = hashids.encode(*numbers)

        hashids.encode(1, 2) shouldBeEqualTo "GjHB"
        hashids.encode(-3) shouldBeEqualTo "GJTs="

        val decoded = hashids.decode(encoded)
        log.debug { "decoded=${decoded.joinToString()}" }
        decoded shouldBeEqualTo numbers
    }

    @Test
    fun `encode single with large number`() {
        val number = Hashids.MAX_NUMBER + 100
        val encoded = hashids.encode(number)
        val decoded = hashids.decode(encoded).first()
        decoded shouldBeEqualTo number
    }

    @Test
    fun `encode single with negative large number`() {
        val number = -(Hashids.MAX_NUMBER + 100)
        val encoded = hashids.encode(number)
        val decoded = hashids.decode(encoded).first()
        decoded shouldBeEqualTo number
    }
}
