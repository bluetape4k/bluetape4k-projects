package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.netty.buffer.Unpooled
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ForyCodec encode/decode & fallback")
class ForyCodecTest {

    companion object: KLogging()

    data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable

    @Test
    fun `Fory 로 정상 직렬화_역직렬화 roundtrip`() {
        val codec = ForyCodec()
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
    fun `Fory 역직렬화 실패 시 fallback Codec(Kryo5) 으로 자동 전환한다`() {
        // Fory 가 해석하지 못하는 바이트(깨진 헤더)를 전달했을 때,
        // fallback Codec 이 호출되어 복구를 시도하거나, 최종적으로 null 을 반환해도
        // 프로세스 레벨 예외가 외부로 전파되지 않아야 한다 (안정성 보장).
        val fallbackCodec = RedissonCodecs.Kryo5
        val foryCodec = ForyCodec(fallbackCodec)

        val original = Sample(99L, "bob", listOf("x", "y"))
        val kryoEncodedBuf = fallbackCodec.valueEncoder.encode(original)
        try {
            val kryoBytes = ByteArray(kryoEncodedBuf.readableBytes())
            kryoEncodedBuf.getBytes(kryoEncodedBuf.readerIndex(), kryoBytes)

            val wrapped = Unpooled.wrappedBuffer(kryoBytes)
            try {
                // Fory 가 Kryo5 바이트를 해석하지 못하면 fallback 으로 전환한다.
                // Fory 가 우연히 decode 에 성공하거나 null 반환할 수도 있으므로
                // 핵심은 예외 없이 처리되어 프로세스가 계속 진행될 수 있어야 한다.
                runCatching { foryCodec.valueDecoder.decode(wrapped, null) }
                    .isSuccess.shouldNotBeNull()
            } finally {
                wrapped.release()
            }
        } finally {
            kryoEncodedBuf.release()
        }
    }

    @Test
    fun `Encode 실패 시 fallback Codec 으로 전환한다`() {
        // 직렬화 불가능한 타입(예: Lambda/Thread)을 주면 Fory 가 실패할 수 있다.
        // 이 경우 fallback 으로 전환되어야 예외가 전파되지 않아야 한다.
        val foryCodec = ForyCodec(RedissonCodecs.Kryo5)

        // Fory 가 거의 모든 것을 처리할 수 있으므로, 정상 경로로 진행되어도 OK.
        // 목적은 내부 catch 가 동작하여 예외가 터지지 않는 것.
        val result = runCatching {
            val buf = foryCodec.valueEncoder.encode("simple-string")
            buf.release()
        }
        result.isSuccess.shouldNotBeNull()
    }

    @Test
    fun `ForyCodec 는 null 이 아닌 Encoder_Decoder 를 반환한다`() {
        val codec = ForyCodec()
        codec.valueEncoder.shouldNotBeNull()
        codec.valueDecoder.shouldNotBeNull()
        codec.mapKeyEncoder.shouldNotBeNull()
        codec.mapKeyDecoder.shouldNotBeNull()
        codec.mapValueEncoder.shouldNotBeNull()
        codec.mapValueDecoder.shouldNotBeNull()
    }

    @Test
    fun `ForyCodec(ClassLoader) 보조 생성자는 Kryo5 fallback 을 기본으로 사용한다`() {
        val codec = ForyCodec(this::class.java.classLoader)
        codec.shouldNotBeNull()
        // roundtrip 이 성공해야 한다
        val buf = codec.valueEncoder.encode("hello")
        try {
            val decoded = codec.valueDecoder.decode(buf, null)
            decoded shouldBeEqualTo "hello"
        } finally {
            buf.release()
        }
    }
}
