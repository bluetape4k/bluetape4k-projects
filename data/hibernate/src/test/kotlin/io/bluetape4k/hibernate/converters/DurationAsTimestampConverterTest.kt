package io.bluetape4k.hibernate.converters

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationAsTimestampConverterTest {

    private val converter = DurationAsTimestampConverter()

    @Test
    fun `convertToDatabaseColumn은 null 입력 시 null을 반환한다`() {
        converter.convertToDatabaseColumn(null).shouldBeNull()
    }

    @Test
    fun `convertToEntityAttribute은 null 입력 시 null을 반환한다`() {
        converter.convertToEntityAttribute(null).shouldBeNull()
    }

    @Test
    fun `Duration을 Timestamp로 변환한다`() {
        val duration = Duration.ofMillis(12345L)
        val timestamp = converter.convertToDatabaseColumn(duration)
        timestamp.shouldNotBeNull()
        timestamp.time shouldBeEqualTo 12345L
    }

    @Test
    fun `Timestamp를 Duration으로 역변환한다`() {
        val duration = Duration.ofSeconds(60)
        val timestamp = converter.convertToDatabaseColumn(duration)!!
        val restored = converter.convertToEntityAttribute(timestamp)
        restored shouldBeEqualTo duration
    }

    @Test
    fun `왕복 변환 후 원본과 동일해야 한다`() {
        val durations = listOf(
            Duration.ZERO,
            Duration.ofSeconds(1),
            Duration.ofMinutes(5),
            Duration.ofHours(2),
            Duration.ofDays(1),
        )
        durations.forEach { original ->
            val timestamp = converter.convertToDatabaseColumn(original)!!
            val restored = converter.convertToEntityAttribute(timestamp)
            restored shouldBeEqualTo original
        }
    }

    @Test
    fun `음수 Duration도 변환한다`() {
        val duration = Duration.ofMillis(-5000L)
        val timestamp = converter.convertToDatabaseColumn(duration)!!
        val restored = converter.convertToEntityAttribute(timestamp)
        restored shouldBeEqualTo duration
    }
}
