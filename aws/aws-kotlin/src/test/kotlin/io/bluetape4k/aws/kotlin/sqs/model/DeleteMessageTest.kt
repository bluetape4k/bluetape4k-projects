package io.bluetape4k.aws.kotlin.sqs.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DeleteMessageTest {

    companion object : KLogging()

    private val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"

    @Test
    fun `deleteMessageRequestOf는 queueUrl로 요청을 생성한다`() {
        val req = deleteMessageRequestOf(queueUrl = queueUrl)

        req.queueUrl shouldBeEqualTo queueUrl
    }

    @Test
    fun `deleteMessageRequestOf는 receiptHandle을 설정할 수 있다`() {
        val req = deleteMessageRequestOf(
            queueUrl = queueUrl,
            receiptHandle = "AQEBwJnKyrHigUMZj6reyYjyudnBuGxo..."
        )

        req.receiptHandle shouldBeEqualTo "AQEBwJnKyrHigUMZj6reyYjyudnBuGxo..."
    }

    @Test
    fun `deleteMessageRequestOf는 빈 queueUrl을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteMessageRequestOf(queueUrl = "")
        }
    }

    @Test
    fun `deleteMessageBatchRequestEntryOf는 id와 receiptHandle로 entry를 생성한다`() {
        val entry = deleteMessageBatchRequestEntryOf(id = "msg-001", receiptHandle = "receipt-001")

        entry.id shouldBeEqualTo "msg-001"
        entry.receiptHandle shouldBeEqualTo "receipt-001"
    }

    @Test
    fun `deleteMessageBatchRequestEntryOf는 receiptHandle을 설정할 수 있다`() {
        val entry = deleteMessageBatchRequestEntryOf(
            id = "msg-001",
            receiptHandle = "receiptHandle1"
        )

        entry.receiptHandle shouldBeEqualTo "receiptHandle1"
    }

    @Test
    fun `deleteMessageBatchRequestEntryOf는 빈 id를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteMessageBatchRequestEntryOf(id = "")
        }
    }

    @Test
    fun `deleteMessageBatchRequestOf Collection entries로 요청을 생성한다`() {
        val entries = listOf(
            deleteMessageBatchRequestEntryOf("id1", "rh1"),
            deleteMessageBatchRequestEntryOf("id2", "rh2"),
        )
        val req = deleteMessageBatchRequestOf(queueUrl = queueUrl, entries = entries)

        req.queueUrl shouldBeEqualTo queueUrl
        req.entries.shouldNotBeNull()
        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `deleteMessageBatchRequestOf vararg entries로 요청을 생성한다`() {
        val req = deleteMessageBatchRequestOf(
            queueUrl,
            deleteMessageBatchRequestEntryOf("id1", "rh1"),
            deleteMessageBatchRequestEntryOf("id2", "rh2"),
        )

        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `deleteMessageBatchRequestOf는 빈 entries를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteMessageBatchRequestOf(queueUrl = queueUrl, entries = emptyList())
        }
    }
}
