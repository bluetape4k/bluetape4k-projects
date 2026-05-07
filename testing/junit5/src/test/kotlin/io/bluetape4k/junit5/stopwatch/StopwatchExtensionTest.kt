package io.bluetape4k.junit5.stopwatch

import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@StopwatchTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class StopwatchExtensionTest {

    companion object: KLogging()

    private lateinit var appender: InMemoryLogbackAppender

    @BeforeEach
    fun setup() {
        appender = InMemoryLogbackAppender(StopwatchExtension::class)
    }

    @AfterEach
    fun teardown() {
        appender.close()
    }

    @Test
    fun `테스트 후 실행시간을 로그에 출력합니다`() {
        Thread.sleep(10)

        // StopwatchExtension의 afterTestExecution이 호출되기 전이므로
        // "Starting test" 메시지가 이미 기록되었는지 확인합니다.
        appender.messages.shouldNotBeEmpty()
        appender.messages.any { it.contains("Starting test") }.shouldBeTrue()
    }

    @StopwatchTest
    fun `메소드 별로 실행 시간을 측정합니다`() {
        Thread.sleep(10)

        // "Starting test" 메시지가 로그에 기록되었는지 확인합니다.
        appender.messages.shouldNotBeEmpty()
        appender.messages.any { it.contains("Starting test") }.shouldBeTrue()
    }
}
