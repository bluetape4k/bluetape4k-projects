package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
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
import java.net.URI
import java.time.Instant

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val NAMESPACE = "Bluetape4k/Test-${System.currentTimeMillis()}"
        private val LOG_GROUP_NAME = "/bluetape4k/test-${System.currentTimeMillis()}"
        private const val LOG_STREAM_NAME = "app-stream"
    }

    private val cloudWatch: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack
            .withServices("clowdwatch", "cloudwatchlogs")
    }

    private val cloudWatchEndpoint: URI
        get() = cloudWatch.endpoint

    private val cloudWatchLogsEndpoint: URI
        get() = cloudWatch.endpoint

    private val cloudWatchClient by lazy {
        CloudWatchClient.builder()
            .endpointOverride(cloudWatchEndpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(cloudWatch.getCredentialProvider())
            .httpClient(ApacheHttpClient.create())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val cloudWatchLogsClient by lazy {
        CloudWatchLogsClient.builder()
            .endpointOverride(cloudWatchLogsEndpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(cloudWatch.getCredentialProvider())
            .httpClient(ApacheHttpClient.create())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    @BeforeAll
    fun setup() {
        cloudWatch.start()
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
        // NOTE: LocalStack v4 Community Edition 버그로 인해 MetricDatum에 Dimension을 포함하면
        // CloudWatch Query 프로토콜의 form-encoded body 파싱 실패 → "unknown operation" 500 오류 발생.
        // 따라서 dimensions() 및 timestamp() 없이 최소 파라미터로만 테스트한다.
        // 참고: https://github.com/localstack/localstack/issues (LocalStack v4 CloudWatch Dimension parsing bug)
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
                .message("로그 이벤트 #$i - LocalStack CloudWatchLogs 테스트")
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
