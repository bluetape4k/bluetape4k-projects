package io.bluetape4k.aws.kotlin.sqs

import io.bluetape4k.aws.kotlin.sqs.model.sendMessageBatchRequestEntryOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.assertFailsWith

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SqsClientExtensionsTest: AbstractKotlinSqsTest() {

    companion object: KLoggingChannel() {
        private const val QUEUE_PREFIX = "test-queue"
        private val QUEUE_NAME = "$QUEUE_PREFIX-${Base58.randomString(12).lowercase()}"
    }

    private lateinit var testQueueUrl: String

    @Test
    @Order(1)
    fun `create queue`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.createQueue(QUEUE_NAME)
            log.debug { "Create queue response=$response" }

            testQueueUrl = client.getQueueUrl(QUEUE_NAME) ?: fail("Queue URL not found")

            log.debug { "Queue URL=$testQueueUrl" }
            testQueueUrl shouldBeEqualTo response.queueUrl
        }
    }

    @Test
    @Order(2)
    fun `list queues`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.listQueues(QUEUE_PREFIX)

            response.queueUrls!!.forEach {
                log.debug { "Queue URL=$it" }
            }
            val queueUrls = response.queueUrls!!
            queueUrls shouldHaveSize 1
            queueUrls.first() shouldBeEqualTo testQueueUrl
        }
    }

    @Test
    @Order(3)
    fun `send messages`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val messageBody = randomString()
            val response = client.sendMessage(testQueueUrl, messageBody, 3)

            response.messageId.shouldNotBeNull().shouldNotBeEmpty()
            log.debug { "Send messages response=$response" }
        }
    }

    @Test
    @Order(4)
    fun `send messages in batch mode`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val messageCount = 10
            // NOTE: 배치로 한번에 전송할 메시지의 총 크기가 262,144 바이트(256 KB)를 초과할 수 없습니다.
            val entries = List(messageCount) {
                sendMessageBatchRequestEntryOf(
                    id = "id-$it",
                    messageBody = "Hello, World! $it"
                )
            }

            val response = client.sendMessageBatch(testQueueUrl, entries)
            response.successful shouldHaveSize messageCount
            response.successful.forEach {
                log.debug { "result=$it" }
            }
        }
    }

    @Test
    @Order(5)
    fun `receive messages`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val messages = client.receiveMessage(testQueueUrl, 3).messages!!

            messages shouldHaveSize 3
            messages.forEach {
                log.debug { "message=$it" }
            }
        }
    }

    @Test
    @Order(6)
    fun `change message visibility`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val messages = client.receiveMessage(testQueueUrl, 3).messages!!

            val responses = messages.map { msg ->
                async {
                    log.debug { "Change visibility of message=$msg" }
                    client.changeMessageVisibility(testQueueUrl, msg.receiptHandle, 10)
                }
            }.awaitAll()

            responses shouldHaveSize messages.size
            responses.forEach { response ->
                log.debug { "response metadata=$response" }
            }
        }
    }

    @Test
    @Order(7)
    fun `delete messages`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val messages = client.receiveMessage(testQueueUrl, 3).messages!!

            val responses = messages.map { msg ->
                async {
                    client.deleteMessage(testQueueUrl, msg.receiptHandle)
                }
            }.awaitAll()

            responses shouldHaveSize messages.size
            responses.forEach {
                log.debug { "response=$it" }
            }
        }
    }

    @Test
    @Order(8)
    fun `delete queue`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val response = client.deleteQueue(testQueueUrl)
            log.debug { "Delete queue response=$response" }

            client.existsQueue(QUEUE_NAME).shouldBeFalse()
        }
    }

    @Test
    @Order(9)
    fun `receiveMessage는 maxNumberOfMessages 범위를 검증한다`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val queueUrl = "https://example.com/queue/demo"

            assertFailsWith<IllegalArgumentException> {
                client.receiveMessage(queueUrl, maxNumberOfMessages = 0)
            }
            assertFailsWith<IllegalArgumentException> {
                client.receiveMessage(queueUrl, maxNumberOfMessages = 11)
            }
        }
    }

    @Test
    @Order(10)
    fun `sendMessage는 blank messageBody를 허용하지 않는다`() = runSuspendIO {
        withSqsClient(
            localStackServer.endpointUrl,
            localStackServer.region,
            localStackServer.credentialsProvider,
        ) { client ->
            val queueUrl = "https://example.com/queue/demo"

            assertFailsWith<IllegalArgumentException> {
                client.sendMessage(queueUrl, "   ")
            }
        }
    }
}
