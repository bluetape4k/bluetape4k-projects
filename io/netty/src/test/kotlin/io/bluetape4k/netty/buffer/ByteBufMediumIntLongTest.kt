package io.bluetape4k.netty.buffer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * [ByteBufExtensions]의 Medium/Int/Long 계열 get/set/read/write 함수들의 정확성을 검증합니다.
 */
class ByteBufMediumIntLongTest: AbstractNettyTest() {

    companion object: KLogging() {
        private const val ITEM_COUNT = 50
    }

    @Nested
    inner class MediumLME {
        @Test
        fun `getMediumLME와 setMediumLME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(Medium.MIN_VALUE, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Medium.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setMediumLME(i * Medium.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getMediumLME(i * Medium.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readMediumLME와 writeMediumLME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(Medium.MIN_VALUE, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeMediumLME(v) }
                values.forEach { v ->
                    buf.readMediumLME() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class MediumRME {
        @Test
        fun `getMediumRME와 setMediumRME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(Medium.MIN_VALUE, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Medium.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setMediumRME(i * Medium.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getMediumRME(i * Medium.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readMediumRME와 writeMediumRME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(Medium.MIN_VALUE, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeMediumRME(v) }
                values.forEach { v ->
                    buf.readMediumRME() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class UMedium {
        @Test
        fun `getUMediumLME와 setMediumLME는 unsigned 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Medium.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setMediumLME(i * Medium.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getUMediumLME(i * Medium.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `getUMediumRME와 setMediumRME는 unsigned 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Medium.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setMediumRME(i * Medium.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getUMediumRME(i * Medium.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readUMediumLME와 writeMediumLME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeMediumLME(v) }
                values.forEach { v ->
                    buf.readUMediumLME() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readUMediumRME와 writeMediumRME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Medium.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Medium.SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeMediumRME(v) }
                values.forEach { v ->
                    buf.readUMediumRME() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class IntME {
        @Test
        fun `getIntME와 setIntME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt() }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Int.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setIntME(i * Int.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getIntME(i * Int.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readIntME와 writeIntME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt() }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntME(v) }
                values.forEach { v ->
                    buf.readIntME() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class IntIME {
        @Test
        fun `getIntIME와 setIntIME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt() }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Int.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setIntIME(i * Int.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getIntIME(i * Int.SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readIntIME와 writeIntIME는 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt() }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntIME(v) }
                values.forEach { v ->
                    buf.readIntIME() shouldBeEqualTo v
                }
            }
        }
    }

    @Nested
    inner class UInt {
        @Test
        fun `getUIntME와 setIntME는 양수 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Int.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Int.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setIntME(i * Int.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getUIntME(i * Int.SIZE_BYTES) shouldBeEqualTo v.toLong()
                }
            }
        }

        @Test
        fun `getUIntIME와 setIntIME는 양수 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Int.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * Int.SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setIntIME(i * Int.SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getUIntIME(i * Int.SIZE_BYTES) shouldBeEqualTo v.toLong()
                }
            }
        }

        @Test
        fun `readUIntME와 writeIntME는 양수 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Int.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntME(v) }
                values.forEach { v ->
                    buf.readUIntME() shouldBeEqualTo v.toLong()
                }
            }
        }

        @Test
        fun `readUIntIME와 writeIntIME는 양수 값에서 roundtrip이 일치한다`() {
            val values = List(ITEM_COUNT) { Random.nextInt(0, Int.MAX_VALUE) }
            ByteBufAllocator.DEFAULT.buffer(values.size * Int.SIZE_BYTES).use { buf ->
                values.forEach { v -> buf.writeIntIME(v) }
                values.forEach { v ->
                    buf.readUIntIME() shouldBeEqualTo v.toLong()
                }
            }
        }
    }

    @Nested
    inner class SmallLong {

        private val SMALL_LONG_SIZE_BYTES: Int = Medium.SIZE_BYTES * 2

        @Test
        fun `getSmallLong와 setSmallLong는 roundtrip이 일치한다`() {
            val maxVal = (1L shl (Medium.SIZE_BITS * 2 - 1)) - 1
            val values = List(ITEM_COUNT) { Random.nextLong(-maxVal, maxVal) }
            ByteBufAllocator.DEFAULT.buffer(values.size * SMALL_LONG_SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * SMALL_LONG_SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setSmallLong(i * SMALL_LONG_SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getSmallLong(i * SMALL_LONG_SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readSmallLong와 writeSmallLong는 roundtrip이 일치한다`() {
            val maxVal = (1L shl (Medium.SIZE_BITS * 2 - 1)) - 1
            val values = List(ITEM_COUNT) { Random.nextLong(-maxVal, maxVal) }
            ByteBufAllocator.DEFAULT.buffer(values.size * SMALL_LONG_SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeSmallLong(v) }
                values.forEach { v ->
                    buf.readSmallLong() shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `getUSmallLong와 setSmallLong는 양수 값에서 roundtrip이 일치한다`() {
            val maxVal = (1L shl (Medium.SIZE_BITS)) - 1
            val values = List(ITEM_COUNT) { Random.nextLong(0, maxVal) }
            ByteBufAllocator.DEFAULT.buffer(values.size * SMALL_LONG_SIZE_BYTES).use { buf ->
                buf.writeZero(values.size * SMALL_LONG_SIZE_BYTES)
                values.forEachIndexed { i, v ->
                    buf.setSmallLong(i * SMALL_LONG_SIZE_BYTES, v)
                }
                values.forEachIndexed { i, v ->
                    buf.getUSmallLong(i * SMALL_LONG_SIZE_BYTES) shouldBeEqualTo v
                }
            }
        }

        @Test
        fun `readUSmallLong와 writeSmallLong는 양수 값에서 roundtrip이 일치한다`() {
            val maxVal = (1L shl (Medium.SIZE_BITS)) - 1
            val values = List(ITEM_COUNT) { Random.nextLong(0, maxVal) }
            ByteBufAllocator.DEFAULT.buffer(values.size * SMALL_LONG_SIZE_BYTES + 1).use { buf ->
                values.forEach { v -> buf.writeSmallLong(v) }
                values.forEach { v ->
                    buf.readUSmallLong() shouldBeEqualTo v
                }
            }
        }
    }
}
