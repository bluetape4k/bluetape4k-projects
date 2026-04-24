package io.bluetape4k.aws.kotlin.kinesis.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PutRecordTest {

    companion object : KLogging()

    @Test
    fun `putRecordRequestOf는 streamName, partitionKey, data로 요청을 생성한다`() {
        val data = "hello".toByteArray()
        val req = putRecordRequestOf(
            streamName = "my-stream",
            partitionKey = "pk-001",
            data = data
        )

        req.streamName shouldBeEqualTo "my-stream"
        req.partitionKey shouldBeEqualTo "pk-001"
        req.data shouldBeEqualTo data
    }

    @Test
    fun `putRecordRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val req = putRecordRequestOf(
            streamName = "my-stream",
            partitionKey = "pk-001",
            data = "test".toByteArray()
        ) {
            sequenceNumberForOrdering = "49600047091...001"
        }

        req.shouldNotBeNull()
        req.sequenceNumberForOrdering shouldBeEqualTo "49600047091...001"
    }

    @Test
    fun `putRecordRequestOf는 빈 streamName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            putRecordRequestOf(streamName = "", partitionKey = "pk", data = ByteArray(0))
        }
    }

    @Test
    fun `putRecordRequestOf는 빈 partitionKey를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            putRecordRequestOf(streamName = "my-stream", partitionKey = "  ", data = ByteArray(0))
        }
    }

    @Test
    fun `빈 ByteArray 데이터도 허용된다`() {
        val req = putRecordRequestOf(
            streamName = "my-stream",
            partitionKey = "pk",
            data = ByteArray(0)
        )
        req.data.shouldNotBeNull()
        req.data!!.size shouldBeEqualTo 0
    }
}
