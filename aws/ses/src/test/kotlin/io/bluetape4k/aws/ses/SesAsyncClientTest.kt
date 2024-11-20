package io.bluetape4k.aws.ses

import io.bluetape4k.aws.ses.model.SendEmailRequest
import io.bluetape4k.aws.ses.model.bodyOf
import io.bluetape4k.aws.ses.model.contentOf
import io.bluetape4k.aws.ses.model.destinationOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SesAsyncClientTest: AbstractSesTest() {

    companion object: KLogging()

    @Test
    fun `send email asynchronously`() = runTest {
        client.verifyEmailAddress { it.emailAddress(senderEmail) }
        client.verifyEmailAddress { it.emailAddress(receiverEamil) }

        val request = SendEmailRequest {
            source(senderEmail)
            destination(destinationOf(receiverEamil))
            message { mb ->
                mb.subject(contentOf("제목"))
                mb.body(bodyOf("본문", "<p1>본문</p1>"))
            }
        }


        val response = asyncClient.send(request).await()
        response.messageId().shouldNotBeEmpty()
        log.debug { "response=$response" }
    }
}
