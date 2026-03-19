package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs.inputLogEventOf
import io.bluetape4k.aws.cloudwatch.model.metricDatumOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
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
class CloudWatchAsyncClientCoroutinesExtensionsTest: AbstractCloudWatchTest() {

    companion object: KLogging() {
        private val NAMESPACE = "bluetape4k/coroutines-${Base58.randomString(6).lowercase()}"
        private val METRIC_NAME = "CoroutinesMetric-${Base58.randomString(4).lowercase()}"
        private val LOG_GROUP_NAME = "/bluetape4k/coroutines-${Base58.randomString(6).lowercase()}"
        private val LOG_STREAM_NAME = "coroutines-stream-${Base58.randomString(4).lowercase()}"
    }

    @Test
    @Order(1)
    fun `put metric data with coroutines`() = runSuspendIO {
        // NOTE: LocalStack v4 Community Edition 버그로 인해 MetricDatum에 Dimension을 포함하면
        // CloudWatch Query 프로토콜의 form-encoded body 파싱 실패 → "unknown operation" 500 오류 발생.
        // 따라서 dimensions 없이 최소 파라미터로만 테스트한다.
        val datum = metricDatumOf(METRIC_NAME, 99.0, StandardUnit.COUNT)

        val response = asyncClient.putMetricData(NAMESPACE, datum)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "coroutine putMetricData status: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(2)
    fun `list metrics with coroutines`() = runSuspendIO {
        // 먼저 데이터 게시
        asyncClient.putMetricData(NAMESPACE, metricDatumOf(METRIC_NAME, 1.0, StandardUnit.COUNT))

        val response = asyncClient.listMetrics(namespace = NAMESPACE)

        response.metrics().shouldNotBeNull().shouldNotBeEmpty()
        response.metrics().forEach { metric ->
            log.debug { "coroutine metric: ${metric.metricName()}" }
        }
    }

    @Test
    @Order(3)
    fun `create log group with coroutines`() = runSuspendIO {
        val response = logsAsyncClient.createLogGroup(LOG_GROUP_NAME)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "coroutine createLogGroup status: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(4)
    fun `create log stream with coroutines`() = runSuspendIO {
        val response = logsAsyncClient.createLogStream(LOG_GROUP_NAME, LOG_STREAM_NAME)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "coroutine createLogStream status: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(5)
    fun `put log events with coroutines`() = runSuspendIO {
        val events = listOf(
            inputLogEventOf(System.currentTimeMillis(), "Coroutine log message 1"),
            inputLogEventOf(System.currentTimeMillis() + 1, "Coroutine log message 2"),
        )

        val response = logsAsyncClient.putLogEvents(LOG_GROUP_NAME, LOG_STREAM_NAME, events)

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        log.debug { "coroutine putLogEvents status: ${response.sdkHttpResponse().statusCode()}" }
    }

    @Test
    @Order(6)
    fun `describe log groups with coroutines`() = runSuspendIO {
        val response = logsAsyncClient.describeLogGroups(logGroupNamePrefix = "/bluetape4k")

        response.logGroups().shouldNotBeNull().shouldNotBeEmpty()
        response.logGroups().forEach { group ->
            log.debug { "coroutine logGroup: ${group.logGroupName()}" }
        }
    }

    @Test
    @Order(7)
    fun `describe log streams with coroutines`() = runSuspendIO {
        val response = logsAsyncClient.describeLogStreams(LOG_GROUP_NAME)

        response.logStreams().shouldNotBeNull().shouldNotBeEmpty()
        response.logStreams().forEach { stream ->
            log.debug { "coroutine logStream: ${stream.logStreamName()}" }
        }
    }
}
