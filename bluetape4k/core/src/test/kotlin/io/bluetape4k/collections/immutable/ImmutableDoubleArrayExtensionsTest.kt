package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ImmutableDoubleArrayExtensionsTest {

    companion object: KLogging()

    @Test
    fun `chunk immutable array - partial windowed`() {
        val array = immutableArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val chunks = array.chunked(3, true)

        chunks shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0),
            immutableArrayOf(4.0, 5.0)
        )

        // partial windows 가 False 일 때에는 마지막 chunk 가 size 보다 작으면 추가하지 않는다.
        val chunksNotPartial = array.chunked(3, false)
        chunksNotPartial shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0)
        )
    }

    @Test
    fun `sliding immutable array`() {
        val array = immutableArrayOf(1.0, 2.0, 3.0, 4.0)

        val sliding = array.sliding(3, false)
        sliding shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0),
            immutableArrayOf(2.0, 3.0, 4.0),
        )

        val sliding2 = array.sliding(3, true)
        sliding2 shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0),
            immutableArrayOf(2.0, 3.0, 4.0),
            immutableArrayOf(3.0, 4.0),
            immutableArrayOf(4.0)
        )
    }

    @Test
    fun `windowed immutable array`() {
        val array = immutableArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val windowed = array.windowed(3, 1, false)

        windowed shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0),
            immutableArrayOf(2.0, 3.0, 4.0),
            immutableArrayOf(3.0, 4.0, 5.0),
        )

        val windowed2 = array.windowed(3, 1, true)

        windowed2 shouldBeEqualTo immutableArrayOf(
            immutableArrayOf(1.0, 2.0, 3.0),
            immutableArrayOf(2.0, 3.0, 4.0),
            immutableArrayOf(3.0, 4.0, 5.0),
            immutableArrayOf(4.0, 5.0),
            immutableArrayOf(5.0)
        )
    }
}