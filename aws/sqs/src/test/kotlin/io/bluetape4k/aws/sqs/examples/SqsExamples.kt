package io.bluetape4k.aws.sqs.examples

import io.bluetape4k.aws.sqs.AbstractSqsTest
import io.bluetape4k.codec.Base58
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SqsExamples: AbstractSqsTest() {

    companion object: KLogging() {
        private const val QUEUE_PREFIX = "test-queue"
        private val QUEUE_NAME = "$QUEUE_PREFIX-${Base58.randomString(12).lowercase()}"
    }

    private lateinit var queueUrl: String

    @Test
    @Order(1)
    fun `create queue`() {
        val url = client.createQueue { it.queueName(QUEUE_NAME) }.queueUrl()
        queueUrl = client.getQueueUrl { it.queueName(QUEUE_NAME) }.queueUrl()

        queueUrl shouldBeEqualTo url
        log.debug { "queue url=$queueUrl" }
    }

    @Test
    @Order(2)
    fun `list queues`() {
        val response = client.listQueues { it.queueNamePrefix(QUEUE_PREFIX) }

        response.queueUrls() shouldHaveSize 1
        response.queueUrls().forEach {
            log.debug { "queue url=$it" }
        }
    }

    @Test
    @Order(3)
    fun `send message`() {
        val response = client.sendMessage {
            it.queueUrl(queueUrl).messageBody("Hello, World!").delaySeconds(3)
        }
        response.messageId().shouldNotBeEmpty()
        log.debug { "response=$response" }
    }

    @Test
    @Order(4)
    fun `send messages in batch mode`() {
        // NOTE: The total size of all messages that you send in a single SendMessageBatch call can't exceed 262,144 bytes (256 KB).
        // https://stackoverflow.com/questions/40489815/checking-size-of-sqs-message-batches
        // 이 것 계산하려면 Jdk Serializer를 통해서 bytes 를 계산해야 한다
        val entries = fastList(10) {
            SendMessageBatchRequestEntry.builder()
                .id("id-$it")
                .messageBody("Hello, world $it")
                .build()
        }
        val response = client.sendMessageBatch {
            it.queueUrl(queueUrl).entries(entries)
        }
        response.successful() shouldHaveSize entries.size
        response.successful().forEach {
            log.debug { "result entry=$it" }
        }
    }

    @Test
    @Order(5)
    fun `receive messages`() {
        val messages = client
            .receiveMessage {
                it.queueUrl(queueUrl).maxNumberOfMessages(3)
            }
            .messages()

        messages shouldHaveSize 3
        messages.forEach {
            log.trace { "message=$it" }
        }
    }

    @Test
    @Order(6)
    fun `chnage messages`() {
        val messages = client
            .receiveMessage { it.queueUrl(queueUrl).maxNumberOfMessages(3) }
            .messages()

        val responses = messages.map { message ->
            client.changeMessageVisibility {
                it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()).visibilityTimeout(10)
            }
        }
        responses shouldHaveSize messages.size
        responses.forEach {
            log.debug { "response  metadata=${it.responseMetadata()}" }
        }
    }

    @Test
    @Order(7)
    fun `delete messages`() {
        val messages = client
            .receiveMessage { it.queueUrl(queueUrl).maxNumberOfMessages(3) }
            .messages()

        val responses = messages.map { message ->
            client.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
        }
        responses shouldHaveSize messages.size
        responses.forEach {
            log.debug { "response=$it" }
        }
    }

    @Test
    @Order(8)
    fun `delete queue`() {
        val response: DeleteQueueResponse = client.deleteQueue { it.queueUrl(queueUrl) }
        response.responseMetadata().requestId().shouldNotBeEmpty()
    }
}
