package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType
import software.amazon.awssdk.services.kinesis.model.StreamStatus
import java.time.Duration

/**
 * LocalStack을 사용한 AWS Kinesis 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackKinesisServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    private fun buildKinesisClient(server: LocalStackServer): KinesisClient =
        KinesisClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.KINESIS))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    /** 스트림이 [StreamStatus.ACTIVE] 상태가 될 때까지 대기합니다. */
    private fun KinesisClient.waitUntilActive(streamName: String) {
        await atMost Duration.ofSeconds(30) until {
            describeStream(
                DescribeStreamRequest.builder().streamName(streamName).build()
            ).streamDescription().streamStatus() == StreamStatus.ACTIVE
        }
    }

    @Test
    fun `Kinesis 스트림 생성 후 레코드 발행 및 소비`() {
        LocalStackServer().withServices(LocalStackContainer.Service.KINESIS).use { server ->
            server.start()
            val kinesis = buildKinesisClient(server)
            val streamName = "test-stream"

            // 스트림 생성 및 활성화 대기
            kinesis.createStream(
                CreateStreamRequest.builder().streamName(streamName).shardCount(1).build()
            )
            kinesis.waitUntilActive(streamName)

            // 레코드 발행
            val payload = "LocalStack Kinesis 테스트 레코드"
            kinesis.putRecord(
                PutRecordRequest.builder()
                    .streamName(streamName)
                    .partitionKey("pk-1")
                    .data(SdkBytes.fromUtf8String(payload))
                    .build()
            )

            // 샤드 이터레이터 획득 (처음부터 읽기)
            val shardId = kinesis.describeStream(
                DescribeStreamRequest.builder().streamName(streamName).build()
            ).streamDescription().shards().first().shardId()

            val initialIterator = kinesis.getShardIterator(
                GetShardIteratorRequest.builder()
                    .streamName(streamName)
                    .shardId(shardId)
                    .shardIteratorType(ShardIteratorType.TRIM_HORIZON)
                    .build()
            ).shardIterator()
            initialIterator.shouldNotBeNull()

            // 레코드 수신 (nextShardIterator를 추적하며 폴링)
            var currentIterator = initialIterator
            var receivedData: String? = null
            await atMost Duration.ofSeconds(15) until {
                val response = kinesis.getRecords(
                    GetRecordsRequest.builder().shardIterator(currentIterator).limit(10).build()
                )
                currentIterator = response.nextShardIterator() ?: currentIterator
                val records = response.records()
                if (records.isNotEmpty()) {
                    receivedData = records.first().data().asUtf8String()
                    true
                } else {
                    false
                }
            }

            receivedData shouldBeEqualTo payload
        }
    }

    @Test
    fun `Kinesis PutRecords로 배치 레코드 발행`() {
        LocalStackServer().withServices(LocalStackContainer.Service.KINESIS).use { server ->
            server.start()
            val kinesis = buildKinesisClient(server)
            val streamName = "batch-stream"

            kinesis.createStream(
                CreateStreamRequest.builder().streamName(streamName).shardCount(2).build()
            )
            kinesis.waitUntilActive(streamName)

            // 배치 레코드 발행 (PutRecords)
            val recordCount = 5
            val entries = (1..recordCount).map { i ->
                PutRecordsRequestEntry.builder()
                    .partitionKey("pk-$i")
                    .data(SdkBytes.fromUtf8String("배치 메시지 #$i"))
                    .build()
            }
            val response = kinesis.putRecords(
                PutRecordsRequest.builder()
                    .streamName(streamName)
                    .records(entries)
                    .build()
            )

            response.failedRecordCount() shouldBeEqualTo 0
            response.records().size shouldBeEqualTo recordCount

            // 스트림 목록 조회
            val streams = kinesis.listStreams(ListStreamsRequest.builder().build()).streamNames()
            streams.size shouldBeGreaterThan 0
        }
    }
}
