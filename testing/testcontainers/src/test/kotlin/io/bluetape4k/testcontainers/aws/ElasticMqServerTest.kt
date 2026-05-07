package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * [ElasticMqServer] 테스트 - Docker 없이 JVM 내에서 실행
 */
class ElasticMqServerTest {

    companion object: KLogging()

    private lateinit var server: ElasticMqServer

    @BeforeEach
    fun setup() {
        server = ElasticMqServer(port = 19324)  // 기본 포트 충돌 방지
        server.start()
    }

    @AfterEach
    fun teardown() {
        if (server.isRunning) {
            server.stop()
        }
    }

    @Test
    fun `서버가 시작되고 실행 중이어야 한다`() {
        server.isRunning.shouldBeTrue()
        server.endpoint.toString().shouldNotBeBlank()
    }

    @Test
    fun `서버를 중지하면 isRunning이 false가 되어야 한다`() {
        server.stop()
        server.isRunning.shouldBeFalse()
    }

    @Test
    fun `SQS 큐를 생성하고 메시지를 전송할 수 있어야 한다`() {
        val sqsClient = SqsClient.builder()
            .endpointOverride(server.endpoint)
            .region(Region.of(server.regionName))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(server.accessKey, server.secretKey)
                )
            )
            .build()

        val queueUrl = sqsClient.createQueue(
            CreateQueueRequest.builder().queueName("test-queue").build()
        ).queueUrl()

        queueUrl.shouldNotBeBlank()

        sqsClient.sendMessage(
            SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("test-message")
                .build()
        )
    }
}
