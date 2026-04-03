package io.bluetape4k.netty.util

import io.bluetape4k.netty.AbstractNettyTest
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test

/**
 * [ReferenceCountedSupport]의 기능을 검증하는 테스트 클래스입니다.
 */
class ReferenceCountedSupportTest : AbstractNettyTest() {
    @Test
    fun `use 블록 실행 후 ByteBuf가 릴리즈된다`() {
        val buf = ByteBufAllocator.DEFAULT.buffer(16)
        buf.refCnt() shouldBeEqualTo 1
        buf.use {
            it.writeInt(42)
        }
        // release 후 refCnt는 0
        buf.refCnt() shouldBeEqualTo 0
    }

    @Test
    fun `use 블록에서 예외가 발생해도 ByteBuf가 릴리즈된다`() {
        val buf = ByteBufAllocator.DEFAULT.buffer(16)
        runCatching {
            buf.use {
                throw RuntimeException("테스트 예외")
            }
        }
        buf.refCnt() shouldBeEqualTo 0
    }

    @Test
    fun `decrement 파라미터로 참조 카운트를 여러 번 감소시킬 수 있다`() {
        val buf = ByteBufAllocator.DEFAULT.buffer(16)
        buf.retain() // refCnt == 2
        buf.refCnt() shouldBeEqualTo 2
        buf.use(decrement = 2) { }
        buf.refCnt() shouldBeEqualTo 0
    }

    @Test
    fun `use 블록 내에서 retain 후 단계적으로 릴리즈가 가능하다`() {
        val buf = ByteBufAllocator.DEFAULT.buffer(16)
        buf.retain()
        buf.refCnt() shouldBeEqualTo 2
        // 내부 release()는 refCnt 2→1 이므로 false 반환, use가 추가로 release(1) 호출 → 0
        buf.use { it.release().shouldBeFalse() }
        buf.refCnt() shouldBeEqualTo 0
    }
}
