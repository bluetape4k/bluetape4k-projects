package io.bluetape4k.aws.ses

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import java.util.concurrent.CompletableFuture

class SesAsyncClientCoroutinesExtensionsTest {

    @Test
    fun `sendSuspend는 sendEmail 결과를 반환한다`() = runTest {
        val client = mockk<SesAsyncClient>()
        val request = SendEmailRequest.builder().build()
        val response = SendEmailResponse.builder().messageId("mid-1").build()

        every { client.sendEmail(request) } returns CompletableFuture.completedFuture(response)

        val result = client.send(request)

        result.messageId() shouldBeEqualTo "mid-1"
        verify(exactly = 1) { client.sendEmail(request) }
    }
}
