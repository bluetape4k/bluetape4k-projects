package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class SkewnessTest {

    companion object: KLogging()

    @Test
    fun `대칭 분포는 skewness 가 0 에 가깝다`() {
        // 대칭 균등 분포
        val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        val result = data.skewness()
        result.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `Iterable skewness 가 동작한다`() {
        val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 20.0)
        val result = data.skewness()
        // 오른쪽으로 치우친 분포는 양의 skewness
        assert(result > 0.0) { "오른쪽 치우침 분포는 양의 skewness 이어야 합니다" }
    }

    @Test
    fun `Sequence skewness 가 동작한다`() {
        val data = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val result = data.skewness()
        result.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `Int Iterable skewness 가 동작한다`() {
        val data = listOf(1, 2, 3, 4, 5)
        val result = data.skewness()
        result.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `Iterable 과 Sequence skewness 가 같은 결과를 반환한다`() {
        val values = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        val iterableResult = values.skewness()
        val sequenceResult = values.asSequence().skewness()
        iterableResult.shouldBeNear(sequenceResult, 1e-10)
    }
}
