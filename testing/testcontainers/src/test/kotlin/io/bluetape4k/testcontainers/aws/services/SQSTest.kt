package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SQSTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val QUEUE_NAME = "test-queue-${System.currentTimeMillis()}"
    }

    private val sqsServer: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.SQS)
    }
    private val endpoint: URI get() = sqsServer.getEndpointOverride(LocalStackContainer.Service.SQS)
    private val region get() = Region.of(sqsServer.region)

    private val sqsClient: SqsClient by lazy {
        SqsClient.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(sqsServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private lateinit var queueUrl: String

    @BeforeAll
    fun setup() {
        sqsServer.start()
    }

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
        val listResponse = sqsClient.listQueues { it.queueNamePrefix("test") }

        listResponse.queueUrls().forEach {
            log.debug { "queue url=$it" }
        }
    }

    @Test
    @Order(3)
    fun `send message`() {
        val sendResponse = sqsClient.sendMessage {
            it.queueUrl(queueUrl)
                .messageBody("Hello world")
                .delaySeconds(10)
        }
        log.debug { "sendResponse=$sendResponse" }
    }

    @Test
    @Order(4)
    fun `send batch messages`() {
        // NOTE: The total size of all messages that you send in a single SendMessageBatch call can't exceed 262,144 bytes (256 KB).
        // https://stackoverflow.com/questions/40489815/checking-size-of-sqs-message-batches
        // 이 것 계산하려면 Jdk Serializer를 통해서 bytes 를 계산해야 한다
        val entries = List(10) {
            SendMessageBatchRequestEntry.builder()
                .id("id$it")
                .messageBody("Hello, world $it")
                .build()
        }
        val sendBatchResponse = sqsClient.sendMessageBatch {
            it.queueUrl(queueUrl)
                .entries(entries)
        }
        sendBatchResponse.successful() shouldHaveSize entries.size
        sendBatchResponse.successful().forEach {
            log.debug { "result entry=$it" }
        }
    }

    @Test
    @Order(5)
    fun `receive messages`() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(queueUrl).maxNumberOfMessages(3)
        }.messages()

        messages shouldHaveSize 3
    }

    @Test
    @Order(6)
    fun `change messages`() {
        val messages = sqsClient.receiveMessage {
            it.queueUrl(queueUrl).maxNumberOfMessages(3)
        }.messages()

        val responses = messages.map { message ->
            sqsClient.changeMessageVisibility {
                it.queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .visibilityTimeout(100)
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
            sqsClient.deleteMessage {
                it.queueUrl(queueUrl).receiptHandle(message.receiptHandle())
            }
        }
        responses shouldHaveSize messages.size
    }
}
