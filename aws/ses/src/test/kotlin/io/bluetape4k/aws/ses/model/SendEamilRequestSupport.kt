package io.bluetape4k.aws.ses.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SendEamilRequestSupport {

    companion object: KLogging()

    @Test
    fun `build SendEmailRequest`() {
        val request = SendEmailRequest {
            source("source@example.com")
            destination(destinationOf("target@example.com"))

            message { builder ->
                builder.subject(contentOf("제목"))
                builder.body(bodyOf("본문", "<p1>본문</p1>"))
            }
            tags(messageTagOf("name", "value"))
        }

        request.source() shouldBeEqualTo "source@example.com"
        request.destination().toAddresses() shouldBeEqualTo listOf("target@example.com")
        request.message().subject().data() shouldBeEqualTo "제목"
        request.message().body().html().data() shouldBeEqualTo "<p1>본문</p1>"
        request.message().body().text().data() shouldBeEqualTo "본문"

        request.tags() shouldBeEqualTo listOf(messageTagOf("name", "value"))
    }

    @Test
    fun `build SendTemplatedEmailRequest`() {
        val request = SendTemplatedEmailRequest {
            source("source@example.com")
            destination(destinationOf("target@example.com"))

            template("template")
            templateData("json data")

            tags(messageTagOf("name", "value"))
        }

        request.source() shouldBeEqualTo "source@example.com"
        request.destination().toAddresses() shouldBeEqualTo listOf("target@example.com")
        request.template() shouldBeEqualTo "template"
        request.templateData() shouldBeEqualTo "json data"

        request.tags() shouldBeEqualTo listOf(messageTagOf("name", "value"))
    }
}
