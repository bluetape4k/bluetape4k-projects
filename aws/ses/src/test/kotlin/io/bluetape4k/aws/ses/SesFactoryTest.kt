package io.bluetape4k.aws.ses

import io.bluetape4k.aws.ses.model.bodyOf
import io.bluetape4k.aws.ses.model.contentOf
import io.bluetape4k.aws.ses.model.destinationOf
import io.bluetape4k.aws.ses.model.sendEmailRequest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class SesFactoryTest: AbstractSesTest() {

    @Test
    fun `SesFactory Sync create는 email을 전송할 수 있다`() {
        val sync = SesFactory.Sync.create(endpoint, region, credentialsProvider)
        sync.verifyEmailAddress { it.emailAddress(senderEmail) }
        sync.verifyEmailAddress { it.emailAddress(receiverEamil) }

        val request = sendEmailRequest {
            source(senderEmail)
            destination(destinationOf(receiverEamil))
            message {
                it.subject(contentOf("factory-sync"))
                it.body(bodyOf("본문", "<p>본문</p>"))
            }
        }

        val response = sync.send(request)
        response.messageId().shouldNotBeEmpty()
    }

    @Test
    fun `SesFactory Async create는 email을 전송할 수 있다`() = runSuspendIO {
        val async = SesFactory.Async.create(endpoint, region, credentialsProvider)
        client.verifyEmailAddress { it.emailAddress(senderEmail) }
        client.verifyEmailAddress { it.emailAddress(receiverEamil) }

        val request = sendEmailRequest {
            source(senderEmail)
            destination(destinationOf(receiverEamil))
            message {
                it.subject(contentOf("factory-async"))
                it.body(bodyOf("본문", "<p>본문</p>"))
            }
        }

        val response = async.send(request)
        response.messageId().shouldNotBeEmpty()
    }
}
