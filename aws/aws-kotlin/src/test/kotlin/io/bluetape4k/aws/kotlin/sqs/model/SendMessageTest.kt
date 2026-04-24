package io.bluetape4k.aws.kotlin.sqs.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SendMessageTest {

    companion object : KLogging()

    private val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"

    @Test
    fun `sendMessageRequestOf는 queueUrl과 messageBody로 요청을 생성한다`() {
        val req = sendMessageRequestOf(
            queueUrl = queueUrl,
            messageBody = "Hello, SQS!"
        )

        req.queueUrl shouldBeEqualTo queueUrl
        req.messageBody shouldBeEqualTo "Hello, SQS!"
    }

    @Test
    fun `sendMessageRequestOf는 지연 전송을 설정할 수 있다`() {
        val req = sendMessageRequestOf(
            queueUrl = queueUrl,
            messageBody = "Delayed message"
        ) {
            delaySeconds = 10
        }

        req.delaySeconds shouldBeEqualTo 10
    }

    @Test
    fun `sendMessageRequestOf는 빈 queueUrl을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            sendMessageRequestOf(queueUrl = "", messageBody = "test")
        }
    }

    @Test
    fun `sendMessageBatchRequestEntryOf는 id와 messageBody로 entry를 생성한다`() {
        val entry = sendMessageBatchRequestEntryOf(
            id = "msg-001",
            messageBody = "Batch message"
        )

        entry.id shouldBeEqualTo "msg-001"
        entry.messageBody shouldBeEqualTo "Batch message"
    }

    @Test
    fun `sendMessageBatchRequestEntryOf는 빈 id를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            sendMessageBatchRequestEntryOf(id = "", messageBody = "test")
        }
    }

    @Test
    fun `sendMessageBatchRequestOf는 queueUrl과 entries로 요청을 생성한다`() {
        val entries = listOf(
            sendMessageBatchRequestEntryOf("id1", "Message 1"),
            sendMessageBatchRequestEntryOf("id2", "Message 2"),
        )
        val req = sendMessageBatchRequestOf(queueUrl = queueUrl, entries = entries)

        req.queueUrl shouldBeEqualTo queueUrl
        req.entries.shouldNotBeNull()
        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `sendMessageBatchRequestOf vararg 버전으로 요청을 생성한다`() {
        val req = sendMessageBatchRequestOf(
            queueUrl,
            sendMessageBatchRequestEntryOf("id1", "Hello!"),
            sendMessageBatchRequestEntryOf("id2", "World!")
        )

        req.entries!!.size shouldBeEqualTo 2
    }

    @Test
    fun `sendMessageBatchRequestOf는 빈 entries를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            sendMessageBatchRequestOf(queueUrl = queueUrl, entries = emptyList())
        }
    }
}
