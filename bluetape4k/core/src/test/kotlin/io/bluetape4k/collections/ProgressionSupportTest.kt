package io.bluetape4k.collections

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProgressionSupportTest {

    companion object: KLogging()

    @Test
    fun `create CharProgression`() {
        val chars = charProgressionOf('a', 'z')
        chars.toFastList() shouldHaveSize 26
    }

    @Nested
    inner class IntProgression {
        @Test
        fun `create progression`() {
            val ints = intProgressionOf(1, 10, 1)
            ints.size() shouldBeEqualTo 10
        }

        @Test
        fun `as stream`() {
            val ints = intProgressionOf(1, 10, 1)
            ints.asStream().count() shouldBeEqualTo 10
        }

        @Test
        fun `chunked progression`() {
            val ints = intProgressionOf(1, 10, 1)
            ints.size() shouldBeEqualTo 10
            val chunked = ints.chunked(2).toFastList()
            chunked.size shouldBeEqualTo 5
            chunked.forEach {
                log.debug { "group=$it" }
            }
        }

        @Test
        fun `partitioning evenly`() {
            val ints = intProgressionOf(1, 10, 1)
            val partitioned = ints.partitioning(2).toFastList()
            partitioned.size shouldBeEqualTo 2
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo intProgressionOf(1, 5)
            partitioned[1] shouldBeEqualTo intProgressionOf(6, 10)
        }

        @Test
        fun `partitioning oddly`() {
            val ints = intProgressionOf(1, 10, 1)
            val partitioned = ints.partitioning(3).toFastList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo intProgressionOf(1, 4)
            partitioned[1] shouldBeEqualTo intProgressionOf(5, 8)
            partitioned[2] shouldBeEqualTo intProgressionOf(9, 10)
        }

        @Test
        fun `partitioning reversed`() {
            val ints = intProgressionOf(10, 1, -1)
            val partitioned = ints.partitioning(3).toFastList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo intProgressionOf(10, 7, -1)
            partitioned[1] shouldBeEqualTo intProgressionOf(6, 3, -1)
            partitioned[2] shouldBeEqualTo intProgressionOf(2, 1, -1)
        }
    }

    @Nested
    inner class LongProgression {
        @Test
        fun `create progression`() {
            val longs = longProgressionOf(1, 10, 1)
            longs.size() shouldBeEqualTo 10
        }

        @Test
        fun `as stream`() {
            val longs = longProgressionOf(1, 10, 1)
            longs.asStream().count() shouldBeEqualTo 10
        }

        @Test
        fun `chunked progression`() {
            val longs = longProgressionOf(1, 10, 1)
            longs.size() shouldBeEqualTo 10
            val chunked = longs.chunked(2).toFastList()
            chunked.size shouldBeEqualTo 5
            chunked.forEach {
                log.debug { "group=$it" }
            }
        }

        @Test
        fun `partitioning evenly`() {
            val longs = longProgressionOf(1, 10, 1)
            val partitioned = longs.partitioning(2).toFastList()
            partitioned.size shouldBeEqualTo 2
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo longProgressionOf(1, 5)
            partitioned[1] shouldBeEqualTo longProgressionOf(6, 10)
        }

        @Test
        fun `partitioning oddly`() {
            val longs = longProgressionOf(1, 10, 1)
            val partitioned = longs.partitioning(3).toFastList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo longProgressionOf(1, 4)
            partitioned[1] shouldBeEqualTo longProgressionOf(5, 8)
            partitioned[2] shouldBeEqualTo longProgressionOf(9, 10)
        }

        @Test
        fun `partitioning reversed`() {
            val longs = longProgressionOf(10, 1, -1)
            val partitioned = longs.partitioning(3).toFastList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo longProgressionOf(10, 7, -1)
            partitioned[1] shouldBeEqualTo longProgressionOf(6, 3, -1)
            partitioned[2] shouldBeEqualTo longProgressionOf(2, 1, -1)
        }
    }
}
