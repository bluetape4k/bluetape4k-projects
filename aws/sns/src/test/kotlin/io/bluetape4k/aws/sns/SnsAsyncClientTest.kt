package io.bluetape4k.aws.sns

import io.bluetape4k.aws.sns.model.SubscribeRequest
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.hashOf
import kotlinx.coroutines.future.await
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

@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SnsAsyncClientTest: AbstractSnsTest() {

    companion object: KLoggingChannel() {
        private val TOPIC_NAME = Base58.randomString(8).lowercase() + ".fifo"
    }

    private lateinit var topicArn: String
    private lateinit var subscriptionArn: String
    private lateinit var token: String

    val phoneNumber = "+821088885555"

    @Test
    @Order(1)
    fun `create topic`() = runSuspendIO {
        val response = asyncClient.createFIFOTopic(TOPIC_NAME).await()

        topicArn = response.topicArn()
        topicArn.shouldNotBeEmpty()
        log.debug { "topic name=$TOPIC_NAME, topicArn=$topicArn" }
    }

    @Test
    @Order(2)
    fun `subscribe topic`() = runSuspendIO {
        val request = SubscribeRequest {
            protocol("sms")
            endpoint(phoneNumber)
            returnSubscriptionArn(true)
            topicArn(topicArn)
        }

        val response: SubscribeResponse = asyncClient.subscribe(request).await()

        subscriptionArn = response.subscriptionArn()
        subscriptionArn.shouldNotBeEmpty()
        log.debug { "subscriptionArn=$subscriptionArn" }

        response.sdkHttpResponse().statusCode() shouldBeEqualTo 200
        response.responseMetadata()
    }

    @Disabled("token은 SMS 구독 시에 클라이언트에 전송된다고 한다")
    @Test
    @Order(3)
    fun `confirm subscription`() = runSuspendIO {
        val response = asyncClient.confirmSubscription {
            it.token("EXAMPLE-TOKEN")
            it.topicArn(topicArn)
        }.await()

        log.debug { "Subscription confirmed: ${response.sdkHttpResponse().statusCode()}" }
        log.debug { "SubscriptionArn: ${response.subscriptionArn()}" }
    }

    @Test
    @Order(4)
    fun `list subscriptions`() = runSuspendIO {
        val response = asyncClient.listSubscriptions {}.await()
        response.subscriptions().forEach {
            log.debug { "subscriptionArn=${it.subscriptionArn()}" }
        }
    }

    @Test
    @Order(5)
    fun `check opt out`() = runSuspendIO {
        val result = asyncClient.checkIfPhoneNumberIsOptedOut {
            it.phoneNumber(phoneNumber)
        }.await()

        log.debug { "${result.isOptedOut} $phoneNumber has opted out of receiving sns" }
        result.sdkHttpResponse().statusCode() shouldBeEqualTo 200
    }

    @Test
    @Order(6)
    fun `send message`() = runSuspendIO {
        val response = asyncClient.publish {
            val message = "Hello, World!"
            it.subject("[TEST]")
            it.message(message)
            it.topicArn(topicArn)
            it.messageGroupId("partitionKey")
            it.messageDeduplicationId(hashOf(topicArn, message).toString())
        }.await()

        response.messageId().shouldNotBeEmpty()
        log.debug { "response=$response" }
    }
}
