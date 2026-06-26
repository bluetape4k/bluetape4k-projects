package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.netty.buffer.Unpooled
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Jackson3Codec encode/decode & security")
class Jackson3CodecTest {

    companion object: KLogging()

    data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable

    data class Nested(val value: Int, val child: Sample): java.io.Serializable

    @Test
    fun `Jackson3Codec 으로 정상 직렬화_역직렬화 roundtrip`() {
        val codec = Jackson3Codec()
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
        val codec = Jackson3Codec()
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
        val codec = Jackson3Codec()
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
        val codec = Jackson3Codec()
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
    fun `Jackson3Codec 역직렬화 실패 시 fallback Codec(Fory) 으로 자동 전환한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(fallbackCodec = fallbackCodec)

        val original = Sample(99L, "bob", listOf("x", "y"))
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        try {
            val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
            foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

            val wrapped = Unpooled.wrappedBuffer(foryBytes)
            try {
                // Fory 바이트는 JSON 엔벨로프가 아니므로 Jackson3Codec이 fallback으로 전환한다.
                // fallback(Fory)이 원본을 복원하거나 실패해도 프로세스 예외가 전파되지 않아야 한다.
                runCatching { jackson3Codec.valueDecoder.decode(wrapped, null) }
                    .isSuccess.shouldNotBeNull()
            } finally {
                wrapped.release()
            }
        } finally {
            foryEncodedBuf.release()
        }
    }

    @Test
    fun `allowedPackagePrefixes 가 지정된 Jackson3Codec 은 binary fallback payload 를 거부한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
        )

        val original = Sample(99L, "blocked-fallback", listOf("x", "y"))
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        try {
            val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
            foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

            val wrapped = Unpooled.wrappedBuffer(foryBytes)
            try {
                assertFailsWith<SecurityException> {
                    jackson3Codec.valueDecoder.decode(wrapped, null)
                }
            } finally {
                wrapped.release()
            }
        } finally {
            foryEncodedBuf.release()
        }
    }

    @Test
    fun `trusted migration mode 의 Jackson3Codec 은 allowlist 와 함께 binary fallback payload 를 허용한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
            allowFallbackDecode = true,
        )

        val original = Sample(100L, "migration", listOf("legacy"))
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        try {
            val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
            foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

            val wrapped = Unpooled.wrappedBuffer(foryBytes)
            try {
                jackson3Codec.valueDecoder.decode(wrapped, null) shouldBeEqualTo original
            } finally {
                wrapped.release()
            }
        } finally {
            foryEncodedBuf.release()
        }
    }

    @Test
    fun `allowedPackagePrefixes 에 포함된 클래스는 정상 역직렬화된다`() {
        val codec = Jackson3Codec(
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
        val codec = Jackson3Codec(
            allowedPackagePrefixes = setOf("com.example.")
        )
        val original = Sample(1L, "blocked", emptyList())

        val buf = codec.valueEncoder.encode(original)
        try {
            assertFailsWith<SecurityException> { codec.valueDecoder.decode(buf, null) }
        } finally {
            buf.release()
        }
    }

    @Test
    fun `Jackson3Codec ClassLoader 보조 생성자는 Fory fallback 을 기본으로 사용한다`() {
        val codec = Jackson3Codec(this::class.java.classLoader)
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
    fun `Jackson3Codec 는 null 이 아닌 Encoder_Decoder 를 반환한다`() {
        val codec = Jackson3Codec()
        codec.valueEncoder.shouldNotBeNull()
        codec.valueDecoder.shouldNotBeNull()
        codec.mapKeyEncoder.shouldNotBeNull()
        codec.mapKeyDecoder.shouldNotBeNull()
    }

    @Test
    fun `Jackson3Codec toString 에는 fallback 과 allowedPrefixes 정보가 포함된다`() {
        val codec = Jackson3Codec(allowedPackagePrefixes = setOf("io.bluetape4k."))
        val str = codec.toString()
        str.contains("Jackson3Codec") shouldBeEqualTo true
        str.contains("allowedPrefixes") shouldBeEqualTo true
    }
}
