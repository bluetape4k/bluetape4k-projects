package io.bluetape4k.aws.sqs

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.util.concurrent.CompletableFuture

class SqsAsyncClientCoroutinesExtensionsTest {

    @Test
    fun `sendSuspend는 sendMessage 결과를 반환한다`() = runTest {
        val client = mockk<SqsAsyncClient>()
        val requestSlot = slot<SendMessageRequest>()
        val response = SendMessageResponse.builder().messageId("mid-1").build()

        every {
            client.sendMessage(capture(requestSlot))
        } returns CompletableFuture.completedFuture(response)

        val result = client.send(
            queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/000000000000/test-queue",
            messageBody = "hello",
        )

        result.messageId() shouldBeEqualTo "mid-1"
        requestSlot.captured.queueUrl() shouldBeEqualTo "https://sqs.ap-northeast-2.amazonaws.com/000000000000/test-queue"
        requestSlot.captured.messageBody() shouldBeEqualTo "hello"
        verify(exactly = 1) { client.sendMessage(any<SendMessageRequest>()) }
    }
}
