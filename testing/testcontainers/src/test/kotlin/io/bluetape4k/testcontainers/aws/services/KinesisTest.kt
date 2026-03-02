package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType
import software.amazon.awssdk.services.kinesis.model.StreamStatus
import java.net.URI
import java.time.Duration

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KinesisTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val STREAM_NAME = "test-stream-${System.currentTimeMillis()}"
    }

    private val kinesisServer: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.KINESIS)
    }
    private val endpoint: URI get() = kinesisServer.getEndpointOverride(LocalStackContainer.Service.KINESIS)

    private val kinesisClient: KinesisClient by lazy {
        KinesisClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(kinesisServer.region))
            .credentialsProvider(kinesisServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    @BeforeAll
    fun setup() {
        kinesisServer.start()
    }

    @Test
    @Order(1)
    fun `create stream`() {
        kinesisClient.createStream { it.streamName(STREAM_NAME).shardCount(1) }

        // 스트림이 ACTIVE 상태가 될 때까지 대기
        await atMost Duration.ofSeconds(30) until {
            kinesisClient.describeStream { it.streamName(STREAM_NAME) }
                .streamDescription().streamStatus() == StreamStatus.ACTIVE
        }
        log.debug { "Stream $STREAM_NAME is now ACTIVE" }
    }

    @Test
    @Order(2)
    fun `list streams`() {
        val streams = kinesisClient.listStreams().streamNames()
        log.debug { "Streams: $streams" }
        streams.shouldNotBeNull()
    }

    @Test
    @Order(3)
    fun `put record`() {
        val response = kinesisClient.putRecord {
            it.streamName(STREAM_NAME)
                .partitionKey("pk-1")
                .data(SdkBytes.fromUtf8String("Hello Kinesis!"))
        }
        log.debug { "SequenceNumber: ${response.sequenceNumber()}, ShardId: ${response.shardId()}" }
        response.sequenceNumber().shouldNotBeNull()
    }

    @Test
    @Order(4)
    fun `put records batch`() {
        val entries = (1..5).map { i ->
            PutRecordsRequestEntry.builder()
                .partitionKey("pk-$i")
                .data(SdkBytes.fromUtf8String("배치 메시지 #$i"))
                .build()
        }
        val response = kinesisClient.putRecords {
            it.streamName(STREAM_NAME).records(entries)
        }
        log.debug { "FailedRecordCount: ${response.failedRecordCount()}" }
        response.failedRecordCount() shouldBeEqualTo 0
    }

    @Test
    @Order(5)
    fun `get records`() {
        val shardId = kinesisClient.describeStream { it.streamName(STREAM_NAME) }
            .streamDescription().shards().first().shardId()

        val shardIterator = kinesisClient.getShardIterator {
            it.streamName(STREAM_NAME)
                .shardId(shardId)
                .shardIteratorType(ShardIteratorType.TRIM_HORIZON)
        }.shardIterator()

        var receivedCount = 0
        var currentIterator = shardIterator
        await atMost Duration.ofSeconds(15) until {
            val response = kinesisClient.getRecords {
                it.shardIterator(currentIterator).limit(10)
            }
            currentIterator = response.nextShardIterator() ?: currentIterator
            receivedCount += response.records().size
            response.records().isNotEmpty()
        }
        log.debug { "Received $receivedCount records" }
        receivedCount shouldBeGreaterOrEqualTo 1
    }

    @Test
    @Order(6)
    fun `describe stream`() {
        val description = kinesisClient.describeStream { it.streamName(STREAM_NAME) }.streamDescription()
        log.debug { "Stream status: ${description.streamStatus()}, shards: ${description.shards().size}" }
        description.streamStatus() shouldBeEqualTo StreamStatus.ACTIVE
    }

    @Test
    @Order(7)
    fun `delete stream`() {
        val response = kinesisClient.deleteStream { it.streamName(STREAM_NAME) }
        log.debug { "DeleteStream HTTP status: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }
}
