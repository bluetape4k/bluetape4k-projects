package io.bluetape4k.aws.sns

import io.bluetape4k.concurrent.completableFutureOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse
import java.util.function.Consumer

class SnsAsyncClientCoroutinesExtensionsTest {

    @Test
    fun `createPlatformEndpoint는 createPlatformEndpoint 결과를 반환한다`() = runTest {
        val client = mockk<SnsAsyncClient>()
        lateinit var capturedConsumer: Consumer<CreatePlatformEndpointRequest.Builder>
        val response = CreatePlatformEndpointResponse.builder()
            .endpointArn("arn:aws:sns:ap-northeast-2:000000000000:endpoint/test")
            .build()

        every {
            client.createPlatformEndpoint(any<Consumer<CreatePlatformEndpointRequest.Builder>>())
        } answers {
            capturedConsumer = firstArg()
            completableFutureOf(response)
        }

        val result = client.createPlatformEndpoint(
            token = "token-1",
            platformApplicationArn = "arn:aws:sns:ap-northeast-2:000000000000:app/test",
        )

        val requestBuilder = CreatePlatformEndpointRequest.builder()
        capturedConsumer.accept(requestBuilder)
        val capturedRequest = requestBuilder.build()

        result.endpointArn() shouldBeEqualTo "arn:aws:sns:ap-northeast-2:000000000000:endpoint/test"
        capturedRequest.token() shouldBeEqualTo "token-1"
        capturedRequest.platformApplicationArn() shouldBeEqualTo "arn:aws:sns:ap-northeast-2:000000000000:app/test"

        verify(exactly = 1) { client.createPlatformEndpoint(any<Consumer<CreatePlatformEndpointRequest.Builder>>()) }
    }
}
