package io.bluetape4k.netty.buffer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.util.AsciiString
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [ByteBufUtilSupport]의 추가 기능을 검증하는 테스트 클래스입니다.
 * 특히 `setShortBE`, `writeMediumBE`, `writeShortBE`, `writeAscii`, `reserveAndWriteUtf8`,
 * `utf8Bytes`, `appendPrettyHexDumpTo`, `copyTo`, `decodeHexByte`, `swapMedium`, `isText` 등을 검증합니다.
 */
class ByteBufUtilAdditionalTest: AbstractNettyTest() {

    companion object: KLogging()

    @Test
    fun `writeShortBE는 big-endian 순서로 short를 기록한다`() {
        ByteBufAllocator.DEFAULT.buffer(Short.SIZE_BYTES).use { buf ->
            buf.writeShortBE(0x0102)
            buf.getByte(0) shouldBeEqualTo 0x01.toByte()
            buf.getByte(1) shouldBeEqualTo 0x02.toByte()
        }
    }

    @Test
    fun `setShortBE는 지정 위치에 big-endian 순서로 short를 기록한다`() {
        ByteBufAllocator.DEFAULT.buffer(4).use { buf ->
            buf.writeZero(4)
            buf.setShortBE(0, 0x0304)
            buf.getByte(0) shouldBeEqualTo 0x03.toByte()
            buf.getByte(1) shouldBeEqualTo 0x04.toByte()
        }
    }

    @Test
    fun `writeMediumBE는 big-endian 순서로 medium을 기록한다`() {
        ByteBufAllocator.DEFAULT.buffer(Medium.SIZE_BYTES).use { buf ->
            buf.writeMediumBE(0x010203)
            buf.getByte(0) shouldBeEqualTo 0x01.toByte()
            buf.getByte(1) shouldBeEqualTo 0x02.toByte()
            buf.getByte(2) shouldBeEqualTo 0x03.toByte()
        }
    }

    @Test
    fun `writeUtf8는 지정 범위의 UTF-8 문자열을 기록한다`() {
        val text = "Hello, World!"
        Unpooled.buffer(text.length * 4).use { buf ->
            val written = buf.writeUtf8(text, 0, text.length)
            written shouldBeEqualTo text.length
            buf.toString(Charsets.UTF_8) shouldBeEqualTo text
        }
    }

    @Test
    fun `reserveAndWriteUtf8는 예약된 공간에 UTF-8 문자열을 기록한다`() {
        val text = "Hello!"
        Unpooled.buffer(64).use { buf ->
            val written = buf.reserveAndWriteUtf8(text, reserveBytes = 32)
            written shouldBeEqualTo text.length
            buf.toString(Charsets.UTF_8) shouldBeEqualTo text
        }
    }

    @Test
    fun `utf8Bytes는 문자열의 UTF-8 바이트 수를 반환한다`() {
        val text = "Hello"
        text.utf8Bytes() shouldBeEqualTo 5

        val korean = "안녕"
        korean.utf8Bytes() shouldBeEqualTo 6  // 3 bytes per Korean char in UTF-8
    }

    @Test
    fun `writeAscii는 ASCII 문자열을 ByteBuf에 기록한다`() {
        val text = "Hello"
        Unpooled.buffer(text.length).use { buf ->
            val written = buf.writeAscii(text)
            written shouldBeEqualTo text.length
            buf.toString(Charsets.US_ASCII) shouldBeEqualTo text
        }
    }

    @Test
    fun `allocator writeAscii는 ASCII 문자열을 담은 새 ByteBuf를 반환한다`() {
        val text = "Hello ASCII"
        val buf = ByteBufAllocator.DEFAULT.writeAscii(text)
        val decoded = buf.toString(Charsets.US_ASCII)
        buf.release()
        decoded shouldBeEqualTo text
    }

    @Test
    fun `appendPrettyHexDumpTo는 StringBuilder에 hex dump를 추가한다`() {
        Unpooled.wrappedBuffer(byteArrayOf(0x41, 0x42, 0x43)).use { buf ->
            val sb = StringBuilder()
            buf.appendPrettyHexDumpTo(sb)
            val dump = sb.toString()
            dump.shouldNotBeNull()
            dump.shouldNotBeEmpty()
        }
    }

    @Test
    fun `isText는 지정된 charset으로 텍스트 여부를 확인한다`() {
        val text = "Hello"
        Unpooled.copiedBuffer(text, Charsets.UTF_8).use { buf ->
            buf.isText(charset = Charsets.UTF_8) shouldBeEqualTo true
            buf.isText(charset = Charsets.US_ASCII) shouldBeEqualTo true
        }
    }

    @Test
    fun `swapMedium은 3바이트 값의 바이트 순서를 뒤집는다`() {
        val value = 0x010203
        val swapped = value.swapMedium()
        swapped shouldBeEqualTo 0x030201
    }

    @Test
    fun `decodeHexByte는 hex 문자열의 지정 위치에서 바이트를 디코딩한다`() {
        val hex = "abcd"
        hex.decodeHexByte(0) shouldBeEqualTo 0xAB.toByte()
        hex.decodeHexByte(2) shouldBeEqualTo 0xCD.toByte()
    }

    @Test
    fun `AsciiString copyTo는 지정 위치에 내용을 복사한다`() {
        val ascii = AsciiString("hello")
        val asciiLength = ascii.length
        ByteBufAllocator.DEFAULT.buffer(asciiLength).use { dst ->
            dst.writeZero(asciiLength)
            // dstIndex=0 으로 명시적으로 지정해야 함
            ascii.copyTo(dstIndex = 0, dst = dst, length = asciiLength)
            dst.readerIndex(0)
            dst.toString(0, asciiLength, Charsets.US_ASCII) shouldBeEqualTo "hello"
        }
    }

    @Test
    fun `ByteBuf equals는 지정 범위가 동일한지 확인한다`() {
        val a = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val b = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val result = a.equals(0, b, 0, 3)
        a.release()
        b.release()
        result shouldBeEqualTo true
    }
}
