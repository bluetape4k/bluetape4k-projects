package io.bluetape4k.aws.kotlin.sqs.model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SqsModelSupportTest {

    @Test
    fun `sendMessageRequestOf는 queueUrl과 messageBody를 설정한다`() {
        val request = sendMessageRequestOf(
            queueUrl = "https://localhost:4566/000000000000/test-queue",
            messageBody = "hello",
            delaySeconds = 3,
        )

        request.queueUrl shouldBeEqualTo "https://localhost:4566/000000000000/test-queue"
        request.messageBody shouldBeEqualTo "hello"
        request.delaySeconds shouldBeEqualTo 3
    }

    @Test
    fun `sendMessageBatchRequestOf는 엔트리를 설정한다`() {
        val entry = sendMessageBatchRequestEntryOf(id = "id-1", messageBody = "hello")

        val request = sendMessageBatchRequestOf(
            queueUrl = "https://localhost:4566/000000000000/test-queue",
            entries = listOf(entry),
        )

        request.entries?.size shouldBeEqualTo 1
        request.entries?.first()?.id shouldBeEqualTo "id-1"
    }

    @Test
    fun `receiveMessageRequestOf는 필드를 설정한다`() {
        val request = receiveMessageRequestOf(
            queueUrl = "https://localhost:4566/000000000000/test-queue",
            maxNumberOfMessages = 5,
            waitTimeSeconds = 10,
        )

        request.maxNumberOfMessages shouldBeEqualTo 5
        request.waitTimeSeconds shouldBeEqualTo 10
    }

    @Test
    fun `changeMessageVisibilityBatchRequestOf는 엔트리를 설정한다`() {
        val entry = changeMessageVisibilityBatchRequestEntryOf(
            id = "id-1",
            receiptHandle = "receipt-handle",
            visibilityTimeout = 5,
        )

        val request = changeMessageVisibilityBatchRequestOf(
            queueUrl = "https://localhost:4566/000000000000/test-queue",
            entries = listOf(entry),
        )

        request.entries?.size shouldBeEqualTo 1
        request.entries?.first()?.receiptHandle shouldBeEqualTo "receipt-handle"
    }

    @Test
    fun `deleteMessageBatchRequestOf는 엔트리를 설정한다`() {
        val entry = deleteMessageBatchRequestEntryOf(id = "id-1", receiptHandle = "receipt-handle")

        val request = deleteMessageBatchRequestOf(
            queueUrl = "https://localhost:4566/000000000000/test-queue",
            entries = listOf(entry),
        )

        request.entries?.size shouldBeEqualTo 1
        request.entries?.first()?.id shouldBeEqualTo "id-1"
    }

    @Test
    fun `receiveMessageRequestOf는 maxNumberOfMessages 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                maxNumberOfMessages = 0,
                waitTimeSeconds = 10,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                maxNumberOfMessages = 11,
                waitTimeSeconds = 10,
            )
        }
    }

    @Test
    fun `receiveMessageRequestOf는 waitTimeSeconds 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                maxNumberOfMessages = 1,
                waitTimeSeconds = -1,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                maxNumberOfMessages = 1,
                waitTimeSeconds = 21,
            )
        }
    }

    @Test
    fun `sendMessageBatchRequestOf는 빈 엔트리를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            sendMessageBatchRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                entries = emptyList(),
            )
        }
    }

    @Test
    fun `changeMessageVisibilityBatchRequestOf는 빈 엔트리를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            changeMessageVisibilityBatchRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                entries = emptyList(),
            )
        }
    }

    @Test
    fun `deleteMessageBatchRequestOf는 빈 엔트리를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteMessageBatchRequestOf(
                queueUrl = "https://localhost:4566/000000000000/test-queue",
                entries = emptyList(),
            )
        }
    }
}
