package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.testcontainers.aws.ministack.AbstractMiniStackServiceTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
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
 * MiniStack CloudWatch / CloudWatchLogs 서비스 통합 테스트.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackCloudWatchTest : AbstractMiniStackServiceTest() {

    companion object : KLogging() {
        private val NAMESPACE = "Bluetape4k/MiniStack-Test-${System.currentTimeMillis()}"
        private val LOG_GROUP_NAME = "/bluetape4k/ministack-test-${System.currentTimeMillis()}"
        private const val LOG_STREAM_NAME = "app-stream"
    }

    private val cloudWatchClient by lazy {
        CloudWatchClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .httpClient(ApacheHttpClient.create())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private val cloudWatchLogsClient by lazy {
        CloudWatchLogsClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
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
                .message("로그 이벤트 #$i - MiniStack CloudWatchLogs 테스트")
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
        groups.map { it.logGroupName() }.shouldContain(LOG_GROUP_NAME)
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
