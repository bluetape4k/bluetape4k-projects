package io.bluetape4k.aws.ses.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MessageSupportTest {

    companion object: KLogging()

    @Test
    fun `build email message`() {
        val message = Message {
            subject(contentOf("제목"))
            body(
                bodyOf(
                    text = "본문",
                    html = "<p1>본문</p1>"
                )
            )
        }
        message.subject().data() shouldBeEqualTo "제목"
        message.body().html().data() shouldBeEqualTo "<p1>본문</p1>"
        message.body().text().data() shouldBeEqualTo "본문"
    }
}
