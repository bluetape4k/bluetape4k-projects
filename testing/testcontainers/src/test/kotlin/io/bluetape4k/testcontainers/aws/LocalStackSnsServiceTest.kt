package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.CreateTopicRequest
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest
import software.amazon.awssdk.services.sns.model.ListTopicsRequest
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.SubscribeRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

/**
 * LocalStack을 사용한 AWS SNS 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackSnsServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    private fun buildSnsClient(server: LocalStackServer): SnsClient =
        SnsClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.SNS))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    @Test
    fun `SNS 토픽 생성 후 메시지 발행 및 토픽 삭제`() {
        LocalStackServer().withServices(LocalStackContainer.Service.SNS).use { server ->
            server.start()
            val snsClient = buildSnsClient(server)

            // 토픽 생성
            val topicArn = snsClient.createTopic(
                CreateTopicRequest.builder().name("test-topic").build()
            ).topicArn()
            topicArn.shouldNotBeNull()

            // 메시지 발행
            val publishResponse = snsClient.publish(
                PublishRequest.builder()
                    .topicArn(topicArn)
                    .message("안녕하세요, LocalStack SNS!")
                    .subject("테스트 알림")
                    .build()
            )
            publishResponse.messageId().shouldNotBeNull()

            // 토픽 목록 조회
            val topics = snsClient.listTopics(ListTopicsRequest.builder().build()).topics()
            topics.shouldNotBeEmpty()

            // 토픽 삭제
            snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build())
        }
    }

    @Test
    fun `SNS 토픽에 SQS 큐 구독 연결 후 팬아웃 메시지 수신`() {
        LocalStackServer()
            .withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS)
            .use { server ->
                server.start()

                val region = Region.of(server.region)
                val credentialProvider = server.getCredentialProvider()

                val snsClient = SnsClient.builder()
                    .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.SNS))
                    .region(region)
                    .credentialsProvider(credentialProvider)
                    .build()
                    .apply { ShutdownQueue.register(this) }

                val sqsClient = SqsClient.builder()
                    .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.SQS))
                    .region(region)
                    .credentialsProvider(credentialProvider)
                    .build()
                    .apply { ShutdownQueue.register(this) }

                // SQS 큐 생성 및 ARN 조회
                val queueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder().queueName("fanout-queue").build()
                ).queueUrl()

                val queueArn = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build()
                ).attributes()[QueueAttributeName.QUEUE_ARN]
                queueArn.shouldNotBeNull()

                // SNS 토픽 생성
                val topicArn = snsClient.createTopic(
                    CreateTopicRequest.builder().name("fanout-topic").build()
                ).topicArn()

                // SQS 구독 등록
                val subscriptionArn = snsClient.subscribe(
                    SubscribeRequest.builder()
                        .topicArn(topicArn)
                        .protocol("sqs")
                        .endpoint(queueArn)
                        .attributes(mapOf("RawMessageDelivery" to "true"))
                        .build()
                ).subscriptionArn()
                subscriptionArn.shouldNotBeNull()

                // 구독 목록 확인
                val subscriptions = snsClient.listSubscriptionsByTopic(
                    ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build()
                ).subscriptions()
                subscriptions.size shouldBeGreaterThan 0

                // SNS → SQS 팬아웃 메시지 발행
                snsClient.publish(
                    PublishRequest.builder()
                        .topicArn(topicArn)
                        .message("팬아웃 메시지")
                        .build()
                )

                // SQS 큐에서 팬아웃 메시지 수신 확인
                val messages = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(5)
                        .build()
                ).messages()
                messages.shouldNotBeEmpty()
            }
    }
}
