package io.bluetape4k.aws.ses.examples

import io.bluetape4k.aws.ses.AbstractSesTest
import io.bluetape4k.aws.ses.model.SendEmailRequest
import io.bluetape4k.aws.ses.model.bodyOf
import io.bluetape4k.aws.ses.model.contentOf
import io.bluetape4k.aws.ses.model.destinationOf
import io.bluetape4k.aws.ses.send
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SesClientExamples: AbstractSesTest() {

    companion object: KLogging()

    @Test
    @Order(1)
    fun `get identity verification attributes`() {
        val response = client.getIdentityVerificationAttributes {
            it.identities(domain, senderEmail)
        }
        val attrs = response.verificationAttributes()
        attrs.forEach { (key, attr) ->
            log.debug { "key=$key, attr=$attr" }
        }
    }

    @Test
    @Order(2)
    fun `list identifiers`() {
        val response = client.listIdentities()
        val identities = response.identities()
        identities.forEach {
            log.debug { "Identity=$it" }
        }
    }

    @Test
    @Order(3)
    fun `verify email identity`() {
        client.verifyEmailIdentity { it.emailAddress(senderEmail) }
        client.verifyEmailIdentity { it.emailAddress(receiverEamil) }
    }

    @Test
    @Order(3)
    fun `send email`() {
        val emailRequest = SendEmailRequest {
            source(senderEmail)
            destination(destinationOf(receiverEamil))
            message { mb ->
                mb.subject(contentOf("제목"))
                mb.body(bodyOf("본문", "<p1>본문</p1>"))
            }
        }

        val response = client.send(emailRequest)
        log.debug { "response=$response" }
        response.messageId().shouldNotBeEmpty()
    }
}
