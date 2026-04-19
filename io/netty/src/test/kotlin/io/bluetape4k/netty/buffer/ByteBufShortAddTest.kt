package io.bluetape4k.netty.buffer

import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * [ByteBufExtensions]의 Short add/sub 계열 set/get/read/write 함수들의 정확성을 검증합니다.
 *
 * 특히 `setShortAdd`에서 두 번째 바이트 offset으로 `Byte.SIZE_BYTES` (1) 을 사용해야 하는데
 * 실수로 `Byte.SIZE_BITS` (8) 을 사용하는 버그를 회귀 방지합니다.
 */
class ByteBufShortAddTest: AbstractNettyTest() {

    @Test
    fun `setShortAdd와 getShortAdd는 roundtrip이 일치한다`() {
        // value = 0x0100 (256)
        // 상위 바이트 = value shr 8 = 0x01
        // 하위 바이트 = (value + 128) and 0xFF = (256 + 128) and 0xFF = 0x80
        val value = 0x0100
        ByteBufAllocator.DEFAULT.buffer(Short.SIZE_BYTES).use { buf ->
            buf.writeZero(Short.SIZE_BYTES)
            buf.setShortAdd(0, value)
            val result = buf.getShortAdd(0)
            result shouldBeEqualTo value.toShort()
        }
    }

    @Test
    fun `setShortAdd은 두 번째 바이트를 index+1 에 기록한다 - 버그 회귀 방지`() {
        // 버그: `index + Byte.SIZE_BITS` (=8) 사용 → 범위 초과
        // 수정: `index + Byte.SIZE_BYTES` (=1) 사용
        val value = 0x0010  // 상위=0, 하위=(16+128)=144
        ByteBufAllocator.DEFAULT.buffer(Short.SIZE_BYTES).use { buf ->
            buf.writeZero(Short.SIZE_BYTES)
            buf.setShortAdd(0, value)
            // 상위 바이트 at index 0: value shr 8 = 0
            buf.getByte(0) shouldBeEqualTo 0.toByte()
            // 하위 바이트 at index 1: (value + 128) and 0xFF = 144
            buf.getByte(1) shouldBeEqualTo (16 + 128).toByte()
        }
    }

    @Test
    fun `writeShortAdd와 readShortAdd는 roundtrip이 일치한다`() {
        val testValues = listOf(0, 1, -1, 127, -128, 0x0100, -0x0100)
        ByteBufAllocator.DEFAULT.buffer(testValues.size * Short.SIZE_BYTES).use { buf ->
            testValues.forEach { v -> buf.writeShortAdd(v) }
            testValues.forEach { v ->
                val read = buf.readShortAdd()
                read shouldBeEqualTo v.toShort()
            }
        }
    }

    @Test
    fun `writeShortLEAdd와 readShortLEAdd는 roundtrip이 일치한다`() {
        val testValues = listOf(0, 1, -1, 100, -100)
        ByteBufAllocator.DEFAULT.buffer(testValues.size * Short.SIZE_BYTES).use { buf ->
            testValues.forEach { v -> buf.writeShortLEAdd(v) }
            testValues.forEach { v ->
                val read = buf.readShortLEAdd()
                read shouldBeEqualTo v.toShort()
            }
        }
    }

    @Test
    fun `setShortAdd와 getShortAdd는 다수 값에 대해 roundtrip이 일치한다`() {
        val count = 100
        val values = IntArray(count) { Random.nextInt(-32768, 32767) }
        ByteBufAllocator.DEFAULT.buffer(count * Short.SIZE_BYTES).use { buf ->
            buf.writeZero(count * Short.SIZE_BYTES)
            values.forEachIndexed { i, v ->
                buf.setShortAdd(i * Short.SIZE_BYTES, v)
            }
            values.forEachIndexed { i, v ->
                val got = buf.getShortAdd(i * Short.SIZE_BYTES)
                got shouldBeEqualTo v.toShort()
            }
        }
    }

    @Test
    fun `getShortAdd는 Unpooled 버퍼에서도 올바르게 읽는다`() {
        // 수동으로 값을 설정하고 getShortAdd로 읽는다
        // setShortAdd: high = value shr 8, low = (value + 128) and 0xFF
        // getShortAdd: (high shl 8) or ((low - 128) and 0xFF)
        val value = 0x0055  // 85 decimal
        // high = 0x00 = 0, low = (85 + 128) and 0xFF = 213
        val buf = Unpooled.wrappedBuffer(byteArrayOf(0x00, (85 + 128).toByte()))
        val result = buf.getShortAdd(0)
        buf.release()
        result shouldBeEqualTo value.toShort()
    }
}
