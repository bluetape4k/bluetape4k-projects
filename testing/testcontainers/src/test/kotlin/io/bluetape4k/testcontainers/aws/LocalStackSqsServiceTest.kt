package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * LocalStack을 사용한 AWS SQS 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackSqsServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    private fun buildSqsClient(server: LocalStackServer): SqsClient =
        SqsClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.SQS))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    @Test
    fun `SQS 표준 큐 생성 후 메시지 발행 및 수신`() {
        LocalStackServer().withServices(LocalStackContainer.Service.SQS).use { server ->
            server.start()
            val sqsClient = buildSqsClient(server)

            // 큐 생성
            val queueUrl = sqsClient.createQueue(
                CreateQueueRequest.builder().queueName("test-standard-queue").build()
            ).queueUrl()
            queueUrl.shouldNotBeNull()

            // 메시지 전송
            val messageBody = "Hello, LocalStack SQS!"
            sqsClient.sendMessage(
                SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build()
            )

            // 메시지 수신
            val messages = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(5)
                    .build()
            ).messages()
            messages.shouldNotBeEmpty()
            messages.first().body() shouldBeEqualTo messageBody

            // 메시지 처리 완료 후 삭제
            sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(messages.first().receiptHandle())
                    .build()
            )
        }
    }

    @Test
    fun `SQS FIFO 큐에 순서 보장 메시지 발행 및 수신`() {
        LocalStackServer().withServices(LocalStackContainer.Service.SQS).use { server ->
            server.start()
            val sqsClient = buildSqsClient(server)

            // FIFO 큐 생성 (콘텐츠 기반 중복 제거 활성화)
            val queueUrl = sqsClient.createQueue(
                CreateQueueRequest.builder()
                    .queueName("ordered-queue.fifo")
                    .attributes(
                        mapOf(
                            QueueAttributeName.FIFO_QUEUE to "true",
                            QueueAttributeName.CONTENT_BASED_DEDUPLICATION to "true",
                        )
                    )
                    .build()
            ).queueUrl()
            queueUrl.shouldNotBeNull()

            // 순서대로 메시지 전송
            val payloads = listOf("first", "second", "third")
            payloads.forEach { body ->
                sqsClient.sendMessage(
                    SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(body)
                        .messageGroupId("group-1")
                        .build()
                )
            }

            // 메시지 수신 및 검증
            val received = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .build()
            ).messages()
            received.shouldNotBeEmpty()
            received.first().body() shouldBeEqualTo "first"
        }
    }

    @Test
    fun `SQS 다수 큐 생성 후 목록 조회`() {
        LocalStackServer().withServices(LocalStackContainer.Service.SQS).use { server ->
            server.start()
            val sqsClient = buildSqsClient(server)

            listOf("queue-alpha", "queue-beta", "queue-gamma").forEach { name ->
                sqsClient.createQueue(CreateQueueRequest.builder().queueName(name).build())
            }

            val queueUrls = sqsClient.listQueues(ListQueuesRequest.builder().build()).queueUrls()
            queueUrls.size shouldBeGreaterThan 0
        }
    }
}
