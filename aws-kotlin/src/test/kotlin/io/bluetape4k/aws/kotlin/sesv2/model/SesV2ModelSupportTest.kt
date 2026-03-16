package io.bluetape4k.aws.kotlin.sesv2.model

import aws.sdk.kotlin.services.sesv2.model.EmailContent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SesV2ModelSupportTest {

    @Test
    fun `destinationOf는 to address를 설정한다`() {
        val destination = destinationOf("a@example.com", "b@example.com")

        destination.toAddresses shouldBeEqualTo listOf("a@example.com", "b@example.com")
    }

    @Test
    fun `destinationOf list 오버로드는 builder를 적용한다`() {
        val destination = destinationOf(
            toAddresses = listOf("to@example.com"),
            ccAddresses = listOf("cc@example.com"),
            bccAddresses = listOf("bcc@example.com"),
        ) {
            this.toAddresses = listOf("to@example.com", "to2@example.com")
        }

        destination.toAddresses shouldBeEqualTo listOf("to@example.com", "to2@example.com")
        destination.ccAddresses shouldBeEqualTo listOf("cc@example.com")
        destination.bccAddresses shouldBeEqualTo listOf("bcc@example.com")
    }

    @Test
    fun `contentOf는 data와 charset을 설정한다`() {
        val content = contentOf("hello")

        content.data shouldBeEqualTo "hello"
        content.charset shouldBeEqualTo "UTF-8"
    }

    @Test
    fun `rawMessageOf는 data를 설정한다`() {
        val raw = rawMessageOf("hello".toByteArray())
        raw.data.decodeToString() shouldBeEqualTo "hello"
    }

    @Test
    fun `sendEmailRequestOf는 필드를 설정한다`() {
        val request = sendEmailRequestOf(
            fromEmailAddress = "from@example.com",
            destination = destinationOf("to@example.com"),
            content = EmailContent {
                simple = messageOf(
                    subject = contentOf("subject"),
                    body = textBodyOf(contentOf("body")),
                )
            },
        )

        request.fromEmailAddress shouldBeEqualTo "from@example.com"
        request.destination?.toAddresses shouldBeEqualTo listOf("to@example.com")
        request.content?.simple?.subject?.data shouldBeEqualTo "subject"
    }

    @Test
    fun `destinationOf는 주소 없으면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            destinationOf()
        }
    }

    @Test
    fun `contentOf는 빈 데이터를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            contentOf("")
        }
    }

    @Test
    fun `rawMessageOf는 빈 데이터를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            rawMessageOf(byteArrayOf())
        }
    }
}
