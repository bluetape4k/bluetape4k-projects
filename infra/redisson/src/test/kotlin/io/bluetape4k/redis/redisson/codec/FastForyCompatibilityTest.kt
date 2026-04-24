package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.netty.buffer.Unpooled
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State

/**
 * [FastForyCodec]과 [ForyCodec] 간의 와이어 포맷 호환성 검증 테스트.
 *
 * ## 비대칭 호환성 규칙
 * - [FastForyCodec] encode → [FastForyCodec] decode: **성공** (roundtrip)
 * - [ForyCodec] encode → [FastForyCodec] decode: **성공** (FastFory 실패 → Fory fallback)
 * - [FastForyCodec] encode → [ForyCodec] decode: **실패** (Fory decode 실패 → Kryo5 fallback도 실패)
 */
class FastForyCompatibilityTest {

    companion object: KLogging()

    private val fastForyCodec = FastForyCodec()
    private val foryCodec = ForyCodec()

    data class SampleData(
        val id: Int,
        val name: String,
        val value: Double,
    ): java.io.Serializable

    private val testData = SampleData(id = 42, name = "test-data", value = 3.14)

    /**
     * Task 7 - 테스트 1: FastForyCodec roundtrip PASS 검증.
     * FastForyCodec으로 encode한 데이터를 FastForyCodec으로 decode하면 원본과 동일해야 합니다.
     */
    @Test
    fun `FastForyCodec roundtrip should succeed`() {
        val buf = fastForyCodec.valueEncoder.encode(testData)
        try {
            val decoded = fastForyCodec.valueDecoder.decode(buf, State())
            decoded.shouldNotBeNull()
            decoded shouldBeEqualTo testData
        } finally {
            buf.release()
        }
    }

    /**
     * Task 7 - 테스트 2 (방향 A): ForyCodec encode → FastForyCodec decode 성공 검증.
     * ForyCodec(COMPATIBLE 모드)으로 encode한 데이터를 FastForyCodec으로 decode할 때
     * FastFory 직접 decode가 실패하더라도 Fory fallback을 통해 성공해야 합니다.
     */
    @Test
    fun `ForyCodec encoded data should be decodable by FastForyCodec via fallback`() {
        val buf = foryCodec.valueEncoder.encode(testData)
        val bytes = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), bytes)
        buf.release()

        // FastForyCodec은 Fory fallback을 통해 ForyCodec 인코딩 데이터를 읽을 수 있습니다
        val decodeBuf = Unpooled.wrappedBuffer(bytes)
        try {
            val decoded = fastForyCodec.valueDecoder.decode(decodeBuf, State())
            decoded.shouldNotBeNull()
            decoded shouldBeEqualTo testData
        } finally {
            decodeBuf.release()
        }
    }

    /**
     * M3: copy-constructor 경로 검증 — Redisson 동적 인스턴스화 시 사용되는 경로.
     */
    @Test
    fun `copy-constructor(classLoader, codec) should produce functional codec`() {
        val classLoader = Thread.currentThread().contextClassLoader
        val copied = FastForyCodec(classLoader, fastForyCodec)

        val buf = copied.valueEncoder.encode(testData)
        try {
            val decoded = copied.valueDecoder.decode(buf, State())
            decoded.shouldNotBeNull()
            decoded shouldBeEqualTo testData
        } finally {
            buf.release()
        }
    }

    /**
     * Task 7 - 테스트 3 (방향 B 고정): FastForyCodec encode → ForyCodec decode 비호환 검증.
     *
     * FastForyCodec(SCHEMA_CONSISTENT)으로 encode한 데이터는 ForyCodec(COMPATIBLE)으로 올바르게
     * 복원되지 않습니다. ForyCodec 내부에서 예외를 잡고 fallback(Kryo5)도 실패하며 null을 반환합니다.
     * 즉, 원본 객체와 동일한 값을 얻을 수 없습니다.
     *
     * ⚠️ 비대칭 호환성: ForyCodec이 FastFory 데이터를 decode할 때 예외 없이 null을 반환합니다.
     */
    @Test
    fun `FastForyCodec encoded data cannot be correctly decoded by ForyCodec`() {
        val buf = fastForyCodec.valueEncoder.encode(testData)
        val bytes = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), bytes)
        buf.release()

        // ForyCodec은 FastFory 포맷을 COMPATIBLE decode 실패 → Kryo5 fallback도 실패 → null 반환
        val decodeBuf = Unpooled.wrappedBuffer(bytes)
        try {
            val decoded = foryCodec.valueDecoder.decode(decodeBuf, State())
            // null이거나 원본과 다른 값이 반환됩니다 (비호환)
            // decoded가 null이거나 원본 testData와 다른 값임을 검증합니다
            check(decoded == null || decoded != testData) {
                "ForyCodec should NOT correctly decode FastFory-encoded data, but got: $decoded"
            }
        } finally {
            decodeBuf.release()
        }
    }
}
