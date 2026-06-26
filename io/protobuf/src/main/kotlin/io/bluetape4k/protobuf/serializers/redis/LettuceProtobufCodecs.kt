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

    private val strictSerializer: BinarySerializer by lazy { ProtobufSerializer() }
    private val trustedInternalSerializer: BinarySerializer by lazy { ProtobufSerializer.trustedInternalProtobuf() }

    /**
     * Protobuf Serializer를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.protobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> protobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(strictSerializer)

    /**
     * Protobuf Serializer와 Gzip Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.gzipProtobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> gzipProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(strictSerializer, Compressors.GZip))

    /**
     * Protobuf Serializer와 Deflate Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.deflateProtobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> deflateProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(strictSerializer, Compressors.Deflate))

    /**
     * Protobuf Serializer와 LZ4 Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.lz4Protobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> lz4Protobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(strictSerializer, Compressors.LZ4))

    /**
     * Protobuf Serializer와 Snappy Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.snappyProtobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> snappyProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(strictSerializer, Compressors.Snappy))

    /**
     * Protobuf Serializer와 Zstd Compressor를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec]를 생성합니다.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.zstdProtobuf<MyMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> zstdProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(strictSerializer, Compressors.Zstd))

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec.
     *
     * Use only for internal Redis stores that already contain legacy fallback-encoded values.
     */
    fun <V: Any> trustedInternalProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(trustedInternalSerializer)

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec with Gzip compression.
     */
    fun <V: Any> trustedInternalGzipProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(trustedInternalSerializer, Compressors.GZip))

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec with Deflate compression.
     */
    fun <V: Any> trustedInternalDeflateProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(trustedInternalSerializer, Compressors.Deflate))

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec with LZ4 compression.
     */
    fun <V: Any> trustedInternalLz4Protobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(trustedInternalSerializer, Compressors.LZ4))

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec with Snappy compression.
     */
    fun <V: Any> trustedInternalSnappyProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(trustedInternalSerializer, Compressors.Snappy))

    /**
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec with Zstd compression.
     */
    fun <V: Any> trustedInternalZstdProtobuf(): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(trustedInternalSerializer, Compressors.Zstd))
}
