package io.bluetape4k.kafka.codec

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.toUtf8Bytes
import kotlinx.coroutines.CancellationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test

/**
 * [AbstractKafkaCodec.deserialize] 의 poison-pill 정책과 예외 통과 정책을 검증한다.
 *
 * - 일반 [Exception] → WARN 로그 + null 반환 (consumer 루프 진행 보장)
 * - [CancellationException] → 항상 재던짐 (코루틴 취소 신호 보존)
 * - [Error] → 그대로 전파 (JVM 손상 상태 은폐 금지)
 */
class AbstractKafkaCodecPoisonPillTest {

    companion object: KLoggingChannel()

    private class FakeException: RuntimeException("fake deserialize failure")

    private class ThrowingCodec(private val toThrow: Throwable): AbstractKafkaCodec<String>() {
        override fun doSerialize(topic: String?, headers: Headers?, graph: String): ByteArray =
            graph.toByteArray()

        override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): String? {
            throw toThrow
        }
    }

    @Test
    fun `general Exception logs bounded context without payload or header values`() {
        val codec = ThrowingCodec(FakeException())
        val headers = RecordHeaders().add("trace-id", "secret-header".toUtf8Bytes())
        val logger = AbstractKafkaCodec.log as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            codec.deserialize("test-topic", headers, "secret-payload".toUtf8Bytes()).shouldBeNull()
            val event = appender.list.single()
            event.level shouldBeEqualTo Level.WARN
            event.throwableProxy.shouldBeNull()
            val message = event.formattedMessage
            message.contains("topic=test-topic") shouldBeEqualTo true
            message.contains("trace-id") shouldBeEqualTo true
            message.contains("dataSize=14") shouldBeEqualTo true
            message.contains("failureType=${FakeException::class.java.name}") shouldBeEqualTo true
            message.contains("secret-header") shouldBeEqualTo false
            message.contains("secret-payload") shouldBeEqualTo false
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `CancellationException is rethrown with identity preserved`() {
        val failure = CancellationException("coroutine cancelled")
        val thrown = assertFailsWith<CancellationException> {
            ThrowingCodec(failure).deserialize("test-topic", RecordHeaders(), byteArrayOf(1, 2, 3))
        }
        thrown shouldBeSameInstanceAs failure
    }

    @Test
    fun `Error is propagated with identity preserved`() {
        val failure = OutOfMemoryError("simulated")
        val thrown = assertFailsWith<OutOfMemoryError> {
            ThrowingCodec(failure).deserialize("test-topic", RecordHeaders(), byteArrayOf(1, 2, 3))
        }
        thrown shouldBeSameInstanceAs failure
    }

    @Test
    fun `null data returns null without invoking doDeserialize`() {
        // doDeserialize 가 호출되면 예외가 던져지므로, null 입력에서 doDeserialize 가 호출되지 않음을 보장
        val codec = ThrowingCodec(FakeException())
        val headers = RecordHeaders()
        val result = codec.deserialize("test-topic", headers, null as ByteArray?)
        result.shouldBeNull()
    }
}
