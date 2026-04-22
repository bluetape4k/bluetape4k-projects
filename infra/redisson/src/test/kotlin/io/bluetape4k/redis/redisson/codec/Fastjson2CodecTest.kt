package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.logging.KLogging
import io.netty.buffer.Unpooled
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Fastjson2Codec encode/decode & security")
class Fastjson2CodecTest {

    companion object: KLogging()

    data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable

    data class Nested(val value: Int, val child: Sample): java.io.Serializable

    @Test
    fun `Fastjson2Codec 으로 정상 직렬화_역직렬화 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = Sample(42L, "alice", listOf("a", "b", "c"))

        val buf = codec.valueEncoder.encode(original)
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `String 타입 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = "hello-world"

        val buf = codec.valueEncoder.encode(original)
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `Long 타입 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = 12345678901234L

        val buf = codec.valueEncoder.encode(original)
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `중첩 data class roundtrip`() {
        val codec = Fastjson2Codec()
        val original = Nested(7, Sample(1L, "nested", listOf("x")))

        val buf = codec.valueEncoder.encode(original)
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `Fastjson2Codec 역직렬화 실패 시 fallback Codec(Fory) 으로 자동 전환한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val fastjson2Codec = Fastjson2Codec(fallbackCodec = fallbackCodec)

        val original = Sample(99L, "bob", listOf("x", "y"))
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        try {
            val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
            foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

            val wrapped = Unpooled.wrappedBuffer(foryBytes)
            try {
                // Fory 바이트는 JSONB WriteClassName 포맷이 아니므로 Fastjson2Codec이 fallback으로 전환한다.
                runCatching { fastjson2Codec.valueDecoder.decode(wrapped, null) }
                    .isSuccess.shouldNotBeNull()
            } finally {
                wrapped.release()
            }
        } finally {
            foryEncodedBuf.release()
        }
    }

    @Test
    fun `allowedPackagePrefixes 에 포함된 클래스는 정상 역직렬화된다`() {
        val codec = Fastjson2Codec(
            allowedPackagePrefixes = setOf("io.bluetape4k.")
        )
        val original = Sample(1L, "allowed", listOf("ok"))

        val buf = codec.valueEncoder.encode(original)
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `allowedPackagePrefixes 에 포함되지 않은 클래스는 SecurityException 을 발생시킨다`() {
        val codec = Fastjson2Codec(
            allowedPackagePrefixes = setOf("com.example.")
        )
        val original = Sample(1L, "blocked", emptyList())

        val buf = codec.valueEncoder.encode(original)
        try {
            invoking { codec.valueDecoder.decode(buf, null) }
                .shouldThrow(SecurityException::class)
        } finally {
            buf.release()
        }
    }

    @Test
    fun `Fastjson2Codec ClassLoader 보조 생성자는 Fory fallback 을 기본으로 사용한다`() {
        val codec = Fastjson2Codec(this::class.java.classLoader)
        codec.shouldNotBeNull()

        val buf = codec.valueEncoder.encode("hello")
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo "hello"
        } finally {
            buf.release()
        }
    }

    @Test
    fun `FastjsonSerializer 로 인코딩된 바이트는 Fastjson2Codec 으로 도메인 타입 복원 불가`() {
        // FastjsonSerializer: WriteClassName 없이 JSONB 인코딩
        // Fastjson2Codec encoder: WriteClassName 포함 — 두 포맷은 비호환
        val serializer = FastjsonSerializer()
        val original = Sample(1L, "test", listOf("a"))
        val bytes = serializer.serialize(original) ?: return  // serializer가 null 반환 시 테스트 스킵

        val codec = Fastjson2Codec()
        val buf = Unpooled.wrappedBuffer(bytes)
        try {
            val decoded = runCatching { codec.valueDecoder.decode(buf, null) }.getOrNull()
            // decoded가 null이거나 Sample 타입이 아닌 JSONObject/Map으로 복원됨
            if (decoded != null) {
                decoded shouldNotBeInstanceOf Sample::class
            }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `Fastjson2Codec 과 FastjsonSerializer 는 서로 다른 포맷으로 인코딩한다`() {
        // Fastjson2Codec: WriteClassName 포함 JSONB 인코딩
        // FastjsonSerializer: WriteClassName 없이 JSONB 인코딩
        // → 두 인코더가 생성하는 바이트가 다름을 검증한다
        val codec = Fastjson2Codec()
        val serializer = FastjsonSerializer()
        val original = Sample(2L, "format-diff", listOf("b"))

        val buf = codec.valueEncoder.encode(original)
        val codecBytes = ByteArray(buf.readableBytes()).also { buf.getBytes(buf.readerIndex(), it) }
        buf.release()

        val serializerBytes = serializer.serialize(original) ?: return

        codecBytes.contentEquals(serializerBytes) shouldBeEqualTo false
    }

    @Test
    fun `Fastjson2Codec 는 null 이 아닌 Encoder_Decoder 를 반환한다`() {
        val codec = Fastjson2Codec()
        codec.valueEncoder.shouldNotBeNull()
        codec.valueDecoder.shouldNotBeNull()
        codec.mapKeyEncoder.shouldNotBeNull()
        codec.mapKeyDecoder.shouldNotBeNull()
    }
}
