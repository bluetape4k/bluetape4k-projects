package io.bluetape4k.aws.kotlin.sesv2.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class SesV2MessageTest {

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
}
