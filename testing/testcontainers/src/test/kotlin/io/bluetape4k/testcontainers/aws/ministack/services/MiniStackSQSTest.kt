package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeBlank
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry

/**
 * MiniStack SQS 서비스 통합 테스트.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackSQSTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val QUEUE_NAME = "ministack-test-queue-${System.currentTimeMillis()}"
    }

    private val miniStack: MiniStackServer by lazy { MiniStackServer.Launcher.miniStack }

    private val sqsClient: SqsClient by lazy {
        SqsClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private lateinit var queueUrl: String

    @Test
    @Order(1)
    fun `create queue`() {
        sqsClient.createQueue { it.queueName(QUEUE_NAME) }
        queueUrl = sqsClient.getQueueUrl { it.queueName(QUEUE_NAME) }.queueUrl()
        log.debug { "Queue url=$queueUrl" }
    }

    @Test
    @Order(2)
    fun `list queue`() {
        val queues = sqsClient.listQueues { it.queueNamePrefix("ministack") }.queueUrls()
        queues.forEach { log.debug { "queue url=$it" } }
    }

    @Test
    @Order(3)
    fun `send message`() {
        val response = sqsClient.sendMessage {
            it.queueUrl(queueUrl).messageBody("Hello MiniStack SQS!")
        }
        log.debug { "sendResponse=$response" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
        response.messageId().shouldNotBeBlank()
    }

    @Test
    @Order(4)
    fun `send batch messages`() {
        val entries = List(10) {
            SendMessageBatchRequestEntry.builder()
                .id("id$it")
                .messageBody("Hello MiniStack SQS $it")
                .build()
        }
        val response = sqsClient.sendMessageBatch { it.queueUrl(queueUrl).entries(entries) }
        response.successful() shouldHaveSize entries.size
        response.successful().forEach { log.debug { "result entry=$it" } }
    }

    @Test
    @Order(5)
    fun `receive messages`() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(queueUrl).maxNumberOfMessages(3)
        }.messages()
        messages.size shouldBeGreaterOrEqualTo 1
    }

    @Test
    @Order(6)
    fun `change message visibility`() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(queueUrl).maxNumberOfMessages(3)
        }.messages()
        val responses = messages.map { message ->
            sqsClient.changeMessageVisibility {
                it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()).visibilityTimeout(100)
            }
        }
        responses shouldHaveSize messages.size
    }

    @Test
    @Order(7)
    fun `delete messages`() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(queueUrl).maxNumberOfMessages(3)
        }.messages()
        val responses = messages.map { message ->
            sqsClient.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
        }
        responses shouldHaveSize messages.size
    }
}
