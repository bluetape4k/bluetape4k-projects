package io.bluetape4k.aws.kinesis

import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.awaitility.kotlin.await
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType
import software.amazon.awssdk.services.kinesis.model.StreamStatus
import java.time.Duration

/**
 * [KinesisAsyncClient] 코루틴 확장 함수 테스트.
 *
 * 스트림 생성 → 레코드 전송 → 조회 → 삭제 순서로 테스트합니다.
 */
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KinesisAsyncClientCoroutinesExtensionsTest: AbstractKinesisTest() {

    companion object: KLogging() {
        private val STREAM_NAME = "test-async-stream-" + Base58.randomString(6).lowercase()
    }

    private lateinit var shardId: String
    private lateinit var shardIterator: String

    @Test
    @Order(1)
    fun `코루틴으로 스트림 생성`() = runSuspendIO {
        val response = asyncClient.createStream(STREAM_NAME, shardCount = 1)
        log.debug { "createStream httpStatus=${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().statusCode() shouldBeGreaterOrEqualTo 200
    }

    @Test
    @Order(2)
    fun `스트림 ACTIVE 상태 대기`() {
        await.atMost(Duration.ofSeconds(30)).until {
            val desc = client.describeStream(STREAM_NAME)
            val status = desc.streamDescription().streamStatus()
            log.debug { "stream=$STREAM_NAME status=$status" }
            status == StreamStatus.ACTIVE
        }
    }

    @Test
    @Order(3)
    fun `코루틴으로 단일 레코드 전송`() = runSuspendIO {
        val data = SdkBytes.fromUtf8String("Hello Kinesis Coroutines!")
        val response = asyncClient.putRecord(STREAM_NAME, "partition-1", data)

        response.sequenceNumber().shouldNotBeEmpty()
        response.shardId().shouldNotBeEmpty()
        shardId = response.shardId()
        log.debug { "putRecord sequenceNumber=${response.sequenceNumber()}, shardId=$shardId" }
    }

    @Test
    @Order(4)
    fun `코루틴으로 복수 레코드 배치 전송`() = runSuspendIO {
        val entries = (1..5).map { i ->
            PutRecordsRequestEntry.builder()
                .partitionKey("partition-$i")
                .data(SdkBytes.fromUtf8String("async-message-$i"))
                .build()
        }
        val response = asyncClient.putRecords(STREAM_NAME, entries)

        log.debug { "putRecords failedRecordCount=${response.failedRecordCount()}" }
        response.records().shouldNotBeEmpty()
    }

    @Test
    @Order(5)
    fun `코루틴으로 샤드 이터레이터 조회`() = runSuspendIO {
        val response = asyncClient.getShardIterator(STREAM_NAME, shardId, ShardIteratorType.TRIM_HORIZON)

        shardIterator = response.shardIterator()
        shardIterator.shouldNotBeEmpty()
        log.debug { "shardIterator=$shardIterator" }
    }

    @Test
    @Order(6)
    fun `코루틴으로 레코드 조회`() = runSuspendIO {
        val response = asyncClient.getRecords(shardIterator, limit = 100)

        log.debug { "getRecords count=${response.records().size}" }
        response.records().forEach { record ->
            log.debug { "record partitionKey=${record.partitionKey()}, data=${record.data().asUtf8String()}" }
        }
    }

    @Test
    @Order(7)
    fun `코루틴으로 스트림 삭제`() = runSuspendIO {
        val response = asyncClient.deleteStream(STREAM_NAME)
        log.debug { "deleteStream httpStatus=${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().statusCode() shouldBeGreaterOrEqualTo 200
    }
}
