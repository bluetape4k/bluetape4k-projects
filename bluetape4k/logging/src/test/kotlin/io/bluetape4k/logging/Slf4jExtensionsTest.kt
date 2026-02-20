package io.bluetape4k.logging

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class Slf4jExtensionsTest {

    @Test
    fun `logMessageSafe는 메시지를 안전하게 생성한다`() {
        val message = logMessageSafe { "hello" }
        message shouldBeEqualTo "hello"
    }

    @Test
    fun `logMessageSafe는 예외가 발생해도 fallback 메시지를 반환한다`() {
        val message = logMessageSafe(
            fallbackMessage = "fallback",
            msg = { error("boom") }
        )

        message.startsWith("fallback:") shouldBeEqualTo true
    }
}
