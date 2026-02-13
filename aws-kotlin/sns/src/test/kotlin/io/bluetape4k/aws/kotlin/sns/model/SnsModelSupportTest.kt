package io.bluetape4k.aws.kotlin.sns.model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SnsModelSupportTest {

    @Test
    fun `createTopicRequestOf는 name과 attributes를 설정한다`() {
        val request = createTopicRequestOf(
            name = "topic-name",
            attributes = mapOf("DisplayName" to "display-name"),
        )

        request.name shouldBeEqualTo "topic-name"
        request.attributes shouldBeEqualTo mapOf("DisplayName" to "display-name")
    }

    @Test
    fun `subscribeRequestOf는 topicArn endpoint protocol을 설정한다`() {
        val request = subscribeRequestOf(
            topicArn = "arn:aws:sns:ap-northeast-2:123456789012:topic",
            endpoint = "+821012345678",
            protocol = "sms",
        )

        request.topicArn shouldBeEqualTo "arn:aws:sns:ap-northeast-2:123456789012:topic"
        request.endpoint shouldBeEqualTo "+821012345678"
        request.protocol shouldBeEqualTo "sms"
    }

    @Test
    fun `publishRequestOf는 필드를 설정한다`() {
        val request = publishRequestOf(
            topicArn = "arn:aws:sns:ap-northeast-2:123456789012:topic",
            phoneNumber = "+821012345678",
            message = "hello",
            subject = "subject",
        )

        request.topicArn shouldBeEqualTo "arn:aws:sns:ap-northeast-2:123456789012:topic"
        request.phoneNumber shouldBeEqualTo "+821012345678"
        request.message shouldBeEqualTo "hello"
        request.subject shouldBeEqualTo "subject"
    }

    @Test
    fun `messageAttributeValueOf string은 값을 설정한다`() {
        val value = messageAttributeValueOf("hello")

        value.stringValue shouldBeEqualTo "hello"
        value.dataType shouldBeEqualTo "String"
    }

    @Test
    fun `messageAttributeValueOf binary는 값을 설정한다`() {
        val value = messageAttributeValueOf("hello".toByteArray())

        value.binaryValue!!.decodeToString() shouldBeEqualTo "hello"
        value.dataType shouldBeEqualTo "Binary"
    }

    @Test
    fun `messageAttributeValueOf number는 값을 설정한다`() {
        val value = messageAttributeValueOf(10)

        value.stringValue shouldBeEqualTo "10"
        value.dataType shouldBeEqualTo "Number"
    }

    @Test
    fun `listSubscriptionsRequestOf는 nextToken을 설정한다`() {
        val request = listSubscriptionsRequestOf("token")

        request.nextToken shouldBeEqualTo "token"
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated listSubscriptinosRequestOf는 동일하게 동작한다`() {
        val request = listSubscriptinosRequestOf("token")

        request.nextToken shouldBeEqualTo "token"
    }

    @Test
    fun `messageAttributeValueOf string은 빈 값을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            messageAttributeValueOf("")
        }
    }

    @Test
    fun `messageAttributeValueOf binary는 빈 값을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            messageAttributeValueOf(byteArrayOf())
        }
    }

    @Test
    fun `listSubscriptionsRequestOf는 빈 nextToken을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            listSubscriptionsRequestOf("")
        }
    }
}
