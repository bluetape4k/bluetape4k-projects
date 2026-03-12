package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class DescriptivesTest {

    companion object: KLogging()

    private val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)

    @Test
    fun `descriptiveStatistics 평균이 올바르다`() {
        val ds = data.descriptiveStatistics()
        ds.mean.shouldBeNear(5.5, 1e-10)
    }

    @Test
    fun `descriptiveStatistics 최솟값과 최댓값이 올바르다`() {
        val ds = data.descriptiveStatistics()
        ds.min shouldBeEqualTo 1.0
        ds.max shouldBeEqualTo 10.0
    }

    @Test
    fun `descriptiveStatistics 크기가 올바르다`() {
        val ds = data.descriptiveStatistics()
        ds.size shouldBeEqualTo 10L
    }

    @Test
    fun `descriptiveStatistics 합이 올바르다`() {
        val ds = data.descriptiveStatistics()
        ds.sum.shouldBeNear(55.0, 1e-10)
    }

    @Test
    fun `descriptiveStatistics 분산이 양수이다`() {
        val ds = data.descriptiveStatistics()
        assert(ds.variance > 0.0) { "분산은 양수여야 합니다" }
    }

    @Test
    fun `descriptiveStatistics 표준편차가 양수이다`() {
        val ds = data.descriptiveStatistics()
        assert(ds.standardDeviation > 0.0) { "표준편차는 양수여야 합니다" }
    }

    @Test
    fun `descriptiveStatistics percentile 이 올바른 값을 반환한다`() {
        val ds = data.descriptiveStatistics()
        // 50th percentile (중앙값) ≈ 5.5
        ds.percentile(50.0).shouldBeNear(5.5, 0.1)
        // 1st percentile ≈ 최솟값 (0은 허용되지 않음 - 범위: (0, 100])
        ds.percentile(1.0).shouldBeNear(1.0, 1.0)
    }

    @Test
    fun `descriptiveStatistics get 연산자가 동작한다`() {
        val ds = data.descriptiveStatistics()
        ds[0] shouldBeEqualTo 1.0
        ds[9] shouldBeEqualTo 10.0
    }

    @Test
    fun `descriptiveStatistics values 가 원본 데이터를 반환한다`() {
        val ds = data.descriptiveStatistics()
        ds.values.size shouldBeEqualTo 10
        ds.values[0] shouldBeEqualTo 1.0
    }

    @Test
    fun `Sequence descriptiveStatistics 가 동작한다`() {
        val ds = data.asSequence().descriptiveStatistics()
        ds.mean.shouldBeNear(5.5, 1e-10)
        ds.size shouldBeEqualTo 10L
    }

    @Test
    fun `단일 값의 표준편차는 0 이다`() {
        val ds = listOf(42.0).descriptiveStatistics()
        ds.standardDeviation shouldBeEqualTo 0.0
    }
}
