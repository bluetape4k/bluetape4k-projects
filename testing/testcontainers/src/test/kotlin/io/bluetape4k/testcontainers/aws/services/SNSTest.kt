package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SNSTest: AbstractContainerTest() {

    companion object: KLogging() {
        private val TOPIC_NAME = "test-topic-${System.currentTimeMillis()}"
    }

    private val snsServer: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.SNS)
    }
    private val endpoint: URI get() = snsServer.getEndpointOverride(LocalStackContainer.Service.SNS)

    private val snsClient: SnsClient by lazy {
        SnsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(snsServer.region))
            .credentialsProvider(snsServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private lateinit var topicArn: String
    private lateinit var subscriptionArn: String

    @BeforeAll
    fun setup() {
        snsServer.start()
    }

    @Test
    @Order(1)
    fun `create topic`() {
        val response = snsClient.createTopic { it.name(TOPIC_NAME) }
        topicArn = response.topicArn()
        log.debug { "Created topic ARN: $topicArn" }
        topicArn.shouldNotBeNull()
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
                .message("Hello from SNS LocalStack!")
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
        subscriptionArn = response.subscriptionArn()
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
                .attributeValue("Bluetape4k Test Topic")
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
