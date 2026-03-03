package io.bluetape4k.aws.kotlin.kinesis

import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType
import aws.sdk.kotlin.services.kinesis.model.StreamStatus
import io.bluetape4k.aws.kotlin.kinesis.model.putRecordsRequestEntryOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.await
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.Duration

/**
 * AWS Kotlin SDK [aws.sdk.kotlin.services.kinesis.KinesisClient] 확장 함수 테스트.
 *
 * 스트림 생성 → ACTIVE 대기 → 레코드 전송 → 조회 → 삭제 순서로 테스트합니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KinesisClientExtensionsTest: AbstractKotlinKinesisTest() {

    companion object: KLoggingChannel() {
        private val STREAM_NAME = "kotlin-test-stream-" + Base58.randomString(6).lowercase()
    }

    private lateinit var shardId: String
    private lateinit var shardIterator: String

    @Test
    @Order(1)
    fun `스트림 생성`() = runSuspendIO {
        val response = client.createStream(STREAM_NAME, shardCount = 1)
        log.debug { "createStream response=$response" }
        response.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `스트림 ACTIVE 상태 대기`() = runSuspendIO {
        var status: StreamStatus? = null
        await.atMost(Duration.ofSeconds(30))
            .pollInterval(Duration.ofSeconds(1))
            .untilSuspending {
                val desc = client.describeStream(STREAM_NAME)
                status = desc.streamDescription?.streamStatus
                status == StreamStatus.Active
            }
        status.shouldNotBeNull()
    }

    @Test
    @Order(3)
    fun `단일 레코드 전송`() = runSuspendIO {
        val data = "Hello Kotlin Kinesis!".toByteArray()
        val response = client.putRecord(STREAM_NAME, "partition-1", data)

        response.sequenceNumber.shouldNotBeEmpty()
        response.shardId.shouldNotBeEmpty()
        shardId = response.shardId
        log.debug { "putRecord sequenceNumber=${response.sequenceNumber}, shardId=$shardId" }
    }

    @Test
    @Order(4)
    fun `복수 레코드 배치 전송`() = runSuspendIO {
        val entries = (1..5).map { i ->
            putRecordsRequestEntryOf(
                partitionKey = "partition-$i",
                data = "kotlin-message-$i".toByteArray()
            )
        }
        val response = client.putRecords(STREAM_NAME, entries)

        log.debug { "putRecords failedRecordCount=${response.failedRecordCount}" }
        response.records.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    @Order(5)
    fun `샤드 이터레이터 조회`() = runSuspendIO {
        val response = client.getShardIterator(STREAM_NAME, shardId, ShardIteratorType.TrimHorizon)

        shardIterator = response.shardIterator!!
        shardIterator.shouldNotBeEmpty()
        log.debug { "shardIterator=$shardIterator" }
    }

    @Test
    @Order(6)
    fun `레코드 조회`() = runSuspendIO {
        val response = client.getRecords(shardIterator, limit = 100)

        log.debug { "getRecords count=${response.records.size}" }
        response.records.forEach { record ->
            log.debug { "record partitionKey=${record.partitionKey}, data=${record.data.decodeToString()}" }
        }
    }

    @Test
    @Order(7)
    fun `스트림 삭제`() = runSuspendIO {
        val response = client.deleteStream(STREAM_NAME)
        log.debug { "deleteStream response=$response" }
        response.shouldNotBeNull()
    }
}
