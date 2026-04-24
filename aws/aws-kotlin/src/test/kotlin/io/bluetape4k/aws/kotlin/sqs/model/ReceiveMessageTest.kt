package io.bluetape4k.aws.kotlin.sqs.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ReceiveMessageTest {

    companion object : KLogging()

    private val queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/MyQueue"

    @Test
    fun `receiveMessageRequestOf는 queueUrl로 요청을 생성한다`() {
        val req = receiveMessageRequestOf(queueUrl = queueUrl)

        req.queueUrl shouldBeEqualTo queueUrl
        req.maxNumberOfMessages shouldBeEqualTo 3
        req.waitTimeSeconds shouldBeEqualTo 20
    }

    @Test
    fun `receiveMessageRequestOf는 maxNumberOfMessages를 설정할 수 있다`() {
        val req = receiveMessageRequestOf(queueUrl = queueUrl, maxNumberOfMessages = 5)

        req.maxNumberOfMessages shouldBeEqualTo 5
    }

    @Test
    fun `receiveMessageRequestOf는 waitTimeSeconds를 설정할 수 있다`() {
        val req = receiveMessageRequestOf(queueUrl = queueUrl, waitTimeSeconds = 10)

        req.waitTimeSeconds shouldBeEqualTo 10
    }

    @Test
    fun `receiveMessageRequestOf는 visibilityTimeout을 설정할 수 있다`() {
        val req = receiveMessageRequestOf(queueUrl = queueUrl, visibilityTimeout = 30)

        req.visibilityTimeout shouldBeEqualTo 30
    }

    @Test
    fun `receiveMessageRequestOf는 attributeNames를 설정할 수 있다`() {
        val req = receiveMessageRequestOf(
            queueUrl = queueUrl,
            attributeNames = listOf("All")
        )

        req.messageAttributeNames.shouldNotBeNull()
        req.messageAttributeNames!! shouldBeEqualTo listOf("All")
    }

    @Test
    fun `receiveMessageRequestOf는 빈 queueUrl을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl = "")
        }
    }

    @Test
    fun `receiveMessageRequestOf는 maxNumberOfMessages 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl = queueUrl, maxNumberOfMessages = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl = queueUrl, maxNumberOfMessages = 11)
        }
    }

    @Test
    fun `receiveMessageRequestOf는 waitTimeSeconds 범위를 검증한다`() {
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl = queueUrl, waitTimeSeconds = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            receiveMessageRequestOf(queueUrl = queueUrl, waitTimeSeconds = 21)
        }
    }
}
