package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class KurtosisTest {

    companion object: KLogging()

    @Test
    fun `Iterable kurtosis 가 동작한다`() {
        // 정규분포에 가까운 데이터는 kurtosis ≈ 0 (초과 첨도)
        val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        val result = data.kurtosis()
        // 균등분포의 초과 첨도는 음수
        assert(!result.isNaN()) { "kurtosis 가 NaN 이어선 안 됩니다" }
    }

    @Test
    fun `Sequence kurtosis 가 동작한다`() {
        val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        val result = data.kurtosis()
        assert(!result.isNaN()) { "kurtosis 가 NaN 이어선 안 됩니다" }
    }

    @Test
    fun `Int Iterable kurtosis 가 동작한다`() {
        val data = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val result = data.kurtosis()
        assert(!result.isNaN()) { "kurtosis 가 NaN 이어선 안 됩니다" }
    }

    @Test
    fun `Iterable 과 Sequence kurtosis 가 같은 결과를 반환한다`() {
        val values = listOf(2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0)
        val iterableResult = values.kurtosis()
        val sequenceResult = values.asSequence().kurtosis()
        iterableResult.shouldBeNear(sequenceResult, 1e-10)
    }
}
