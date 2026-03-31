package io.bluetape4k.aws.kotlin.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit
import io.bluetape4k.aws.kotlin.cloudwatch.model.metricDatumOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchClientExtensionsTest: AbstractKotlinCloudWatchTest() {

    companion object: KLoggingChannel() {
        private val NAMESPACE = "bluetape4k/kotlin-test-${Base58.randomString(6).lowercase()}"
        private val METRIC_NAME = "KotlinTestMetric-${Base58.randomString(4).lowercase()}"
    }

    @Test
    @Order(1)
    fun `put single metric data`() = runSuspendIO {
        // NOTE: LocalStack v4 Community Edition 버그로 인해 MetricDatum에 Dimension을 포함하면
        // CloudWatch Query 프로토콜의 form-encoded body 파싱 실패 → "unknown operation" 500 오류 발생.
        // 따라서 dimensions 없이 최소 파라미터로만 테스트한다.
        val datum = metricDatumOf(METRIC_NAME, 42.0, StandardUnit.Count)

        withCloudWatchClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.putMetricData(NAMESPACE, datum)
            log.debug { "putMetricData completed: namespace=$NAMESPACE, metric=$METRIC_NAME" }
        }
    }

    @Test
    @Order(2)
    fun `put multiple metric data`() = runSuspendIO {
        val data = listOf(
            metricDatumOf(METRIC_NAME, 10.0, StandardUnit.Count),
            metricDatumOf(METRIC_NAME, 20.0, StandardUnit.Count),
            metricDatumOf(METRIC_NAME, 30.0, StandardUnit.Count),
        )

        withCloudWatchClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            client.putMetricData(NAMESPACE, data)
            log.debug { "putMetricData (multiple) completed: count=${data.size}" }
        }
    }

    @Test
    @Order(3)
    fun `list metrics`() = runSuspendIO {
        withCloudWatchClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.listMetrics(namespace = NAMESPACE)
            response.metrics.shouldNotBeNull().shouldNotBeEmpty()
            response.metrics!!.forEach { metric ->
                log.debug { "metric: namespace=${metric.namespace}, name=${metric.metricName}" }
            }
        }
    }

    @Test
    @Order(4)
    fun `list metrics with metric name filter`() = runSuspendIO {
        withCloudWatchClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.listMetrics(namespace = NAMESPACE, metricName = METRIC_NAME)
            response.metrics.shouldNotBeNull().shouldNotBeEmpty()
            response.metrics!!.forEach { metric ->
                log.debug { "filtered metric: ${metric.metricName}" }
            }
        }
    }
}
