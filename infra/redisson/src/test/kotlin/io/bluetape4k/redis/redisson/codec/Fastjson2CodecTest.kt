package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

@DisplayName("Fastjson2Codec encode/decode & security")
class Fastjson2CodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable

    data class Nested(val value: Int, val child: Sample): java.io.Serializable

    private fun newSample(): Sample = Sample(
        faker.random().nextLong(),
        faker.name().name(),
        faker.lorem().words(Random.nextInt(5))
    )

    private fun newNested(): Nested = Nested(
        value = faker.random().nextInt(),
        child = newSample()
    )

    @RepeatedTest(REPEAT_SIZE)
    fun `Fastjson2Codec 으로 정상 직렬화_역직렬화 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)
        val decoded = codec.valueDecoder.decode(buf, null)
        decoded shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `String 타입 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = faker.lorem().paragraph()

        val buf = codec.valueEncoder.encode(original)
        val decoded = codec.valueDecoder.decode(buf, null)
        decoded shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Long 타입 roundtrip`() {
        val codec = Fastjson2Codec()
        val original = faker.random().nextLong()

        val buf = codec.valueEncoder.encode(original)
        val decoded = codec.valueDecoder.decode(buf, null)
        decoded shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `중첩 data class roundtrip`() {
        val codec = Fastjson2Codec()
        val original = newNested()

        val buf = codec.valueEncoder.encode(original)
        val decoded = codec.valueDecoder.decode(buf, null)
        decoded shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Fastjson2Codec 역직렬화 실패 시 fallback Codec(Fory) 으로 자동 전환한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val fastjson2Codec = Fastjson2Codec(fallbackCodec = fallbackCodec)
        val original = newSample()

        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes)
        try {
            // Fory 바이트는 JSONB WriteClassName 포맷이 아니므로 Fastjson2Codec이 fallback으로 전환한다.
            runCatching {
                fastjson2Codec.valueDecoder.decode(wrapped, null)
            }.isSuccess.shouldBeTrue()
        } finally {
            wrapped.release()
        }

        foryEncodedBuf.release()

    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 가 지정된 Fastjson2Codec 은 binary fallback payload 를 거부한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val fastjson2Codec = Fastjson2Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
        )

        val original = newSample()
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)

        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes)
        assertFailsWith<SecurityException> {
            fastjson2Codec.valueDecoder.decode(wrapped, null)
        }
        wrapped.release()
        foryEncodedBuf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `trusted migration mode 의 Fastjson2Codec 은 allowlist 와 함께 binary fallback payload 를 허용한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val fastjson2Codec = Fastjson2Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
            allowFallbackDecode = true,
        )

        val original = newSample()
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)

        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes).shouldNotBeNull()
        fastjson2Codec.valueDecoder.decode(wrapped, null) shouldBeEqualTo original
        wrapped.release()

        foryEncodedBuf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 에 포함된 클래스는 정상 역직렬화된다`() {
        val codec = Fastjson2Codec(
            allowedPackagePrefixes = setOf("io.bluetape4k.")
        )
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)
        val decoded = codec.valueDecoder.decode(buf, null).shouldNotBeNull()
        decoded shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 에 포함되지 않은 클래스는 SecurityException 을 발생시킨다`() {
        val codec = Fastjson2Codec(
            allowedPackagePrefixes = setOf("com.example.")
        )
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)

        assertFailsWith<SecurityException> {
            codec.valueDecoder.decode(buf, null)
        }

        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Fastjson2Codec ClassLoader 보조 생성자는 Fory fallback 을 기본으로 사용한다`() {
        val codec = Fastjson2Codec(this::class.java.classLoader)
        codec.shouldNotBeNull()

        val expected = faker.lorem().paragraph()
        val buf = codec.valueEncoder.encode(expected)
        val decoded = codec.valueDecoder.decode(buf, null).shouldNotBeNull()
        decoded shouldBeEqualTo expected
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `FastjsonSerializer 로 인코딩된 바이트는 Fastjson2Codec 으로 도메인 타입 복원 불가`() {
        // FastjsonSerializer: WriteClassName 없이 JSONB 인코딩
        // Fastjson2Codec encoder: WriteClassName 포함 — 두 포맷은 비호환
        val serializer = FastjsonSerializer()
        val original = newSample()
        val bytes = serializer.serialize(original)

        val codec = Fastjson2Codec()
        val buf = Unpooled.wrappedBuffer(bytes)

        val decoded = runCatching {
            codec.valueDecoder.decode(buf, null)
        }.getOrNull()

        // decoded가 null이거나 Sample 타입이 아닌 JSONObject/Map으로 복원됨
        decoded?.shouldNotBeInstanceOf<Sample>()

        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Fastjson2Codec 과 FastjsonSerializer 는 서로 다른 포맷으로 인코딩한다`() {
        // Fastjson2Codec: WriteClassName 포함 JSONB 인코딩
        // FastjsonSerializer: WriteClassName 없이 JSONB 인코딩
        // → 두 인코더가 생성하는 바이트가 다름을 검증한다
        val codec = Fastjson2Codec()
        val serializer = FastjsonSerializer()
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)
        val codecBytes = ByteArray(buf.readableBytes()).also {
            buf.getBytes(buf.readerIndex(), it)
        }
        buf.release()

        val serializerBytes = serializer.serialize(original)

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

    @Test
    fun `Fastjson2Codec toString 에는 fallback 과 allowedPrefixes 정보가 포함된다`() {
        val codec = Fastjson2Codec(allowedPackagePrefixes = setOf("io.bluetape4k."))
        val str = codec.toString()
        str shouldContain "Fastjson2Codec"
        str shouldContain "allowedPrefixes"
    }
}
