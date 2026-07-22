package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

/**
 * [LettuceBinaryCodecs.fastFory]와 [LettuceBinaryCodecs.fory] 간의 와이어 포맷 호환성 검증 테스트.
 *
 * ## 비대칭 호환성 규칙
 * - `fastFory` encode → `fastFory` decode: **성공** (roundtrip)
 * - `fory` encode → `fastFory` decode: **실패** (Lettuce는 fallback 없음)
 * - `fastFory` encode → `fory` decode: **실패** (Lettuce는 fallback 없음)
 *
 * ⚠️ Redisson 코덱과 달리 [LettuceBinaryCodec]은 내부 fallback 체인이 없으므로
 * 포맷 불일치 시 반드시 예외가 발생합니다.
 */
class FastForyCompatibilityTest {

    companion object: KLogging()

    private val fastForyCodec = LettuceBinaryCodecs.fastFory<Any>()
    private val foryCodec = LettuceBinaryCodecs.fory<Any>()

    data class SampleData(
        val id: Int,
        val name: String,
        val value: Double,
    ): java.io.Serializable {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private val testData = SampleData(id = 42, name = "lettuce-test", value = 2.718)

    /**
     * Task 13 - 테스트 1: fastFory roundtrip PASS 검증.
     *
     * fastFory codec으로 encode한 데이터를 fastFory codec으로 decode하면 원본과 동일해야 합니다.
     */
    @Test
    fun `fastFory codec roundtrip 성공`() {
        val encoded = fastForyCodec.encodeValue(testData)
        val decoded = fastForyCodec.decodeValue(encoded)
        log.debug { "decoded=$decoded" }
        decoded.shouldNotBeNull() shouldBeEqualTo testData
    }

    /**
     * Task 13 - 테스트 2 (방향 A 실패 고정): fory encode → fastFory decode 역직렬화 오류 검증.
     *
     * [LettuceBinaryCodecs.fory] 코덱(COMPATIBLE 모드)으로 encode한 데이터를
     * [LettuceBinaryCodecs.fastFory] 코덱(SCHEMA_CONSISTENT 모드)으로 decode하면
     * 포맷 불일치로 인해 역직렬화 예외가 발생해야 합니다.
     *
     * ⚠️ Lettuce 코덱은 fallback이 없으므로 Redisson 코덱과 달리 예외가 반드시 발생합니다.
     */
    @Test
    fun `방향A - fory 인코딩 데이터를 fastFory로 decode하면 예외 발생`() {
        val encoded = foryCodec.encodeValue(testData)
        // ByteBuffer position을 rewind해야 재사용 가능
        encoded.rewind()

        assertFailsWith<AssertionError> {
            fastForyCodec.decodeValue(encoded)
        }
    }

    /**
     * Task 13 - 테스트 3 (방향 B 실패 고정): fastFory encode → fory decode 역직렬화 오류 검증.
     *
     * [LettuceBinaryCodecs.fastFory] 코덱(SCHEMA_CONSISTENT 모드)으로 encode한 데이터를
     * [LettuceBinaryCodecs.fory] 코덱(COMPATIBLE 모드)으로 decode하면
     * 포맷 불일치로 인해 역직렬화 예외가 발생해야 합니다.
     *
     * ⚠️ Lettuce 코덱은 fallback이 없으므로 Redisson 코덱과 달리 예외가 반드시 발생합니다.
     */
    @Test
    fun `방향B - fastFory 인코딩 데이터를 fory로 decode하면 예외 발생`() {
        val encoded = fastForyCodec.encodeValue(testData)
        encoded.rewind()

        assertFailsWith<BinarySerializationException> {
            foryCodec.decodeValue(encoded)
        }
    }
}
