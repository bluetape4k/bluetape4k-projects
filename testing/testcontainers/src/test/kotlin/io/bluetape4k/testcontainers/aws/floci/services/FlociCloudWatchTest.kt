package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.aws.floci.AbstractFlociServiceTest
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent
import java.time.Instant

/**
 * [io.bluetape4k.testcontainers.aws.FlociServer]를 사용한 CloudWatch 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.localstack.services.CloudWatchTest]에 대응합니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociCloudWatchTest : AbstractFlociServiceTest() {

    companion object : KLogging() {
        private val NAMESPACE = "Bluetape4k/Test-${System.currentTimeMillis()}"
        private val LOG_GROUP_NAME = "/bluetape4k/test-${System.currentTimeMillis()}"
        private const val LOG_STREAM_NAME = "app-stream"
    }

    private val cloudWatchClient: CloudWatchClient by lazy {
        CloudWatchClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .httpClient(ApacheHttpClient.create())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private val cloudWatchLogsClient: CloudWatchLogsClient by lazy {
        CloudWatchLogsClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .httpClient(ApacheHttpClient.create())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    @Test
    @Order(1)
    fun `create client`() {
        cloudWatchClient.shouldNotBeNull()
        cloudWatchLogsClient.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `put metric data`() {
        val response = cloudWatchClient.putMetricData {
            it.namespace(NAMESPACE)
                .metricData(
                    MetricDatum.builder()
                        .metricName("RequestCount")
                        .value(42.0)
                        .unit(StandardUnit.COUNT)
                        .build(),
                    MetricDatum.builder()
                        .metricName("ResponseTimeMs")
                        .value(125.5)
                        .unit(StandardUnit.MILLISECONDS)
                        .build(),
                )
        }
        log.debug { "PutMetricData response: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(3)
    fun `list metrics`() {
        val metrics = cloudWatchClient.listMetrics { it.namespace(NAMESPACE) }.metrics()
        log.debug { "Metrics: ${metrics.map { it.metricName() }}" }
        metrics.size shouldBeGreaterOrEqualTo 1
    }

    @Test
    @Order(4)
    fun `create log group`() {
        val response = cloudWatchLogsClient.createLogGroup { it.logGroupName(LOG_GROUP_NAME) }
        log.debug { "CreateLogGroup: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(5)
    fun `create log stream`() {
        val response = cloudWatchLogsClient.createLogStream {
            it.logGroupName(LOG_GROUP_NAME).logStreamName(LOG_STREAM_NAME)
        }
        log.debug { "CreateLogStream: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    fun `put log events`() {
        val now = Instant.now().toEpochMilli()
        val events = (1..3).map { i ->
            InputLogEvent.builder()
                .timestamp(now + i * 100L)
                .message("로그 이벤트 #$i - Floci CloudWatchLogs 테스트")
                .build()
        }
        val response = cloudWatchLogsClient.putLogEvents {
            it.logGroupName(LOG_GROUP_NAME)
                .logStreamName(LOG_STREAM_NAME)
                .logEvents(events)
        }
        log.debug { "PutLogEvents nextSequenceToken=${response.nextSequenceToken()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(7)
    fun `describe log groups`() {
        val groups = cloudWatchLogsClient.describeLogGroups { }.logGroups()
        log.debug { "Log groups: ${groups.map { it.logGroupName() }}" }
        groups.shouldNotBeEmpty()
    }

    @Test
    @Order(8)
    fun `describe log streams`() {
        val streams = cloudWatchLogsClient.describeLogStreams {
            it.logGroupName(LOG_GROUP_NAME)
        }.logStreams()
        log.debug { "Log streams: ${streams.map { it.logStreamName() }}" }
        streams.shouldNotBeEmpty()
    }
}
