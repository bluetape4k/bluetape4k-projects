package io.bluetape4k.aws.sqs

import io.bluetape4k.aws.sqs.model.receiveMessageRequestOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry

class SqsValidationTest: AbstractSqsTest() {

    @Test
    fun `receiveMessages validates maxResults range in sync client`() {
        val queueUrl = "https://example.com/queue/demo"

        assertThrows<IllegalArgumentException> {
            client.receiveMessages(queueUrl, maxResults = 0)
        }
        assertThrows<IllegalArgumentException> {
            client.receiveMessages(queueUrl, maxResults = 11)
        }
    }

    @Test
    fun `receiveMessages validates maxResults range in async client`() {
        val queueUrl = "https://example.com/queue/demo"

        assertThrows<IllegalArgumentException> {
            asyncClient.receiveMessagesAsync(queueUrl, maxResults = 0)
        }
        assertThrows<IllegalArgumentException> {
            asyncClient.receiveMessagesAsync(queueUrl, maxResults = 11)
        }
    }

    @Test
    fun `batch operations reject empty entries`() {
        val queueUrl = "https://example.com/queue/demo"

        assertThrows<IllegalArgumentException> {
            client.sendBatch(queueUrl, entries = emptyList<SendMessageBatchRequestEntry>())
        }
        assertThrows<IllegalArgumentException> {
            client.changeMessageVisibilityBatch(
                queueUrl,
                entries = emptyList<ChangeMessageVisibilityBatchRequestEntry>()
            )
        }
        assertThrows<IllegalArgumentException> {
            client.deleteMessageBatch(queueUrl, entries = emptyList<DeleteMessageBatchRequestEntry>())
        }

        assertThrows<IllegalArgumentException> {
            asyncClient.sendBatchAsync(queueUrl, entries = emptyList<SendMessageBatchRequestEntry>())
        }
        assertThrows<IllegalArgumentException> {
            asyncClient.changeMessageVisibilityBatchAsync(
                queueUrl,
                entries = emptyList<ChangeMessageVisibilityBatchRequestEntry>()
            )
        }
        assertThrows<IllegalArgumentException> {
            asyncClient.deleteMessageBatchAsync(queueUrl, entries = emptyList<DeleteMessageBatchRequestEntry>())
        }
    }

    @Test
    fun `receiveMessageRequestOf validates maxNumber and waitTimeSeconds`() {
        val queueUrl = "https://example.com/queue/demo"

        assertThrows<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl, maxNumber = 0)
        }
        assertThrows<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl, maxNumber = 11)
        }
        assertThrows<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl, waitTimeSeconds = -1)
        }
        assertThrows<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl, waitTimeSeconds = 21)
        }
    }

}
