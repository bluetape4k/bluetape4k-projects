package io.bluetape4k.aws.sns

import io.bluetape4k.aws.sns.model.SubscribeRequest
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.hashOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import software.amazon.awssdk.services.sns.model.SubscribeResponse
import java.util.*

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SnsClientTest: AbstractSnsTest() {

    companion object: KLogging() {
        private val TOPIC_NAME = UUID.randomUUID().encodeBase62().lowercase() + ".fifo"
    }

    private lateinit var topicArn: String
    private lateinit var subscriptionArn: String
    private lateinit var token: String

    val phoneNumber = "+821089555081"

    @Test
    @Order(1)
    fun `create topic`() {
        val response = client.createFIFOTopic(TOPIC_NAME)

        topicArn = response.topicArn()
        topicArn.shouldNotBeEmpty()
        log.debug { "topic name=$TOPIC_NAME, topicArn=$topicArn" }
    }

    @Test
    @Order(2)
    fun `subscribe topic`() {
        val request = SubscribeRequest {
            protocol("sms")
            endpoint(phoneNumber)
            returnSubscriptionArn(true)
            topicArn(topicArn)
        }

        val response: SubscribeResponse = client.subscribe(request)

        subscriptionArn = response.subscriptionArn()
        subscriptionArn.shouldNotBeEmpty()
        log.debug { "subscriptionArn=$subscriptionArn" }

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        response.responseMetadata()
    }

    @Disabled("token은 SMS 구독 시에 클라이언트에 전송된다고 한다")
    @Test
    @Order(3)
    fun `confirm subscription`() {
        val response = client.confirmSubscription {
            it.token("EXAMPLE-TOKEN")
            it.topicArn(topicArn)
        }

        log.debug { "Subscription confirmed: ${response.sdkHttpResponse().statusCode()}" }
        log.debug { "SubscriptionArn: ${response.subscriptionArn()}" }
    }

    @Test
    @Order(4)
    fun `list subscriptions`() {
        val response = client.listSubscriptions {}
        response.subscriptions().forEach {
            log.debug { "subscriptionArn=${it.subscriptionArn()}" }
        }
    }

    @Test
    @Order(5)
    fun `check opt out`() {
        val result = client.checkIfPhoneNumberIsOptedOut {
            it.phoneNumber(phoneNumber)
        }
        log.debug { "${result.isOptedOut} $phoneNumber has opted out of receiving sns" }
        result.sdkHttpResponse().statusCode() shouldBeEqualTo 200
    }

    @Test
    @Order(6)
    fun `send message`() {
        val response = client.publish {
            val message = "Hello, World!"
            it.subject("[TEST]")
            it.message(message)
            it.topicArn(topicArn)
            it.messageGroupId("partitionKey")
            it.messageDeduplicationId(hashOf(topicArn, message).toString())
        }

        response.messageId().shouldNotBeEmpty()
        log.debug { "response=$response" }
    }
}
