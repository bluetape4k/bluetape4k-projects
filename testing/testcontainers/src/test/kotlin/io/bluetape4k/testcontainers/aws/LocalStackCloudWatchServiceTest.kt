package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest
import java.time.Instant

/**
 * LocalStack을 사용한 AWS CloudWatch / CloudWatchLogs 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackCloudWatchServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `CloudWatch 커스텀 메트릭 데이터 발행 및 목록 조회`() {
        LocalStackServer().withServices(LocalStackContainer.Service.CLOUDWATCH).use { server ->
            server.start()

            val cloudWatch = CloudWatchClient.builder()
                .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCH))
                .region(Region.of(server.region))
                .credentialsProvider(server.getCredentialProvider())
                .build()
                .apply { ShutdownQueue.register(this) }

            val namespace = "Bluetape4k/ApplicationMetrics"

            // 커스텀 메트릭 발행 (카운터)
            cloudWatch.putMetricData(
                PutMetricDataRequest.builder()
                    .namespace(namespace)
                    .metricData(
                        MetricDatum.builder()
                            .metricName("RequestCount")
                            .value(42.0)
                            .unit(StandardUnit.COUNT)
                            .timestamp(Instant.now())
                            .dimensions(
                                Dimension.builder().name("Service").value("ApiGateway").build(),
                                Dimension.builder().name("Environment").value("test").build(),
                            )
                            .build(),
                        MetricDatum.builder()
                            .metricName("ResponseTimeMs")
                            .value(125.5)
                            .unit(StandardUnit.MILLISECONDS)
                            .timestamp(Instant.now())
                            .dimensions(
                                Dimension.builder().name("Service").value("ApiGateway").build(),
                            )
                            .build(),
                    )
                    .build()
            )

            // 네임스페이스별 메트릭 목록 조회
            val metrics = cloudWatch.listMetrics(
                ListMetricsRequest.builder().namespace(namespace).build()
            ).metrics()
            metrics.size shouldBeGreaterThan 0
        }
    }

    @Test
    fun `CloudWatchLogs 로그 그룹 및 스트림 생성 후 로그 이벤트 발행`() {
        LocalStackServer().withServices(LocalStackContainer.Service.CLOUDWATCHLOGS).use { server ->
            server.start()

            val logsClient = CloudWatchLogsClient.builder()
                .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCHLOGS))
                .region(Region.of(server.region))
                .credentialsProvider(server.getCredentialProvider())
                .build()
                .apply { ShutdownQueue.register(this) }

            val logGroupName = "/bluetape4k/application"
            val logStreamName = "instance-001"

            // 로그 그룹 생성
            logsClient.createLogGroup(
                CreateLogGroupRequest.builder().logGroupName(logGroupName).build()
            )

            // 로그 스트림 생성
            logsClient.createLogStream(
                CreateLogStreamRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .build()
            )

            // 로그 이벤트 발행
            val now = Instant.now().toEpochMilli()
            val events = listOf(
                InputLogEvent.builder().timestamp(now).message("INFO  Application started").build(),
                InputLogEvent.builder().timestamp(now + 100).message("INFO  Processing request #1").build(),
                InputLogEvent.builder().timestamp(now + 200).message("ERROR Failed to connect to database").build(),
            )
            logsClient.putLogEvents(
                PutLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .logEvents(events)
                    .build()
            )

            // 로그 그룹 목록 조회
            val logGroups = logsClient.describeLogGroups(
                DescribeLogGroupsRequest.builder().build()
            ).logGroups()
            logGroups.shouldNotBeEmpty()
            logGroups.first().logGroupName().shouldNotBeBlank()

            // 로그 스트림 목록 조회
            val logStreams = logsClient.describeLogStreams(
                DescribeLogStreamsRequest.builder().logGroupName(logGroupName).build()
            ).logStreams()
            logStreams.shouldNotBeEmpty()
        }
    }

    @Test
    fun `CloudWatchLogs 다수 로그 그룹 생성 후 필터 조회`() {
        LocalStackServer().withServices(LocalStackContainer.Service.CLOUDWATCHLOGS).use { server ->
            server.start()

            val logsClient = CloudWatchLogsClient.builder()
                .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCHLOGS))
                .region(Region.of(server.region))
                .credentialsProvider(server.getCredentialProvider())
                .build()
                .apply { ShutdownQueue.register(this) }

            // 여러 로그 그룹 생성
            val logGroups = listOf("/app/service-a", "/app/service-b", "/infra/nginx")
            logGroups.forEach { name ->
                logsClient.createLogGroup(CreateLogGroupRequest.builder().logGroupName(name).build())
            }

            // 접두사로 필터링
            val filtered = logsClient.describeLogGroups(
                DescribeLogGroupsRequest.builder().logGroupNamePrefix("/app/").build()
            ).logGroups()
            filtered.size shouldBeGreaterThan 0
        }
    }
}
