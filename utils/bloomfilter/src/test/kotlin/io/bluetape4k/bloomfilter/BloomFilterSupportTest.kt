package io.bluetape4k.bloomfilter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BloomFilterSupportTest: AbstractBloomFilterTest() {

    companion object: KLogging()

    @Test
    fun `get optimal m and k with default setting`() {
        val maxBitSize = optimalM(DEFAULT_MAX_NUM, DEFAULT_ERROR_RATE)
        val hashFunCount = optimalK(DEFAULT_MAX_NUM, maxBitSize)

        log.debug { "maximum size=$maxBitSize, hash function count=$hashFunCount" }

        maxBitSize shouldBeEqualTo Int.MAX_VALUE
        hashFunCount shouldBeEqualTo 1
    }

    @Test
    fun `get optimal m and k with custom setting`() {
        val maxBitSize = optimalM(1000, 0.01)
        val hashFunCount = optimalK(1000, maxBitSize)



        log.debug { "maximum size=$maxBitSize, hash function count=$hashFunCount" }

        maxBitSize shouldBeEqualTo 9586
        hashFunCount shouldBeEqualTo 7

        val hashFuncCount2 = optimalK(1000, 0.01)
        hashFuncCount2 shouldBeEqualTo hashFunCount
    }
}
