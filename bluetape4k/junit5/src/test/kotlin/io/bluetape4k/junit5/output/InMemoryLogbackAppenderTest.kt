package io.bluetape4k.junit5.output

import io.github.oshai.kotlinlogging.KotlinLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest

class InMemoryLogbackAppenderTest {

    companion object {
        private val log = KotlinLogging.logger { }
        private const val REPEAT_SIZE = 3
    }

    private lateinit var appender: InMemoryLogbackAppender

    @BeforeEach
    fun beforeEach() {
        appender = InMemoryLogbackAppender(InMemoryLogbackAppenderTest::class)
    }

    @AfterEach
    fun aferEach() {
        if (this::appender.isInitialized) {
            appender.stop()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `capture logback log messages`() {
        val firstMessage = "First message - ${System.currentTimeMillis()}"
        log.debug { firstMessage }
        appender.lastMessage shouldBeEqualTo firstMessage
        appender.size shouldBeEqualTo 1

        val secondMessage = "Second message - ${System.currentTimeMillis()}"
        log.debug { secondMessage }
        appender.lastMessage shouldBeEqualTo secondMessage
        appender.size shouldBeEqualTo 2

        appender.clear()

        appender.size shouldBeEqualTo 0
        appender.lastMessage.shouldBeNull()
        appender.messages.shouldBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `capture logback log messages with info level`() {
        appender.messages.shouldBeEmpty()

        val message = "Information - ${System.currentTimeMillis()}"
        log.info { message }

        appender.messages shouldHaveSize 1
        appender.lastMessage shouldBeEqualTo message
    }
}
