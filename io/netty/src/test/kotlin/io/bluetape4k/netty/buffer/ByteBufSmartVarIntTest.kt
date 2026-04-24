package io.bluetape4k.netty.buffer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [ByteBufExtensions]의 Smart/VarInt/UShort 계열 함수들의 정확성을 검증합니다.
 */
class ByteBufSmartVarIntTest: AbstractNettyTest() {

    companion object: KLogging()

    @Nested
    inner class ShortSmart {
        @Test
        fun `readShortSmart와 writeShortSmart는 byte 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(Smart.MIN_BYTE_VALUE, 0, Smart.MAX_BYTE_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Short.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeShortSmart(v) }
                values.forEach { v ->
                    buf.readShortSmart() shouldBeEqualTo v.toShort()
                }
            }
        }

        @Test
        fun `readShortSmart와 writeShortSmart는 short 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(Smart.MIN_SHORT_VALUE, -100, 100, Smart.MAX_SHORT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Short.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeShortSmart(v) }
                values.forEach { v ->
                    buf.readShortSmart() shouldBeEqualTo v.toShort()
                }
            }
        }

        @Test
        fun `writeShortSmart는 범위를 벗어나면 IllegalArgumentException을 던진다`() {
            ByteBufAllocator.DEFAULT.buffer(4).use { buf ->
                assertThrows<IllegalArgumentException> {
                    buf.writeShortSmart(Smart.MAX_SHORT_VALUE + 1)
                }
            }
        }
    }

    @Nested
    inner class UShortSmart {
        @Test
        fun `readUShortSmart와 writeUShortSmart는 byte 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(0, USmart.MAX_BYTE_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Short.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeUShortSmart(v) }
                values.forEach { v ->
                    buf.readUShortSmart() shouldBeEqualTo v.toShort()
                }
            }
        }

        @Test
        fun `readUShortSmart와 writeUShortSmart는 short 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(USmart.MIN_SHORT_VALUE, 200, USmart.MAX_SHORT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Short.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeUShortSmart(v) }
                values.forEach { v ->
                    buf.readUShortSmart() shouldBeEqualTo v.toShort()
                }
            }
        }

        @Test
        fun `writeUShortSmart는 범위를 벗어나면 IllegalArgumentException을 던진다`() {
            ByteBufAllocator.DEFAULT.buffer(4).use { buf ->
                assertThrows<IllegalArgumentException> {
                    buf.writeUShortSmart(USmart.MAX_SHORT_VALUE + 1)
                }
            }
        }
    }

    @Nested
    inner class IncrShortSmart {
        @Test
        fun `readIncrShortSmart와 writeIncrShortSmart는 작은 값에서 roundtrip이 일치한다`() {
            val values = listOf(0, 100, Short.MAX_VALUE.toInt() - 1)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIncrShortSmart(v) }
                values.forEach { v ->
                    buf.readIncrShortSmart() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readIncrShortSmart와 writeIncrShortSmart는 Short_MAX_VALUE 이상의 값에서 roundtrip이 일치한다`() {
            val values = listOf(Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt() * 2)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES * 4).use { buf ->
                values.forEach { v -> buf.writeIncrShortSmart(v) }
                values.forEach { v ->
                    buf.readIncrShortSmart() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class IntSmart {
        @Test
        fun `readIntSmart와 writeIntSmart는 short 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(Smart.MIN_SHORT_VALUE, 0, Smart.MAX_SHORT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntSmart(v) }
                values.forEach { v ->
                    buf.readIntSmart() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readIntSmart와 writeIntSmart는 int 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(Smart.MIN_INT_VALUE, Smart.MAX_INT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntSmart(v) }
                values.forEach { v ->
                    buf.readIntSmart() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `writeIntSmart는 범위를 벗어나면 IllegalArgumentException을 던진다`() {
            ByteBufAllocator.DEFAULT.buffer(8).use { buf ->
                assertThrows<IllegalArgumentException> {
                    buf.writeIntSmart(Smart.MAX_INT_VALUE + 1)
                }
            }
        }
    }

    @Nested
    inner class UIntSmart {
        @Test
        fun `readUIntSmart와 writeUIntSmart는 short 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(0, USmart.MAX_SHORT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeUIntSmart(v) }
                values.forEach { v ->
                    buf.readUIntSmart() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readUIntSmart와 writeUIntSmart는 int 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(USmart.MIN_INT_VALUE, USmart.MAX_INT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeUIntSmart(v) }
                values.forEach { v ->
                    buf.readUIntSmart() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `writeUIntSmart는 범위를 벗어나면 IllegalArgumentException을 던진다`() {
            ByteBufAllocator.DEFAULT.buffer(8).use { buf ->
                assertThrows<IllegalArgumentException> {
                    buf.writeUIntSmart(-1)
                }
            }
        }
    }

    @Nested
    inner class NullableUIntSmart {
        @Test
        fun `readNullableUIntSmart와 writeNullableUIntSmart는 null 값을 처리한다`() {
            ByteBufAllocator.DEFAULT.buffer(Short.SIZE_BYTES).use { buf ->
                buf.writeNullableUIntSmart(null)
                val result = buf.readNullableUIntSmart()
                result.shouldBeNull()
            }
        }

        @Test
        fun `readNullableUIntSmart와 writeNullableUIntSmart는 short 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(0, 1000, USmart.MAX_SHORT_VALUE - 1)
            ByteBufAllocator.DEFAULT.buffer(values.size * Short.SIZE_BYTES + 4).use { buf ->
                values.forEach { v -> buf.writeNullableUIntSmart(v) }
                values.forEach { v ->
                    buf.readNullableUIntSmart().shouldNotBeNull() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readNullableUIntSmart와 writeNullableUIntSmart는 int 범위 값에서 roundtrip이 일치한다`() {
            val values = listOf(USmart.MAX_SHORT_VALUE + 1, USmart.MAX_INT_VALUE)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES + 4).use { buf ->
                values.forEach { v -> buf.writeNullableUIntSmart(v) }
                values.forEach { v ->
                    buf.readNullableUIntSmart().shouldNotBeNull() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class VarInt {
        @Test
        fun `readVarInt와 writeVarInt는 소형 값에서 roundtrip이 일치한다`() {
            val values = listOf(0, 1, 127, 128, 255)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeVarInt(v) }
                values.forEach { v ->
                    buf.readVarInt() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readVarInt와 writeVarInt는 대형 값에서 roundtrip이 일치한다`() {
            val values = listOf(Int.MAX_VALUE, 1_000_000, 268_435_455)
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeVarInt(v) }
                values.forEach { v ->
                    buf.readVarInt() shouldBeEqualTo v
                }
            }
        }
    }
}
