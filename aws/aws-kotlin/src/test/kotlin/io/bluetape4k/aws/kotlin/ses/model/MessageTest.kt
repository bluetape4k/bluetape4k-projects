package io.bluetape4k.aws.kotlin.ses.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class MessageTest {

    companion object : KLogging()

    @Test
    fun `contentOf는 data와 charset으로 Content를 생성한다`() {
        val content = contentOf("Hello, World!")

        content.data shouldBeEqualTo "Hello, World!"
        content.charset shouldBeEqualTo "UTF-8"
    }

    @Test
    fun `contentOf는 커스텀 charset을 설정할 수 있다`() {
        val content = contentOf("테스트 메시지", charset = "EUC-KR")

        content.charset shouldBeEqualTo "EUC-KR"
    }

    @Test
    fun `contentOf는 빈 data를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            contentOf("")
        }
    }

    @Test
    fun `htmlBodyOf는 HTML Content로 Body를 생성한다`() {
        val html = contentOf("<h1>Hello</h1>")
        val body = htmlBodyOf(html)

        body.html.shouldNotBeNull()
        body.html!!.data shouldBeEqualTo "<h1>Hello</h1>"
    }

    @Test
    fun `textBodyOf는 텍스트 Content로 Body를 생성한다`() {
        val text = contentOf("Hello, World!")
        val body = textBodyOf(text)

        body.text.shouldNotBeNull()
        body.text!!.data shouldBeEqualTo "Hello, World!"
    }

    @Test
    fun `messageOf는 subject와 body로 Message를 생성한다`() {
        val subject = contentOf("Test Subject")
        val body = textBodyOf(contentOf("Test body"))
        val message = messageOf(subject, body)

        message.subject.shouldNotBeNull()
        message.subject!!.data shouldBeEqualTo "Test Subject"
        message.body.shouldNotBeNull()
    }

    @Test
    fun `sendEmailRequestOf는 source, destination, message로 요청을 생성한다`() {
        val dest = destinationOf("to@example.com")
        val subject = contentOf("Hello")
        val body = textBodyOf(contentOf("Hello, World!"))
        val msg = messageOf(subject, body)

        val req = sendEmailRequestOf(
            source = "from@example.com",
            destination = dest,
            message = msg
        )

        req.source shouldBeEqualTo "from@example.com"
        req.destination.shouldNotBeNull()
        req.message.shouldNotBeNull()
    }

    @Test
    fun `sendEmailRequestOf는 빈 source를 허용하지 않는다`() {
        val dest = destinationOf("to@example.com")
        val msg = messageOf(contentOf("Subject"), textBodyOf(contentOf("Body")))

        assertFailsWith<IllegalArgumentException> {
            sendEmailRequestOf(source = "", destination = dest, message = msg)
        }
    }
}
