package io.bluetape4k.testcontainers.mail

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [MailpitServer] 테스트
 */
class MailpitServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { MailpitServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { MailpitServer(tag = " ") }
    }

    @Test
    fun `Mailpit 서버가 시작되고 실행 중이어야 한다`() {
        MailpitServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `Mailpit 서버 url과 uiUrl이 올바른 형식이어야 한다`() {
        MailpitServer().use { server ->
            server.start()
            server.url.shouldNotBeBlank()    // smtp://host:port
            server.uiUrl.shouldNotBeBlank()  // http://host:port
        }
    }
}
