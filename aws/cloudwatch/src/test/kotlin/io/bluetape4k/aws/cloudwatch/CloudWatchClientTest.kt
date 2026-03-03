package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.cloudwatch.model.metricDatumOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchClientTest: AbstractCloudWatchTest() {

    companion object: KLogging() {
        private val NAMESPACE = "bluetape4k/test-${Base58.randomString(6).lowercase()}"
        private val METRIC_NAME = "TestMetric-${Base58.randomString(4).lowercase()}"
    }

    @Test
    @Order(1)
    fun `put metric data`() {
        // NOTE: LocalStack v4 Community Edition 버그로 인해 MetricDatum에 Dimension을 포함하면
        // CloudWatch Query 프로토콜의 form-encoded body 파싱 실패 → "unknown operation" 500 오류 발생.
        // 따라서 metricDatumOf() 헬퍼를 사용하여 dimensions 없이 최소 파라미터로만 테스트한다.
        val datum = metricDatumOf(
            metricName = METRIC_NAME,
            value = 42.0,
            unit = StandardUnit.COUNT
        )

        val response = client.putMetricData(NAMESPACE, datum)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "putMetricData response: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(2)
    fun `put multiple metric data`() {
        // NOTE: LocalStack v4 버그로 인해 Dimension 없이 테스트 (위 `put metric data` 주석 참조)
        val data = listOf(
            metricDatumOf(METRIC_NAME, 10.0, StandardUnit.COUNT),
            metricDatumOf(METRIC_NAME, 20.0, StandardUnit.COUNT),
            metricDatumOf(METRIC_NAME, 30.0, StandardUnit.COUNT),
        )

        val response = client.putMetricData(NAMESPACE, data)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "putMetricData (multiple) response: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(3)
    fun `list metrics`() {
        val response = client.listMetrics(namespace = NAMESPACE)

        response.metrics().shouldNotBeNull().shouldNotBeEmpty()
        response.metrics().forEach { metric ->
            log.debug { "metric: namespace=${metric.namespace()}, name=${metric.metricName()}" }
        }
    }

    @Test
    @Order(4)
    fun `list metrics with metric name filter`() {
        val response = client.listMetrics(namespace = NAMESPACE, metricName = METRIC_NAME)

        response.metrics().shouldNotBeNull().shouldNotBeEmpty()
        response.metrics().forEach { metric ->
            log.debug { "filtered metric: ${metric.metricName()}" }
        }
    }
}
