package io.bluetape4k.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigIntegerPairTest {

    @Test
    fun `pair and unpair는 원래 값을 복원한다`() {
        val hi = BigInteger.valueOf(-123_456_789L)
        val lo = BigInteger.valueOf(987_654_321L)

        val encoded = BigIntegerPair.pair(hi, lo)
        val decoded = BigIntegerPair.unpair(encoded)

        decoded.first shouldBeEqualTo hi
        decoded.second shouldBeEqualTo lo
    }

    @Test
    fun `경계값 pair and unpair를 지원한다`() {
        val hi = BigInteger.valueOf(Long.MAX_VALUE)
        val lo = BigInteger.valueOf(Long.MIN_VALUE)

        val encoded = BigIntegerPair.pair(hi, lo)
        val decoded = BigIntegerPair.unpair(encoded)

        decoded.first shouldBeEqualTo hi
        decoded.second shouldBeEqualTo lo
    }

    @Test
    fun `unsigned와 signed 변환은 역변환 가능하다`() {
        with(BigIntegerPair) {
            val source = BigInteger.valueOf(-1L)
            source.unsigned().signed() shouldBeEqualTo source
        }
    }

}
