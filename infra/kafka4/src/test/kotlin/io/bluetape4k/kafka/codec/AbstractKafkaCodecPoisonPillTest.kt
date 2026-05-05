package io.bluetape4k.kafka.codec

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeNull
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
    fun `general Exception is swallowed and returns null`() {
        val codec = ThrowingCodec(FakeException())
        val headers = RecordHeaders()
        val result = codec.deserialize("test-topic", headers, byteArrayOf(1, 2, 3))
        result.shouldBeNull()
    }

    @Test
    fun `CancellationException is rethrown not swallowed`() {
        val codec = ThrowingCodec(CancellationException("coroutine cancelled"))
        val headers = RecordHeaders()
        assertFailsWith<CancellationException> {
            codec.deserialize("test-topic", headers, byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun `Error is propagated not swallowed`() {
        val codec = ThrowingCodec(OutOfMemoryError("simulated"))
        val headers = RecordHeaders()
        assertFailsWith<OutOfMemoryError> {
            codec.deserialize("test-topic", headers, byteArrayOf(1, 2, 3))
        }
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
