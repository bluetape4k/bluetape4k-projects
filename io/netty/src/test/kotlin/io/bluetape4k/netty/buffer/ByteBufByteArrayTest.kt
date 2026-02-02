package io.bluetape4k.netty.buffer

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ByteBufByteArrayTest: AbstractNettyTest() {

    companion object: KLogging() {
        private const val ITEM_SIZE = 256
        private const val LIST_SIZE = 20
    }

    @Nested
    inner class Reversed {
        @Test
        fun `get and set bytes reversed`() {
            doByteArrayGSTest(ByteBuf::setBytesReversed, ByteBuf::getBytesReversed)
        }

        @Test
        fun `read and write bytes reversed`() {
            doByteArrayRWTest(ByteBuf::writeBytesReversed, ByteBuf::readBytesReversed)
        }

        @Test
        fun `get and set bytes ByteBuf reversed`() {
            doByteArrayByteBufGSTest(ByteBuf::setBytesReversed, ByteBuf::getBytesReversed)
        }

        @Test
        fun `read and write bytes ByteBuf reversed`() {
            doByteArrayByteBufRWTest(ByteBuf::writeBytesReversed, ByteBuf::readBytesReversed)
        }
    }

    @Nested
    inner class Add {
        @Test
        fun `get and set bytes add`() {
            doByteArrayGSTest(ByteBuf::setBytesAdd, ByteBuf::getBytesAdd)
        }

        @Test
        fun `read and write bytes add`() {
            doByteArrayRWTest(ByteBuf::writeBytesAdd, ByteBuf::readBytesAdd)
        }

        @Test
        fun `get and set bytes ByteBuf add`() {
            doByteArrayByteBufGSTest(ByteBuf::setBytesAdd, ByteBuf::getBytesAdd)
        }

        @Test
        fun `read and write bytes ByteBuf add`() {
            doByteArrayByteBufRWTest(ByteBuf::writeBytesAdd, ByteBuf::readBytesAdd)
        }
    }

    @Nested
    inner class ReservedAdd {
        @Test
        fun `get and set bytes reversed add`() {
            doByteArrayGSTest(ByteBuf::setBytesReversedAdd, ByteBuf::getBytesReversedAdd)
        }

        @Test
        fun `read and write bytes reversed add`() {
            doByteArrayRWTest(ByteBuf::writeBytesReversedAdd, ByteBuf::readBytesReversedAdd)
        }

        @Test
        fun `get and set bytes ByteBuf reversed add`() {
            doByteArrayByteBufGSTest(ByteBuf::setBytesReversedAdd, ByteBuf::getBytesReversedAdd)
        }

        @Test
        fun `read and write bytes ByteBuf reversed add`() {
            doByteArrayByteBufRWTest(ByteBuf::writeBytesReversedAdd, ByteBuf::readBytesReversedAdd)
        }
    }

    private val byteArrayList: List<ByteArray> =
        fastList(LIST_SIZE) {
            val itemSize = Random.nextInt(
                ITEM_SIZE,
                ITEM_SIZE * 2
            )
            Random.nextBytes(itemSize)
        }

    private fun doByteArrayGSTest(
        setter: ByteBuf.(Int, ByteArray) -> ByteBuf,
        getter: ByteBuf.(Int, Int) -> ByteArray,
    ) {
        val testData = byteArrayList
        var sizeSum = 0

        ByteBufAllocator.DEFAULT.buffer(testData.sumOf { it.size } * Byte.SIZE_BYTES).use { buf ->
            testData.forEach { expected ->
                buf.setter(sizeSum, expected)
                sizeSum += expected.size
            }
            sizeSum = 0
            testData.forEachIndexed { i, expected ->
                val get = buf.getter(sizeSum, testData[i].size)
                get shouldBeEqualTo expected
                sizeSum += expected.size
            }
        }
    }

    private fun doByteArrayByteBufGSTest(
        setter: ByteBuf.(Int, ByteBuf) -> ByteBuf,
        getter: ByteBuf.(Int, Int) -> ByteArray,
    ) {
        val testData = byteArrayList
        var sizeSum = 0

        ByteBufAllocator.DEFAULT.buffer(testData.sumOf { it.size } * Byte.SIZE_BYTES).use { buf ->
            testData.forEach { expected ->
                val bufExpected = Unpooled.wrappedBuffer(expected)
                buf.setter(sizeSum, bufExpected)
                sizeSum += expected.size
            }
            sizeSum = 0
            testData.forEachIndexed { i, expected ->
                val get = buf.getter(sizeSum, testData[i].size)
                get shouldBeEqualTo expected
                sizeSum += expected.size
            }
        }
    }

    private fun doByteArrayRWTest(writer: ByteBuf.(ByteArray) -> ByteBuf, reader: ByteBuf.(Int) -> ByteArray) {
        val testData = byteArrayList
        ByteBufAllocator.DEFAULT.buffer(testData.sumOf { it.size } * Byte.SIZE_BYTES).use { buf ->
            testData.forEach { expected -> buf.writer(expected) }
            testData.forEachIndexed { i, expected ->
                val read = buf.reader(testData[i].size)
                read shouldBeEqualTo expected
            }
        }
    }

    private fun doByteArrayByteBufRWTest(writer: ByteBuf.(ByteBuf) -> ByteBuf, reader: ByteBuf.(Int) -> ByteArray) {
        val testData = byteArrayList
        ByteBufAllocator.DEFAULT.buffer(testData.sumOf { it.size } * Byte.SIZE_BYTES).use { buf ->
            testData.forEach { expected ->
                val bufExpected = Unpooled.wrappedBuffer(expected)
                buf.writer(bufExpected)
            }
            testData.forEachIndexed { i, expected ->
                val read = buf.reader(testData[i].size)
                read shouldBeEqualTo expected
            }
        }
    }
}
