package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.support.emptyByteArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class RedisBinarySerializersTest: AbstractRedisSerializerTest() {

    companion object {

        @JvmStatic
        fun binarySerializers(): Stream<Arguments> = Stream.of(
            Arguments.of(RedisBinarySerializers.Jdk, "Jdk"),
            Arguments.of(RedisBinarySerializers.Kryo, "Kryo"),
            Arguments.of(RedisBinarySerializers.Fory, "Fory"),
            Arguments.of(RedisBinarySerializers.GzipJdk, "GzipJdk"),
            Arguments.of(RedisBinarySerializers.GzipKryo, "GzipKryo"),
            Arguments.of(RedisBinarySerializers.GzipFory, "GzipFory"),
            Arguments.of(RedisBinarySerializers.LZ4Jdk, "LZ4Jdk"),
            Arguments.of(RedisBinarySerializers.LZ4Kryo, "LZ4Kryo"),
            Arguments.of(RedisBinarySerializers.LZ4Fory, "LZ4Fory"),
            Arguments.of(RedisBinarySerializers.SnappyJdk, "SnappyJdk"),
            Arguments.of(RedisBinarySerializers.SnappyKryo, "SnappyKryo"),
            Arguments.of(RedisBinarySerializers.SnappyFory, "SnappyFory"),
            Arguments.of(RedisBinarySerializers.ZstdJdk, "ZstdJdk"),
            Arguments.of(RedisBinarySerializers.ZstdKryo, "ZstdKryo"),
            Arguments.of(RedisBinarySerializers.ZstdFory, "ZstdFory"),
        )

        @JvmStatic
        fun compressSerializers(): Stream<Arguments> = Stream.of(
            Arguments.of(RedisBinarySerializers.Gzip, "Gzip"),
            Arguments.of(RedisBinarySerializers.LZ4, "LZ4"),
            Arguments.of(RedisBinarySerializers.Snappy, "Snappy"),
            Arguments.of(RedisBinarySerializers.Zstd, "Zstd"),
        )
    }

    @ParameterizedTest(name = "[{1}] 직렬화 왕복 검증")
    @MethodSource("binarySerializers")
    fun `BinarySerializer 직렬화 왕복 검증`(serializer: RedisBinarySerializer, name: String) {
        val sample = newSample()
        val bytes = serializer.serialize(sample)
        bytes.shouldNotBeNull()
        serializer.deserialize(bytes) shouldBeEqualTo sample
    }

    @ParameterizedTest(name = "[{1}] null 직렬화 → null 반환")
    @MethodSource("binarySerializers")
    fun `BinarySerializer null 직렬화는 emptyByteArray 를 반환한다`(serializer: RedisBinarySerializer, name: String) {
        serializer.serialize(null) shouldBeEqualTo emptyByteArray
    }

    @ParameterizedTest(name = "[{1}] null 역직렬화 → null 반환")
    @MethodSource("binarySerializers")
    fun `BinarySerializer null 역직렬화는 null을 반환한다`(serializer: RedisBinarySerializer, name: String) {
        serializer.deserialize(null) shouldBeEqualTo null
    }

    @ParameterizedTest(name = "[{1}] 압축 왕복 검증")
    @MethodSource("compressSerializers")
    fun `CompressSerializer 압축 왕복 검증`(serializer: RedisCompressSerializer, name: String) {
        val bytes = newSampleBytes()
        val compressed = serializer.serialize(bytes)
        compressed.shouldNotBeNull()
        assertArrayEquals(bytes, serializer.deserialize(compressed))
    }

    @ParameterizedTest(name = "[{1}] null 직렬화 → emptyByteArray 반환")
    @MethodSource("compressSerializers")
    fun `CompressSerializer null 직렬화는 emptyByteArray 을 반환한다`(serializer: RedisCompressSerializer, name: String) {
        serializer.serialize(null) shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `모든 BinarySerializer 싱글턴이 non-null이다`() {
        binarySerializers().forEach { args -> (args.get()[0] as RedisBinarySerializer).shouldNotBeNull() }
    }

    @Test
    fun `모든 CompressSerializer 싱글턴이 non-null이다`() {
        compressSerializers().forEach { args -> (args.get()[0] as RedisCompressSerializer).shouldNotBeNull() }
    }
}
