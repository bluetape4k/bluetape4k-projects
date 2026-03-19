package io.bluetape4k.bloomfilter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
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

    @Test
    fun `오류율이 낮을수록 m이 커진다`() {
        val m1 = optimalM(10_000, 0.1)
        val m2 = optimalM(10_000, 0.01)
        val m3 = optimalM(10_000, 0.001)

        log.debug { "m1=$m1, m2=$m2, m3=$m3" }

        (m2 > m1).shouldBeTrue()
        (m3 > m2).shouldBeTrue()
    }

    @Test
    fun `요소 수가 많을수록 m이 커진다`() {
        val m1 = optimalM(100, 0.01)
        val m2 = optimalM(1_000, 0.01)
        val m3 = optimalM(10_000, 0.01)

        log.debug { "m1=$m1, m2=$m2, m3=$m3" }

        (m2 > m1).shouldBeTrue()
        (m3 > m2).shouldBeTrue()
    }

    @Test
    fun `optimalK는 항상 양수를 반환한다`() {
        val k1 = optimalK(100, 0.01)
        val k2 = optimalK(1_000, 0.001)
        val k3 = optimalK(10_000, 0.0001)

        k1 shouldBeGreaterThan 0
        k2 shouldBeGreaterThan 0
        k3 shouldBeGreaterThan 0
    }
}
