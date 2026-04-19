package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.serializer.BinarySerializers
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class LettuceBinaryCodecSizeTest {

    private val codec = LettuceBinaryCodec<Any>(BinarySerializers.Jdk)

    @Test
    fun `estimateSize should handle string and byte array`() {
        codec.estimateSize("abc") shouldBeEqualTo 3
        codec.estimateSize(byteArrayOf(1, 2, 3)) shouldBeEqualTo 3
    }

    @Test
    fun `estimateSize should handle byte buffer remaining`() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4))
        buffer.get()
        codec.estimateSize(buffer) shouldBeEqualTo 3
    }

    @Test
    fun `estimateSize should return -1 for custom object to avoid double serialization`() {
        // 개선: 기존엔 커스텀 객체를 실제 직렬화해 크기를 측정했으나,
        //       Lettuce 가 인코딩 시 encodeValue 를 다시 호출해 put 1 회당 직렬화 2 회가 발생했습니다.
        //       이제 V 타입은 -1(estimate 불가)을 반환해 Netty 가 동적으로 버퍼를 확장하도록 합니다.
        val value = SampleValue(1, "name")
        codec.estimateSize(value) shouldBeEqualTo -1
    }

    @Test
    fun `estimateSize should return 0 for null`() {
        codec.estimateSize(null) shouldBeEqualTo 0
    }

    data class SampleValue(
        val id: Int,
        val name: String,
    ): java.io.Serializable
}
