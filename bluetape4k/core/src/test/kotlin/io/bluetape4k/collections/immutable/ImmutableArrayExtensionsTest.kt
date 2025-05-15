package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.multiplicativeSpecializations.map
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.asInt
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ImmutableArrayExtensionsTest {

    companion object: KLogging()

    @Test
    fun chunk_immutable_array_partial_windowed() {
        val array = immutableArrayOf("1", "2", "3", "4", "5")
        val chunks = array.chunked(3, true)

        chunks shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3"),
            immutableArrayOf("4", "5")
        )

        val intChunk = array.chunked(3, true) { it.map { it.asInt() } }
        intChunk shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1, 2, 3),
            immutableArrayOf(4, 5)
        )

        // partial windows 가 False 일 때에는 마지막 chunk 가 size 보다 작으면 추가하지 않는다.
        val chunksNotPartial = array.chunked(3, false)
        chunksNotPartial shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3")
        )

        val intChunkNotPartial = array.chunked(3, false) { it.map { it.asInt() } }
        intChunkNotPartial shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1, 2, 3)
        )
    }

    @Test
    fun sliding_immutable_array() {
        val array = immutableArrayOf("1", "2", "3", "4")

        val sliding = array.sliding(3, false)
        sliding shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3"),
            immutableArrayOf("2", "3", "4")
        )

        val sliding2 = array.sliding(3, true)
        sliding2 shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3"),
            immutableArrayOf("2", "3", "4"),
            immutableArrayOf("3", "4"),
            immutableArrayOf("4")
        )
    }

    @Test
    fun `windowed immutable array`() {
        val array = immutableArrayOf("1", "2", "3", "4", "5")
        val windowed = array.windowed(3, 1, false)

        windowed shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3"),
            immutableArrayOf("2", "3", "4"),
            immutableArrayOf("3", "4", "5")
        )

        val windowed2 = array.windowed(3, 1, true)

        windowed2 shouldBeEqualTo immutableArrayOf(
            immutableArrayOf("1", "2", "3"),
            immutableArrayOf("2", "3", "4"),
            immutableArrayOf("3", "4", "5"),
            immutableArrayOf("4", "5"),
            immutableArrayOf("5")
        )
    }
}
