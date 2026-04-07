package io.bluetape4k.netty.buffer

import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.nio.CharBuffer

/**
 * [ByteBufUtilSupport]의 기능을 검증하는 테스트 클래스입니다.
 */
class ByteBufUtilSupportTest: AbstractNettyTest() {
    @Test
    fun `hexDump은 ByteBuf를 16진수 문자열로 변환한다`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        Unpooled.wrappedBuffer(bytes).use { buf ->
            val hex = buf.hexDump()
            hex shouldBeEqualTo "01020304"
        }
    }

    @Test
    fun `ByteArray hexDump은 바이트 배열을 16진수 문자열로 변환한다`() {
        val bytes = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
        bytes.hexDump() shouldBeEqualTo "abcd"
    }

    @Test
    fun `decodeHexDump은 16진수 문자열을 바이트 배열로 변환한다`() {
        val hex = "01020304"
        hex.decodeHexDump() shouldBeEqualTo byteArrayOf(0x01, 0x02, 0x03, 0x04)
    }

    @Test
    fun `indexOf는 needle ByteBuf의 위치를 반환한다`() {
        // ByteBufUtil.indexOf(needle, haystack) — receiver가 needle, 인자가 haystack
        val needle = Unpooled.wrappedBuffer(byteArrayOf(0x03, 0x04))
        val haystack = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
        val idx = needle.indexOf(haystack)
        needle.release()
        haystack.release()
        idx shouldBeEqualTo 2
    }

    @Test
    fun `equalsEx는 내용이 동일한 두 ByteBuf를 같다고 판단한다`() {
        val a = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val b = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02, 0x03))
        val result = a.equalsEx(b)
        a.release()
        b.release()
        result.shouldBeTrue()
    }

    @Test
    fun `equalsEx는 내용이 다른 두 ByteBuf를 다르다고 판단한다`() {
        val a = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02))
        val b = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x03))
        val result = a.equalsEx(b)
        a.release()
        b.release()
        result.shouldBeFalse()
    }

    @Test
    fun `compare는 두 ByteBuf를 사전 순으로 비교한다`() {
        val a = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x02))
        val b = Unpooled.wrappedBuffer(byteArrayOf(0x01, 0x03))
        val result = a.compare(b)
        a.release()
        b.release()
        (result < 0).shouldBeTrue()
    }

    @Test
    fun `writeUtf8와 allocator readBytes로 데이터를 읽어온다`() {
        val text = "안녕하세요"
        val buf = ByteBufAllocator.DEFAULT.writeUtf8(text)
        val copy = ByteBufAllocator.DEFAULT.readBytes(buf, buf.readableBytes())
        val decoded = copy.toString(Charsets.UTF_8)
        buf.release()
        copy.release()
        decoded shouldBeEqualTo text
    }

    @Test
    fun `encodeString은 CharBuffer를 ByteBuf로 인코딩한다`() {
        val text = "Hello, Netty!"
        val charBuffer = CharBuffer.wrap(text)
        val buf = ByteBufAllocator.DEFAULT.encodeString(charBuffer, Charsets.UTF_8)
        val decoded = buf.toString(Charsets.UTF_8)
        buf.release()
        decoded shouldBeEqualTo text
    }

    @Test
    fun `prettyHexDump은 가독성 있는 hex dump 문자열을 반환한다`() {
        Unpooled.wrappedBuffer(byteArrayOf(0x41, 0x42, 0x43)).use { buf ->
            val dump = buf.prettyHexDump()
            dump.shouldNotBeNull()
            dump.shouldNotBeEmpty()
        }
    }

    @Test
    fun `isUtf8는 UTF-8 인코딩된 ByteBuf에 대해 true를 반환한다`() {
        val text = "Hello"
        val buf = ByteBufAllocator.DEFAULT.writeUtf8(text)
        val result = buf.isUtf8()
        buf.release()
        result.shouldBeTrue()
    }

    @Test
    fun `isAscii는 ASCII 문자만 포함된 ByteBuf에 대해 true를 반환한다`() {
        val buf = Unpooled.wrappedBuffer("ASCII".toByteArray(Charsets.US_ASCII))
        val result = buf.isAscii()
        buf.release()
        result.shouldBeTrue()
    }

    @Test
    fun `Short swap은 바이트 순서를 뒤집는다`() {
        val value: Short = 0x0102
        value.swap() shouldBeEqualTo 0x0201.toShort()
    }

    @Test
    fun `Int swap은 바이트 순서를 뒤집는다`() {
        val value = 0x01020304
        value.swap() shouldBeEqualTo 0x04030201
    }

    @Test
    fun `Long swap은 바이트 순서를 뒤집는다`() {
        val value = 0x0102030405060708L
        value.swap() shouldBeEqualTo 0x0807060504030201L
    }

    @Test
    fun `ensureAccessible은 접근 가능한 ByteBuf를 그대로 반환한다`() {
        Unpooled.wrappedBuffer(byteArrayOf(0x01)).use { buf ->
            val result = buf.ensureAccessible()
            result shouldBeEqualTo buf
        }
    }
}
