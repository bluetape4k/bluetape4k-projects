package io.bluetape4k.support

import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

class ByteArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        fun sampleByteArray(size: Int = 5): ByteArray {
            return ByteArray(size) { (it + 1).toByte() }
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Int value to ByteArray vice versa`() {
        val value = Fakers.random.nextInt()
        val bytes = value.toByteArray()
        val converted = bytes.toInt()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert Long value to ByteArray vice versa`() {
        val value = Fakers.random.nextLong()
        val bytes = value.toByteArray()
        val converted = bytes.toLong()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert UUID value to ByteArray vice versa`() {
        val value = UUID.randomUUID()
        val bytes = value.toByteArray()
        val converted = bytes.toUuid()

        log.debug { "value=$value, bytes=${bytes.encodeHexString()}, converted=$converted" }

        converted shouldBeEqualTo value
    }

    @Test
    fun `take and drop items from byte array`() {
        val bytes = sampleByteArray()

        bytes.take(3).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3)
        bytes.take(0).toByteArray() shouldBeEqualTo emptyByteArray
        bytes.take(5).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)
        bytes.take(10).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)

        bytes.drop(3).toByteArray() shouldBeEqualTo byteArrayOf(4, 5)
        bytes.drop(0).toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)
        bytes.drop(5).toByteArray() shouldBeEqualTo emptyByteArray
        bytes.drop(10).toByteArray() shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `index of byte`() {
        val bytes = sampleByteArray()
        val target = 0x03.toByte()

        bytes.indexOf(target, 0, bytes.size - 1) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, 1, bytes.size)
        }
    }

    @Test
    fun `index of byte array`() {
        val bytes = sampleByteArray()
        val target = byteArrayOf(3, 4)

        bytes.indexOf(target, 0, bytes.size - 1) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.indexOf(target, 1, bytes.size + 1)
        }
    }

    @Test
    fun `lastIndex of byte`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 3)
        val target = 3.toByte()

        bytes.lastIndexOf(target, 0, bytes.size - 1) shouldBeEqualTo 4

        assertFailsWith<IllegalArgumentException> {
            bytes.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.lastIndexOf(target, 1, bytes.size + 1)
        }
    }

    @Test
    fun `lastIndex of byte array`() {
        val bytes = byteArrayOf(1, 2, 3, 4, 3, 4, 2)
        val target = byteArrayOf(3, 4)

        bytes.lastIndexOf(target, 0, bytes.size - 1) shouldBeEqualTo 4

        assertFailsWith<IllegalArgumentException> {
            bytes.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            bytes.lastIndexOf(target, 1, bytes.size + 1)
        }
    }

    @Test
    fun `ensure capacity`() {
        val bytes = sampleByteArray()

        bytes.ensureCapacity(bytes.size, 5) shouldBeEqualTo sampleByteArray()
        bytes.ensureCapacity(10, 0) shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5, 0, 0, 0, 0, 0)

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

        concat(bytes1, bytes2) shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)
        concat(bytes2, bytes1) shouldBeEqualTo byteArrayOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `reverse empty byte array`() {
        val bytes = emptyByteArray

        bytes.reverseTo() shouldBeEqualTo emptyByteArray
        bytes.reverseTo(0, 0) shouldBeEqualTo emptyByteArray

        bytes.reverseThis()
        bytes shouldBeEqualTo emptyByteArray

        bytes.reverseThis(0, 0)
        bytes shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `reverseTo byte array`() {
        val bytes = sampleByteArray()

        bytes.reverseTo().reverseTo() shouldBeEqualTo bytes
        bytes.reverseTo() shouldBeEqualTo byteArrayOf(5, 4, 3, 2, 1)
        bytes.reverseTo(1, 3) shouldBeEqualTo byteArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `reverse current byte array`() {
        val bytes = sampleByteArray()
        bytes.reverseThis()
        bytes shouldBeEqualTo byteArrayOf(5, 4, 3, 2, 1)

        bytes.reverseThis()
        bytes shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5)

        val bytes2 = sampleByteArray()
        bytes2.reverseThis(1, 3)
        bytes2 shouldBeEqualTo byteArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `rotate empty byte array elements`() {
        val bytes = emptyByteArray
        bytes.rotateTo(2) shouldBeEqualTo emptyByteArray
        bytes.rotateTo(-2) shouldBeEqualTo emptyByteArray

        val bytes2 = emptyByteArray
        bytes2.rotateThis(2)
        bytes2 shouldBeEqualTo emptyByteArray

        val bytes3 = emptyByteArray
        bytes3.rotateThis(-2)
        bytes3 shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `rotate byte array elements`() {
        val bytes = sampleByteArray()

        bytes.rotateTo(2) shouldBeEqualTo byteArrayOf(0x04, 0x05, 0x01, 0x02, 0x03)
        bytes.rotateTo(-2) shouldBeEqualTo byteArrayOf(0x03, 0x04, 0x05, 0x01, 0x02)
    }

    @Test
    fun `rotate itself byte array elements`() {
        val bytes = sampleByteArray()
        bytes.rotateThis(2)
        bytes shouldBeEqualTo byteArrayOf(0x04, 0x05, 0x01, 0x02, 0x03)

        val bytes2 = sampleByteArray()
        bytes2.rotateThis(-2)
        bytes2 shouldBeEqualTo byteArrayOf(0x03, 0x04, 0x05, 0x01, 0x02)
    }
}
