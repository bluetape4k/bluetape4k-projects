package io.bluetape4k.junit5.output

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class InMemoryLogbackAppenderTest {

    companion object: KLogging() {
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

    @Test
    fun `local class로도 appender를 생성할 수 있다`() {
        class LocalClass

        val localAppender = InMemoryLogbackAppender(LocalClass::class)
        try {
            assertTrue(localAppender.isStarted)
        } finally {
            localAppender.stop()
        }
    }

    @Test
    fun `다중 스레드에서 동시 로깅해도 예외가 발생하지 않는다`() {
        val threadCount = 8
        val messagesPerThread = 100
        val latch = java.util.concurrent.CountDownLatch(threadCount)

        val threads = List(threadCount) { threadIdx ->
            Thread.ofVirtual().start {
                repeat(messagesPerThread) { msgIdx ->
                    log.debug { "thread-$threadIdx msg-$msgIdx" }
                }
                latch.countDown()
            }
        }

        latch.await(5, java.util.concurrent.TimeUnit.SECONDS).shouldBeTrue()
        threads.forEach { it.join(1000) }

        // CopyOnWriteArrayList 이므로 동시 접근에 안전해야 함
        appender.size shouldBeEqualTo threadCount * messagesPerThread
    }

    @Test
    fun `stop 호출 후 isStarted 는 false 이다`() {
        appender.stop()
        assertTrue(!appender.isStarted)
        appender.size shouldBeEqualTo 0
    }
}
