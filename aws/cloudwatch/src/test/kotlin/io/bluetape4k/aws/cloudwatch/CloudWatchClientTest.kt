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
