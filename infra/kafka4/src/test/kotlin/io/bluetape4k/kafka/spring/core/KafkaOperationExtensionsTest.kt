package io.bluetape4k.kafka.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.common.Metric
import org.apache.kafka.common.MetricName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaOperations

/**
 * [getMetric], [getMetricValueOrNull] 확장 함수에 대한 MockK 단위 테스트입니다.
 *
 * suspendSend, sendFlowAsParallel, sendAndForget은 실제 Kafka 서버가 필요하므로
 * 이 테스트에서는 제외합니다.
 */
class KafkaOperationExtensionsTest {

    companion object : KLogging()

    private lateinit var kafkaOps: KafkaOperations<String, String>
    private lateinit var metricName: MetricName
    private lateinit var metric: Metric

    @BeforeEach
    fun setup() {
        kafkaOps = mockk(relaxed = true)
        metricName = mockk()
        metric = mockk()

        every { metricName.name() } returns "record-send-total"
        every { kafkaOps.metrics() } returns mapOf(metricName to metric)
    }

    @Test
    fun `getMetric - 존재하는 메트릭 이름으로 조회 시 Metric 반환`() {
        // Act
        val result = kafkaOps.getMetric("record-send-total")

        // Assert
        result.shouldNotBeNull()

        verify(exactly = 1) { kafkaOps.metrics() }
        confirmVerified(kafkaOps)
    }

    @Test
    fun `getMetric - 존재하지 않는 메트릭 이름으로 조회 시 null 반환`() {
        // Act
        val result = kafkaOps.getMetric("nonexistent")

        // Assert
        result.shouldBeNull()

        verify(exactly = 1) { kafkaOps.metrics() }
        confirmVerified(kafkaOps)
    }

    @Test
    fun `getMetricValueOrNull - 존재하는 메트릭 이름으로 조회 시 메트릭 값 반환`() {
        // Arrange
        every { metric.metricValue() } returns 42.0

        // Act
        val result = kafkaOps.getMetricValueOrNull("record-send-total")

        // Assert
        result shouldBeEqualTo 42.0

        verify(exactly = 1) { kafkaOps.metrics() }
        confirmVerified(kafkaOps)
    }

    @Test
    fun `getMetricValueOrNull - 존재하지 않는 메트릭 이름으로 조회 시 null 반환`() {
        // Act
        val result = kafkaOps.getMetricValueOrNull("nonexistent")

        // Assert
        result.shouldBeNull()

        verify(exactly = 1) { kafkaOps.metrics() }
        confirmVerified(kafkaOps)
    }
}
