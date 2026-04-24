package io.bluetape4k.aws.kotlin.sqs.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MessageVisibilityTest {

    companion object : KLogging()

    private val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"

    @Test
    fun `changeMessageVisibilityRequestOf는 queueUrl과 receiptHandle로 요청을 생성한다`() {
        val req = changeMessageVisibilityRequestOf(
            queueUrl = queueUrl,
            receiptHandle = "AQEBreceipt...",
            visibilityTimeout = 30
        )

        req.queueUrl shouldBeEqualTo queueUrl
        req.receiptHandle shouldBeEqualTo "AQEBreceipt..."
        req.visibilityTimeout shouldBeEqualTo 30
    }

    @Test
    fun `changeMessageVisibilityRequestOf는 빈 queueUrl을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            changeMessageVisibilityRequestOf(queueUrl = "", receiptHandle = "handle")
        }
    }

    @Test
    fun `changeMessageVisibilityBatchRequestEntryOf는 id, receiptHandle, visibilityTimeout으로 entry를 생성한다`() {
        val entry = changeMessageVisibilityBatchRequestEntryOf(
            id = "entry-1",
            receiptHandle = "AQEBhandle...",
            visibilityTimeout = 60
        )

        entry.id shouldBeEqualTo "entry-1"
        entry.receiptHandle shouldBeEqualTo "AQEBhandle..."
        entry.visibilityTimeout shouldBeEqualTo 60
    }

    @Test
    fun `changeMessageVisibilityBatchRequestOf Collection entries로 요청을 생성한다`() {
        val entries = listOf(
            changeMessageVisibilityBatchRequestEntryOf("id1", "rh1", 30),
            changeMessageVisibilityBatchRequestEntryOf("id2", "rh2", 60),
        )
        val req = changeMessageVisibilityBatchRequestOf(queueUrl = queueUrl, entries = entries)

        req.queueUrl shouldBeEqualTo queueUrl
        req.entries.shouldNotBeNull()
        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `changeMessageVisibilityBatchRequestOf vararg entries로 요청을 생성한다`() {
        val req = changeMessageVisibilityBatchRequestOf(
            queueUrl,
            changeMessageVisibilityBatchRequestEntryOf("id1", "rh1", 30),
            changeMessageVisibilityBatchRequestEntryOf("id2", "rh2", 60)
        )

        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `changeMessageVisibilityBatchRequestOf는 빈 entries를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            changeMessageVisibilityBatchRequestOf(queueUrl = queueUrl, entries = emptyList())
        }
    }
}
