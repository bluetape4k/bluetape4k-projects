package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.Serializable
import kotlin.random.Random

@DisplayName("Jackson3Codec encode/decode & security")
class Jackson3CodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    data class Sample(val id: Long, val name: String, val tags: List<String>): Serializable

    data class Nested(val value: Int, val child: Sample): Serializable

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
    fun `Jackson3Codec 으로 정상 직렬화_역직렬화 roundtrip`() {
        val codec = Jackson3Codec()
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)

        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `String 타입 roundtrip`() {
        val codec = Jackson3Codec()
        val original = faker.lorem().sentence()

        val buf = codec.valueEncoder.encode(original)
        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Long 타입 roundtrip`() {
        val codec = Jackson3Codec()
        val original = Random.nextLong()

        val buf = codec.valueEncoder.encode(original)
        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `중첩 data class roundtrip`() {
        val codec = Jackson3Codec()
        val original = newNested()

        val buf = codec.valueEncoder.encode(original)
        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Jackson3Codec 역직렬화 실패 시 fallback Codec(Fory) 으로 자동 전환한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(fallbackCodec = fallbackCodec)

        val original = newSample()
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)

        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes)

        // Fory 바이트는 JSON 엔벨로프가 아니므로 Jackson3Codec이 fallback으로 전환한다.
        // fallback(Fory)이 원본을 복원하거나 실패해도 프로세스 예외가 전파되지 않아야 한다.
        runCatching {
            jackson3Codec.valueDecoder.decode(wrapped, null)
        }.isSuccess.shouldBeTrue()

        wrapped.release()

        foryEncodedBuf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 가 지정된 Jackson3Codec 은 binary fallback payload 를 거부한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
        )

        val original = newSample()
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)

        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes)

        assertFailsWith<SecurityException> {
            jackson3Codec.valueDecoder.decode(wrapped, null)
        }

        wrapped.release()
        foryEncodedBuf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `trusted migration mode 의 Jackson3Codec 은 allowlist 와 함께 binary fallback payload 를 허용한다`() {
        val fallbackCodec = RedissonCodecs.Fory
        val jackson3Codec = Jackson3Codec(
            fallbackCodec = fallbackCodec,
            allowedPackagePrefixes = setOf("io.bluetape4k."),
            allowFallbackDecode = true,
        )

        val original = newSample()
        val foryEncodedBuf = fallbackCodec.valueEncoder.encode(original)

        val foryBytes = ByteArray(foryEncodedBuf.readableBytes())
        foryEncodedBuf.getBytes(foryEncodedBuf.readerIndex(), foryBytes)

        val wrapped = Unpooled.wrappedBuffer(foryBytes)

        jackson3Codec.valueDecoder.decode(wrapped, null) shouldBeEqualTo original

        wrapped.release()
        foryEncodedBuf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 에 포함된 클래스는 정상 역직렬화된다`() {
        val codec = Jackson3Codec(
            allowedPackagePrefixes = setOf("io.bluetape4k.")
        )
        val original = newSample()

        val buf = codec.valueEncoder.encode(original)
        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        buf.release()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `allowedPackagePrefixes 에 포함되지 않은 클래스는 SecurityException 을 발생시킨다`() {
        val codec = Jackson3Codec(
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
    fun `Jackson3Codec ClassLoader 보조 생성자는 Fory fallback 을 기본으로 사용한다`() {
        val codec = Jackson3Codec(this::class.java.classLoader)
        codec.shouldNotBeNull()

        val original = faker.lorem().sentence()
        val buf = codec.valueEncoder.encode(original)

        codec.valueDecoder.decode(buf, null) shouldBeEqualTo original

        buf.release()
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
        str shouldContain "Jackson3Codec"
        str shouldContain "allowedPrefixes"
    }
}
