package io.bluetape4k.netty.buffer

import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [BitBuf] 및 [BitBufImpl]의 기능을 검증하는 테스트 클래스입니다.
 */
class BitBufTest : AbstractNettyTest() {
    private fun newBitBuf(bytes: Int = 16): BitBuf = ByteBufAllocator.DEFAULT.buffer(bytes).toBitBuf()

    @Test
    fun `capacity는 바이트 크기의 8배를 반환한다`() {
        newBitBuf(4).use { bitBuf ->
            bitBuf.capacity shouldBeEqualTo 32L
        }
    }

    @Test
    fun `maxCapacity는 바이트 maxCapacity의 8배를 반환한다`() {
        newBitBuf(4).use { bitBuf ->
            bitBuf.maxCapacity shouldBeEqualTo bitBuf.byteBuf.maxCapacity().toLong() * 8L
        }
    }

    @Test
    fun `writeBoolean과 readBoolean이 대칭적으로 동작한다`() {
        newBitBuf(8).use { bitBuf ->
            bitBuf.writeBoolean(true)
            bitBuf.writeBoolean(false)
            bitBuf.writeBoolean(true)

            bitBuf.readBoolean().shouldBeTrue()
            bitBuf.readBoolean().shouldBeFalse()
            bitBuf.readBoolean().shouldBeTrue()
        }
    }

    @Test
    fun `writeBits와 readUnsignedBits가 대칭적으로 동작한다`() {
        newBitBuf(8).use { bitBuf ->
            bitBuf.writeBits(0b101, 3)
            bitBuf.writeBits(0b1100, 4)

            bitBuf.readUnsignedBits(3) shouldBeEqualTo 0b101u
            bitBuf.readUnsignedBits(4) shouldBeEqualTo 0b1100u
        }
    }

    @Test
    fun `setBoolean과 getBoolean이 대칭적으로 동작한다`() {
        newBitBuf(4).use { bitBuf ->
            bitBuf.setBoolean(0L, true)
            bitBuf.setBoolean(1L, false)
            bitBuf.setBoolean(2L, true)

            bitBuf.getBoolean(0L).shouldBeTrue()
            bitBuf.getBoolean(1L).shouldBeFalse()
            bitBuf.getBoolean(2L).shouldBeTrue()
        }
    }

    @Test
    fun `setBits와 getUnsignedBits가 대칭적으로 동작한다`() {
        newBitBuf(8).use { bitBuf ->
            bitBuf.setBits(0L, 5, 0b11010)
            bitBuf.getUnsignedBits(0L, 5) shouldBeEqualTo 0b11010u
        }
    }

    @Test
    fun `readableBits는 writerIndex - readerIndex를 반환한다`() {
        newBitBuf(8).use { bitBuf ->
            bitBuf.writeBits(0b1111, 4)
            bitBuf.readableBits() shouldBeEqualTo 4L
            bitBuf.readUnsignedBits(2)
            bitBuf.readableBits() shouldBeEqualTo 2L
        }
    }

    @Test
    fun `writableBits는 capacity - writerIndex를 반환한다`() {
        newBitBuf(1).use { bitBuf ->
            val initial = bitBuf.writableBits()
            bitBuf.writeBits(0b1, 3)
            bitBuf.writableBits() shouldBeEqualTo initial - 3L
        }
    }

    @Test
    fun `읽을 수 있는 비트보다 많이 읽으면 IndexOutOfBoundsException이 발생한다`() {
        newBitBuf(1).use { bitBuf ->
            bitBuf.writeBits(0b1, 1)
            assertThrows<IndexOutOfBoundsException> {
                bitBuf.readUnsignedBits(2)
            }
        }
    }

    @Test
    fun `쓸 수 있는 비트보다 많이 쓰면 IndexOutOfBoundsException이 발생한다`() {
        newBitBuf(1).use { bitBuf ->
            // 1바이트 = 8비트 모두 씀
            bitBuf.writeBits(0xFF, 8)
            assertThrows<IndexOutOfBoundsException> {
                bitBuf.writeBits(1, 1)
            }
        }
    }

    @Test
    fun `amount가 범위를 벗어나면 IllegalArgumentException이 발생한다`() {
        newBitBuf(8).use { bitBuf ->
            assertThrows<IllegalArgumentException> {
                bitBuf.getUnsignedBits(0L, 0)
            }
            assertThrows<IllegalArgumentException> {
                bitBuf.getUnsignedBits(0L, Int.SIZE_BITS + 1)
            }
        }
    }

    @Test
    fun `refCnt와 release가 정상 동작한다`() {
        val bitBuf = newBitBuf(4)
        bitBuf.refCnt() shouldBeEqualTo 1
        bitBuf.release().shouldBeTrue()
    }

    @Test
    fun `retain이 참조 카운트를 증가시킨다`() {
        val bitBuf = newBitBuf(4)
        bitBuf.retain()
        bitBuf.refCnt() shouldBeEqualTo 2
        bitBuf.release()
        bitBuf.release()
    }
}
