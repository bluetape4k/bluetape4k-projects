package io.bluetape4k.netty.buffer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.random.nextUBytes

class ByteBufByteTest: AbstractNettyTest() {

    companion object: KLogging() {
        private const val DATA_SIZE = 1024
    }

    @Nested
    inner class NegOperations {
        @Test
        fun `get and set byte neg`() {
            doByteGSTest(ByteBuf::setByteNeg, ByteBuf::getByteNeg)
        }

        @Test
        fun `read and write byte neg`() {
            doByteRWTest(ByteBuf::writeByteNeg, ByteBuf::readByteNeg)
        }

        @Test
        fun `get and set unsigned byte neg`() {
            doUByteGSTest(ByteBuf::setByteNeg, ByteBuf::getUByteNeg)
        }

        @Test
        fun `read and write unsigned byte neg`() {
            doUByteRWTest(ByteBuf::writeByteNeg, ByteBuf::readUByteNeg)
        }
    }

    @Nested
    inner class AddOperations {
        @Test
        fun `get and set byte add`() {
            doByteGSTest(ByteBuf::setByteAdd, ByteBuf::getByteAdd)
        }

        @Test
        fun `read and write byte add`() {
            doByteRWTest(ByteBuf::writeByteAdd, ByteBuf::readByteAdd)
        }

        @Test
        fun `get and set unsigned byte add`() {
            doUByteGSTest(ByteBuf::setByteAdd, ByteBuf::getUByteAdd)
        }

        @Test
        fun `read and write unsigned byte add`() {
            doUByteRWTest(ByteBuf::writeByteAdd, ByteBuf::readUByteAdd)
        }
    }

    @Nested
    inner class SubOperations {
        @Test
        fun `get and set byte sub`() {
            doByteGSTest(ByteBuf::setByteSub, ByteBuf::getByteSub)
        }

        @Test
        fun `read and write byte add`() {
            doByteRWTest(ByteBuf::writeByteSub, ByteBuf::readByteSub)
        }

        @Test
        fun `get and set unsigned byte sub`() {
            doUByteGSTest(ByteBuf::setByteSub, ByteBuf::getUByteSub)
        }

        @Test
        fun `read and write unsigned byte sub`() {
            doUByteRWTest(ByteBuf::writeByteSub, ByteBuf::readUByteSub)
        }
    }

    private fun doByteGSTest(setter: ByteBuf.(Int, Int) -> ByteBuf, getter: ByteBuf.(Int) -> Byte) {
        val testData = Random.nextBytes(DATA_SIZE)
        ByteBufAllocator.DEFAULT.buffer(testData.size * Byte.SIZE_BYTES).use { buf ->
            testData.forEachIndexed { i, expected -> buf.setter(i, expected.toInt()) }
            testData.forEachIndexed { i, expected ->
                val get = buf.getter(i)
                get shouldBeEqualTo expected
            }
        }
    }

    private fun doByteRWTest(writer: ByteBuf.(Int) -> ByteBuf, reader: ByteBuf.() -> Byte) {
        val testData = Random.nextBytes(DATA_SIZE)
        ByteBufAllocator.DEFAULT.buffer(testData.size * Byte.SIZE_BYTES).use { buf ->
            testData.forEach { expected -> buf.writer(expected.toInt()) }
            testData.forEach { expected ->
                val read = buf.reader()
                read shouldBeEqualTo expected
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    private fun doUByteGSTest(setter: ByteBuf.(Int, Int) -> ByteBuf, getter: ByteBuf.(Int) -> UByte) {
        val testData = Random.nextUBytes(DATA_SIZE)
        ByteBufAllocator.DEFAULT.buffer(testData.size * UByte.SIZE_BYTES).use { buf ->
            testData.forEachIndexed { i, expected -> buf.setter(i, expected.toInt()) }
            testData.forEachIndexed { i, expected ->
                val get = buf.getter(i)
                get shouldBeEqualTo expected
                get.toInt() shouldBeGreaterOrEqualTo 0
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    private fun doUByteRWTest(writer: ByteBuf.(Int) -> ByteBuf, reader: ByteBuf.() -> UByte) {
        val testData = Random.nextUBytes(DATA_SIZE)
        ByteBufAllocator.DEFAULT.buffer(testData.size * UByte.SIZE_BYTES).use { buf ->
            testData.forEach { expected -> buf.writer(expected.toInt()) }
            testData.forEach { expected ->
                val read = buf.reader()
                read shouldBeEqualTo expected
                read.toInt() shouldBeGreaterOrEqualTo 0
            }
        }
    }
}
