package io.bluetape4k.retrofit2.clients.hc5

import ch.qos.logback.classic.Level
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class Hc5OkHttp3SupportTest {

    @Test
    fun `toSimpleHttpRequest redacts sensitive headers in trace logs`() {
        val loggerName = "io.bluetape4k.retrofit2.clients.hc5.Hc5OkHttp3Support"
        val logger = LoggerFactory.getLogger(loggerName) as ch.qos.logback.classic.Logger
        val previousLevel = logger.level
        val secretToken = "Bearer hc5-secret"
        val apiKey = "hc5-api-key"

        InMemoryLogbackAppender(loggerName).use { appender ->
            try {
                logger.level = Level.TRACE

                val request =
                    Request
                        .Builder()
                        .url("https://example.com/redaction")
                        .get()
                        .header("Authorization", secretToken)
                        .header("X-Api-Key", apiKey)
                        .header("X-Request-Id", "request-123")
                        .build()

                val simpleRequest = request.toSimpleHttpRequest()

                simpleRequest.getFirstHeader("Authorization").value shouldBeEqualTo secretToken
                simpleRequest.getFirstHeader("X-Api-Key").value shouldBeEqualTo apiKey

                val messages = appender.messages.joinToString("\n")
                messages shouldContain "Authorization=<redacted>"
                messages shouldContain "X-Api-Key=<redacted>"
                messages shouldContain "X-Request-Id=request-123"
                messages shouldNotContain secretToken
                messages shouldNotContain apiKey
            } finally {
                logger.level = previousLevel
            }
        }
    }
}
