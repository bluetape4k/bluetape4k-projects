package io.bluetape4k.support

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.collections.reverse

class ByteArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Int value to ByteArray vice versa`() {
        val value = Fakers.random.nextInt()
        val bytes = value.toByteArray()
        val converted = bytes.toInt()

        // log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Long value to ByteArray vice versa`() {
        val value = Fakers.random.nextLong()
        val bytes = value.toByteArray()
        val converted = bytes.toLong()

        // log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert UUID value to ByteArray vice versa`() {
        val value = Fakers.randomUuid()
        val bytes = value.toByteArray()
        val converted = bytes.toUuid()

        // log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @Test
    fun `index of byte`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val target = 0x03.toByte()

        bytes.indexOf(target, 0, bytes.size) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, 1, bytes.size + 1)
        }
    }

    @Test
    fun `index of byte array`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val target = byteArrayOf(0x03, 0x04)

        bytes.indexOf(target, 0, bytes.size) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, 1, bytes.size + 1)
        }
    }

    @Test
    fun `ensure capacity`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        bytes.ensureCapacity(bytes.size, 5) shouldBeEqualTo byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes.ensureCapacity(10, 0) shouldBeEqualTo byteArrayOf(
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )

        assertFailsWith<IllegalArgumentException> {
            bytes.ensureCapacity(-1, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.ensureCapacity(0, -1)
        }
    }

    @Test
    fun `concat byte arrays`() {
        val bytes1 = byteArrayOf(0x01, 0x02, 0x03)
        val bytes2 = byteArrayOf(0x04, 0x05)

        concat(bytes1, bytes2) shouldBeEqualTo byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        concat(bytes2, bytes1) shouldBeEqualTo byteArrayOf(0x04, 0x05, 0x01, 0x02, 0x03)
    }

    @Test
    fun `reverse byte array`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

        bytes.reverseTo() shouldBeEqualTo byteArrayOf(0x05, 0x04, 0x03, 0x02, 0x01)
        bytes.reverseTo(1, 4) shouldBeEqualTo byteArrayOf(0x01, 0x05, 0x04, 0x03, 0x02)
    }

    @Test
    fun `reverse current byte array`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes.reverse()
        bytes shouldBeEqualTo byteArrayOf(0x05, 0x04, 0x03, 0x02, 0x01)

        val bytes2 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes2.reverse(1, 4)
        bytes2 shouldBeEqualTo byteArrayOf(0x01, 0x04, 0x03, 0x02, 0x05)
    }

    @Test
    fun `rotate byte array elements`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes.rotateTo(2) shouldBeEqualTo byteArrayOf(0x04, 0x05, 0x01, 0x02, 0x03)
        bytes.rotateTo(-2) shouldBeEqualTo byteArrayOf(0x03, 0x04, 0x05, 0x01, 0x02)
    }

    @Test
    fun `rotate itself byte array elements`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes.rotate(2)
        bytes shouldBeEqualTo byteArrayOf(0x04, 0x05, 0x01, 0x02, 0x03)

        val bytes2 = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        bytes2.rotate(-2)
        bytes2 shouldBeEqualTo byteArrayOf(0x03, 0x04, 0x05, 0x01, 0x02)
    }
}
