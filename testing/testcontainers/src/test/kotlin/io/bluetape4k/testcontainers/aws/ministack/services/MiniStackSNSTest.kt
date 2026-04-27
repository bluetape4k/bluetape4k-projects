package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

/**
 * MiniStack SNS 서비스 통합 테스트.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackSNSTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val TOPIC_NAME = "ministack-test-topic-${System.currentTimeMillis()}"
    }

    private val miniStack: MiniStackServer by lazy { MiniStackServer.Launcher.miniStack }

    private val snsClient: SnsClient by lazy {
        SnsClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private lateinit var topicArn: String

    @Test
    @Order(1)
    fun `create topic`() {
        val response = snsClient.createTopic { it.name(TOPIC_NAME) }
        topicArn = requireNotNull(response.topicArn()) {
            "createTopic response returned a null topicArn — MiniStack createTopic failed"
        }
        log.debug { "Created topic ARN: $topicArn" }
    }

    @Test
    @Order(2)
    fun `list topics`() {
        val topics = snsClient.listTopics().topics()
        log.debug { "Topics: ${topics.map { it.topicArn() }}" }
        topics.shouldNotBeEmpty()
    }

    @Test
    @Order(3)
    fun `get topic attributes`() {
        val attrs = snsClient.getTopicAttributes { it.topicArn(topicArn) }.attributes()
        log.debug { "Topic attributes: $attrs" }
        attrs.shouldNotBeNull()
    }

    @Test
    @Order(4)
    fun `publish message`() {
        val response = snsClient.publish {
            it.topicArn(topicArn)
                .subject("Test Subject")
                .message("Hello from MiniStack SNS!")
        }
        log.debug { "Published MessageId: ${response.messageId()}" }
        response.messageId().shouldNotBeNull()
    }

    @Test
    @Order(5)
    fun `subscribe with email protocol`() {
        val response = snsClient.subscribe {
            it.topicArn(topicArn)
                .protocol("email")
                .endpoint("test@example.com")
        }
        log.debug { "Subscription ARN: ${response.subscriptionArn()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    fun `list subscriptions`() {
        val subscriptions = snsClient.listSubscriptions().subscriptions()
        log.debug { "Subscriptions: ${subscriptions.map { it.subscriptionArn() }}" }
        subscriptions.shouldNotBeEmpty()
    }

    @Test
    @Order(7)
    fun `set topic attributes`() {
        val response = snsClient.setTopicAttributes {
            it.topicArn(topicArn)
                .attributeName("DisplayName")
                .attributeValue("MiniStack Test Topic")
        }
        log.debug { "SetTopicAttributes HTTP status: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(8)
    fun `delete topic`() {
        val response = snsClient.deleteTopic { it.topicArn(topicArn) }
        log.debug { "DeleteTopic HTTP status: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }
}
