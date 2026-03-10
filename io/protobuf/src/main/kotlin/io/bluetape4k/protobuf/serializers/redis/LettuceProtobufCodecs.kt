package io.bluetape4k.protobuf.serializers.redis

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.CompressableBinarySerializer
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec

/**
 * Protobuf Serializer를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec] 팩토리 모음.
 *
 * Protobuf가 classpath에 있을 때만 이 object를 참조하세요.
 * [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs]와 달리 Protobuf 의존성이 없으면 클래스 로드 시 [NoClassDefFoundError]가 발생합니다.
 */
object LettuceProtobufCodecs {

    private val serializer: BinarySerializer by lazy { ProtobufSerializer() }

    /**
     * Protobuf Serializer를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> protobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(serializer)

    /**
     * Protobuf Serializer와 Gzip Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, Compressors.GZip))

    /**
     * Protobuf Serializer와 Deflate Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, Compressors.Deflate))

    /**
     * Protobuf Serializer와 LZ4 Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Protobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, Compressors.LZ4))

    /**
     * Protobuf Serializer와 Snappy Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, Compressors.Snappy))

    /**
     * Protobuf Serializer와 Zstd Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, Compressors.Zstd))
}
