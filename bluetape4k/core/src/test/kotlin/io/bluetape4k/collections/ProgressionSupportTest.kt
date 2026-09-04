package io.bluetape4k.collections

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProgressionSupportTest {

    companion object: KLogging()

    @Test
    fun `create CharProgression`() {
        val chars = charProgressionOf('a', 'z')
        chars.toList() shouldHaveSize 26
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
        fun `as stream stops at positive Int boundary`() {
            intProgressionOf(Int.MAX_VALUE - 1, Int.MAX_VALUE, 2)
                .asStream()
                .limit(2)
                .toArray() shouldBeEqualTo intArrayOf(Int.MAX_VALUE - 1)
        }

        @Test
        fun `as stream stops at Int MIN_VALUE with step minus one`() {
            intProgressionOf(Int.MIN_VALUE, Int.MIN_VALUE, -1)
                .asStream()
                .limit(2)
                .toArray() shouldBeEqualTo intArrayOf(Int.MIN_VALUE)
        }

        @Test
        fun `as stream stops at negative Int boundary`() {
            intProgressionOf(Int.MIN_VALUE + 2, Int.MIN_VALUE, -2)
                .asStream()
                .limit(3)
                .toArray() shouldBeEqualTo intArrayOf(Int.MIN_VALUE + 2, Int.MIN_VALUE)
        }

        @Test
        fun `as stream preserves normal negative Int progression`() {
            intProgressionOf(3, 1, -1)
                .asStream()
                .toArray() shouldBeEqualTo intArrayOf(3, 2, 1)
        }

        @Test
        fun `chunked progression`() {
            val ints = intProgressionOf(1, 10, 1)
            ints.size() shouldBeEqualTo 10
            val chunked = ints.chunked(2).toList()
            chunked.size shouldBeEqualTo 5
            chunked.forEach {
                log.debug { "group=$it" }
            }
        }

        @Test
        fun `chunked preserves descending non-unit steps`() {
            intProgressionOf(10, 1, -2).chunked(2).toList() shouldBeEqualTo listOf(
                intProgressionOf(10, 8, -2),
                intProgressionOf(6, 4, -2),
                intProgressionOf(2, 2, -2),
            )
        }

        @Test
        fun `empty progression keeps chunked and partitioning contracts`() {
            val empty = intProgressionOf(3, 1)

            empty.chunked(2).toList() shouldBeEqualTo emptyList()
            empty.partitioning(1).toList() shouldBeEqualTo listOf(empty)
            empty.partitioning(2).toList() shouldBeEqualTo emptyList()
        }

        @Test
        fun `partitioning evenly`() {
            val ints = intProgressionOf(1, 10, 1)
            val partitioned = ints.partitioning(2).toList()
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
            val partitioned = ints.partitioning(3).toList()
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
            val partitioned = ints.partitioning(3).toList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo intProgressionOf(10, 7, -1)
            partitioned[1] shouldBeEqualTo intProgressionOf(6, 3, -1)
            partitioned[2] shouldBeEqualTo intProgressionOf(2, 1, -1)
        }

        @Test
        fun `partitioning more than size`() {
            val ints = intProgressionOf(1, 3, 1)
            val partitioned = ints.partitioning(5).toList()
            partitioned.size shouldBeEqualTo 3
            partitioned[0] shouldBeEqualTo intProgressionOf(1, 1, 1)
            partitioned[1] shouldBeEqualTo intProgressionOf(2, 2, 1)
            partitioned[2] shouldBeEqualTo intProgressionOf(3, 3, 1)
        }

        @Test
        fun `partitioning preserves the full Int range`() {
            val ints = intProgressionOf(Int.MIN_VALUE, Int.MAX_VALUE)
            val partitioned = ints.partitioning(2).toList()

            partitioned shouldHaveSize 2
            partitioned[0] shouldBeEqualTo intProgressionOf(Int.MIN_VALUE, -1)
            partitioned[1] shouldBeEqualTo intProgressionOf(0, Int.MAX_VALUE)
            ints.partitioning(1).toList() shouldBeEqualTo listOf(ints)
        }

        @Test
        fun `partitioning preserves extreme Int ranges with a non-unit step`() {
            val ascending = intProgressionOf(Int.MIN_VALUE, Int.MAX_VALUE, 2)
            val ascendingParts = ascending.partitioning(2).toList()
            ascendingParts[0] shouldBeEqualTo intProgressionOf(Int.MIN_VALUE, -2, 2)
            ascendingParts[1] shouldBeEqualTo intProgressionOf(0, Int.MAX_VALUE - 1, 2)

            val descending = intProgressionOf(Int.MAX_VALUE, Int.MIN_VALUE, -2)
            val descendingParts = descending.partitioning(2).toList()
            descendingParts[0] shouldBeEqualTo intProgressionOf(Int.MAX_VALUE, 1, -2)
            descendingParts[1] shouldBeEqualTo intProgressionOf(-1, Int.MIN_VALUE + 1, -2)
        }

        @Test
        fun `partitioning preserves an extreme descending Int range`() {
            val ints = intProgressionOf(Int.MAX_VALUE, Int.MIN_VALUE, -1)
            val partitioned = ints.partitioning(2).toList()

            partitioned shouldHaveSize 2
            partitioned[0] shouldBeEqualTo intProgressionOf(Int.MAX_VALUE, 0, -1)
            partitioned[1] shouldBeEqualTo intProgressionOf(-1, Int.MIN_VALUE, -1)
        }

        @Test
        fun `chunked rejects an Int partition count that cannot be represented`() {
            assertFailsWith<IllegalArgumentException> {
                intProgressionOf(Int.MIN_VALUE, Int.MAX_VALUE).chunked(1).toList()
            }
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
        fun `as stream stops at positive Long boundary`() {
            longProgressionOf(Long.MAX_VALUE - 1L, Long.MAX_VALUE, 2L)
                .asStream()
                .limit(2)
                .toArray() shouldBeEqualTo longArrayOf(Long.MAX_VALUE - 1L)
        }

        @Test
        fun `as stream stops at Long MIN_VALUE with step minus one`() {
            longProgressionOf(Long.MIN_VALUE, Long.MIN_VALUE, -1L)
                .asStream()
                .limit(2)
                .toArray() shouldBeEqualTo longArrayOf(Long.MIN_VALUE)
        }

        @Test
        fun `as stream stops at negative Long boundary`() {
            longProgressionOf(Long.MIN_VALUE + 2L, Long.MIN_VALUE, -2L)
                .asStream()
                .limit(3)
                .toArray() shouldBeEqualTo longArrayOf(Long.MIN_VALUE + 2L, Long.MIN_VALUE)
        }

        @Test
        fun `as stream preserves normal negative Long progression`() {
            longProgressionOf(3L, 1L, -1L)
                .asStream()
                .toArray() shouldBeEqualTo longArrayOf(3L, 2L, 1L)
        }

        @Test
        fun `chunked progression`() {
            val longs = longProgressionOf(1, 10, 1)
            longs.size() shouldBeEqualTo 10
            val chunked = longs.chunked(2).toList()
            chunked.size shouldBeEqualTo 5
            chunked.forEach {
                log.debug { "group=$it" }
            }
        }

        @Test
        fun `chunked preserves descending non-unit steps`() {
            longProgressionOf(10, 1, -2).chunked(2).toList() shouldBeEqualTo listOf(
                longProgressionOf(10, 8, -2),
                longProgressionOf(6, 4, -2),
                longProgressionOf(2, 2, -2),
            )
        }

        @Test
        fun `empty progression keeps chunked and partitioning contracts`() {
            val empty = longProgressionOf(3, 1)

            empty.chunked(2).toList() shouldBeEqualTo emptyList()
            empty.partitioning(1).toList() shouldBeEqualTo listOf(empty)
            empty.partitioning(2).toList() shouldBeEqualTo emptyList()
        }

        @Test
        fun `partitioning evenly`() {
            val longs = longProgressionOf(1, 10, 1)
            val partitioned = longs.partitioning(2).toList()
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
            val partitioned = longs.partitioning(3).toList()
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
            val partitioned = longs.partitioning(3).toList()
            partitioned.size shouldBeEqualTo 3
            partitioned.forEach {
                log.debug { "progression=$it" }
            }
            partitioned[0] shouldBeEqualTo longProgressionOf(10, 7, -1)
            partitioned[1] shouldBeEqualTo longProgressionOf(6, 3, -1)
            partitioned[2] shouldBeEqualTo longProgressionOf(2, 1, -1)
        }

        @Test
        fun `partitioning more than size`() {
            val longs = longProgressionOf(1, 3, 1)
            val partitioned = longs.partitioning(5).toList()
            partitioned.size shouldBeEqualTo 3
            partitioned[0] shouldBeEqualTo longProgressionOf(1, 1, 1)
            partitioned[1] shouldBeEqualTo longProgressionOf(2, 2, 1)
            partitioned[2] shouldBeEqualTo longProgressionOf(3, 3, 1)
        }

        @Test
        fun `partitioning preserves the full Long range`() {
            val longs = longProgressionOf(Long.MIN_VALUE, Long.MAX_VALUE)
            val partitioned = longs.partitioning(2).toList()

            partitioned shouldHaveSize 2
            partitioned[0] shouldBeEqualTo longProgressionOf(Long.MIN_VALUE, -1L)
            partitioned[1] shouldBeEqualTo longProgressionOf(0L, Long.MAX_VALUE)
            longs.partitioning(1).toList() shouldBeEqualTo listOf(longs)
        }

        @Test
        fun `partitioning preserves extreme Long ranges with a non-unit step`() {
            val ascending = longProgressionOf(Long.MIN_VALUE, Long.MAX_VALUE, 2L)
            val ascendingParts = ascending.partitioning(2).toList()
            ascendingParts[0] shouldBeEqualTo longProgressionOf(Long.MIN_VALUE, -2L, 2L)
            ascendingParts[1] shouldBeEqualTo longProgressionOf(0L, Long.MAX_VALUE - 1L, 2L)

            val descending = longProgressionOf(Long.MAX_VALUE, Long.MIN_VALUE, -2L)
            val descendingParts = descending.partitioning(2).toList()
            descendingParts[0] shouldBeEqualTo longProgressionOf(Long.MAX_VALUE, 1L, -2L)
            descendingParts[1] shouldBeEqualTo longProgressionOf(-1L, Long.MIN_VALUE + 1L, -2L)
        }

        @Test
        fun `partitioning preserves an extreme descending Long range`() {
            val longs = longProgressionOf(Long.MAX_VALUE, Long.MIN_VALUE, -1L)
            val partitioned = longs.partitioning(2).toList()

            partitioned shouldHaveSize 2
            partitioned[0] shouldBeEqualTo longProgressionOf(Long.MAX_VALUE, 0L, -1L)
            partitioned[1] shouldBeEqualTo longProgressionOf(-1L, Long.MIN_VALUE, -1L)
        }

        @Test
        fun `chunked rejects a Long partition count that cannot be represented`() {
            assertFailsWith<IllegalArgumentException> {
                longProgressionOf(Long.MIN_VALUE, Long.MAX_VALUE).chunked(1).toList()
            }
        }
    }
}
