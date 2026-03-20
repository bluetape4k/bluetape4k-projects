package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class RedisCompressSerializerTest: AbstractRedisSerializerTest() {

    companion object {

        @JvmStatic
        fun compressors(): Stream<Arguments> = Stream.of(
            Arguments.of(Compressors.GZip, "GZip"),
            Arguments.of(Compressors.LZ4, "LZ4"),
            Arguments.of(Compressors.Snappy, "Snappy"),
            Arguments.of(Compressors.Zstd, "Zstd"),
        )
    }

    @ParameterizedTest(name = "[{1}] null 직렬화 → null 반환")
    @MethodSource("compressors")
    fun `null 직렬화는 null을 반환한다`(compressor: Compressor, name: String) {
        val serializer = RedisCompressSerializer(compressor)
        serializer.serialize(null).shouldBeNull()
    }

    @ParameterizedTest(name = "[{1}] null 역직렬화 → null 반환")
    @MethodSource("compressors")
    fun `null 역직렬화는 null을 반환한다`(compressor: Compressor, name: String) {
        val serializer = RedisCompressSerializer(compressor)
        serializer.deserialize(null).shouldBeNull()
    }

    @ParameterizedTest(name = "[{1}] 압축 후 복원한다")
    @MethodSource("compressors")
    fun `압축 후 복원한다`(compressor: Compressor, name: String) {
        val serializer = RedisCompressSerializer(compressor)
        val bytes = newSampleBytes()

        val compressed = serializer.serialize(bytes)
        compressed.shouldNotBeNull()

        val restored = serializer.deserialize(compressed)
        assertArrayEquals(bytes, restored)
    }
}
