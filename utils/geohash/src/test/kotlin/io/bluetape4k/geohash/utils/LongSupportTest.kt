package io.bluetape4k.geohash.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class LongSupportTest {

    companion object: KLogging()

    @Test
    fun `same number have 64 bits prefix`() {
        for (a in 0L until 120_000_000L step 101L) {
            a.commonPrefixLength(a) shouldBeEqualTo 64
        }
    }

    @Test
    fun `verify with known prefix lengths`() {
        val a = 0x8F00000000000000UL.toLong()
        val b = 0x8000000000000000UL.toLong()
        val c = 0x8800000000000000UL.toLong()

        assertPrefixLength(a, b, 4)
        assertPrefixLength(b, c, 4)
        assertPrefixLength(a, c, 5)
        assertPrefixLength(0x0, a, 0)
        assertPrefixLength(0x8888300000000000UL.toLong(), 0x8888c00000000000UL.toLong(), 16)
    }

    private fun assertPrefixLength(a: Long, b: Long, length: Int) {
        a.commonPrefixLength(b) shouldBeEqualTo length
    }
}
