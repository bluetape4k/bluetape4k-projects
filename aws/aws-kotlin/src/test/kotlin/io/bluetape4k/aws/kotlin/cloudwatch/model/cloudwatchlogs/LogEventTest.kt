package io.bluetape4k.aws.kotlin.cloudwatch.model.cloudwatchlogs

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class LogEventTest {

    companion object : KLogging()

    @Test
    fun `inputLogEvent DSL 블록으로 InputLogEvent를 생성한다`() {
        val now = System.currentTimeMillis()
        val event = inputLogEvent {
            timestamp = now
            message = "Hello, CloudWatch Logs!"
        }

        event.timestamp shouldBeEqualTo now
        event.message shouldBeEqualTo "Hello, CloudWatch Logs!"
    }

    @Test
    fun `inputLogEventOf는 타임스탬프와 메시지로 InputLogEvent를 생성한다`() {
        val now = System.currentTimeMillis()
        val event = inputLogEventOf(now, "Test log message")

        event.timestamp shouldBeEqualTo now
        event.message shouldBeEqualTo "Test log message"
    }

    @Test
    fun `inputLogEventOf는 builder 블록을 통해 추가 설정이 가능하다`() {
        val now = System.currentTimeMillis()
        val event = inputLogEventOf(now, "Error occurred") {
            // builder block is available
        }

        event.shouldNotBeNull()
        event.message shouldBeEqualTo "Error occurred"
    }

    @Test
    fun `inputLogEventOf 인스턴스는 null이 아니다`() {
        val event = inputLogEventOf(System.currentTimeMillis(), "log line")
        event.shouldNotBeNull()
    }

    @Test
    fun `여러 LogEvent를 생성하고 목록으로 만들 수 있다`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            inputLogEventOf(now, "event-1"),
            inputLogEventOf(now + 1, "event-2"),
            inputLogEventOf(now + 2, "event-3"),
        )

        events.size shouldBeEqualTo 3
        events[0].message shouldBeEqualTo "event-1"
        events[2].message shouldBeEqualTo "event-3"
    }
}
