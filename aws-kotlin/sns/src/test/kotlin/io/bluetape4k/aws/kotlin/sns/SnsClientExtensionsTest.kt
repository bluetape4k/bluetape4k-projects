package io.bluetape4k.aws.kotlin.sns

import aws.sdk.kotlin.services.sns.confirmSubscription
import aws.sdk.kotlin.services.sns.listSubscriptions
import io.bluetape4k.aws.kotlin.sns.model.publishBatchRequestEntryOf
import io.bluetape4k.aws.kotlin.sns.model.publishRequestOf
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.hashOf
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SnsClientExtensionsTest: AbstractKotlinSnsTest() {

    companion object: KLogging() {
        // FIFO Topic을 사용하려면 `.fifo` 를 접미사로 붙여야 합니다.
        private val TOPIC_NAME = "sns-topic-${Base58.randomString(6).lowercase()}"
        private val TOPIC_NAME_FIFO = "$TOPIC_NAME.fifo"
    }

    private lateinit var testTopicArn: String
    private lateinit var testSubscriptionArn: String
    private lateinit var testToken: String

    // Debop's phone number
    private val testPhoneNumber = "+821089555081"

    @Test
    @Order(1)
    fun `create FIFO topic`() = runSuspendIO {
        val response = snsClient.createFifoTopic(TOPIC_NAME_FIFO)

        response.topicArn.shouldNotBeNull().shouldNotBeEmpty()
        testTopicArn = response.topicArn!!
        log.debug { "topic name=$TOPIC_NAME_FIFO, topicArn=$testTopicArn" }
    }

    @Test
    @Order(2)
    fun `subscribe topic`() = runSuspendIO {
        val response = snsClient.subscribe(testTopicArn, testPhoneNumber, "sms")

        response.subscriptionArn.shouldNotBeNull().shouldNotBeEmpty()
        testSubscriptionArn = response.subscriptionArn!!
        log.debug { "subscriptionArn=$testSubscriptionArn" }
    }

    @Disabled("token은 SNS 구독 시에 클라이언트에 전송된다")
    @Test
    @Order(3)
    fun `confirm subscription`() = runSuspendIO {
        val response = snsClient.confirmSubscription {
            token = testToken
            topicArn = testTopicArn
        }

        response.subscriptionArn.shouldNotBeNull().shouldNotBeEmpty()
        log.debug { "subscriptionArn=${response.subscriptionArn}" }
    }

    @Test
    @Order(4)
    fun `list subscriptions`() = runSuspendIO {
        val response = snsClient.listSubscriptions { }

        response.subscriptions?.forEach { subscription ->
            log.debug { "subscriptionArn=${subscription.subscriptionArn}" }
        }
        response.subscriptions.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    @Order(5)
    fun `check opt out status for phone number`() = runSuspendIO {
        val response = snsClient.checkIfPhoneNumberIsOptedOut(testPhoneNumber)
        log.debug { "OptOut status=${response.isOptedOut}" }
        response.isOptedOut.shouldBeFalse()
    }

    @Test
    @Order(6)
    fun `publish messages`() = runSuspendIO {
        val request = publishRequestOf(
            topicArn = testTopicArn,
            phoneNumber = testPhoneNumber,
            message = "Hello, AWS SNS!",
            subject = "[Test]",
            messageGroupId = "partitionKey",
            messageDeduplicationId = hashOf(testTopicArn, "Hello, AWS SNS!", testPhoneNumber).toString()
        )
        val response = snsClient.publish(request)

        log.debug { "response=$response" }
        response.messageId.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    @Order(7)
    fun `publish messages in batch`() = runSuspendIO {
        val messageSize = 10
        val entries = List(messageSize) {
            publishBatchRequestEntryOf(
                id = Base58.randomString(6).lowercase(),
                message = "Hello, AWS SNS! ${Base58.randomString(6).lowercase()}",
                messageDeduplicationId = hashOf(testTopicArn, "Hello, AWS SNS!", testPhoneNumber).toString(),
                messageGroupId = "partitionKey"
            )
        }

        val response = snsClient.publishBatch(testTopicArn, entries)

        response.successful?.forEach { result ->
            result.messageId.shouldNotBeNull().shouldNotBeEmpty()
            log.debug { "result=$result" }
        }
        response.successful!! shouldHaveSize messageSize
    }

    @Test
    @Order(8)
    fun `unsubscribe topic`() = runSuspendIO {
        val response = snsClient.unsubscribe(testSubscriptionArn)
        log.debug { "response=$response" }
    }

    @Test
    @Order(9)
    fun `delete topic`() = runSuspendIO {
        val response = snsClient.deleteTopic(testTopicArn)
        log.debug { "response=$response" }
    }
}
