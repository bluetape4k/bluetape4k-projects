package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.io.serializer.CompressableBinarySerializer
import io.bluetape4k.protobuf.serializers.ProtobufSerializer

object LettuceBinaryCodecs {

    val Default: LettuceBinaryCodec<Any> by lazy { lz4Fory() }

    fun <V: Any> codec(serializer: BinarySerializer): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(serializer)

    fun <V: Any> compressedCodec(compressor: Compressor, serializer: BinarySerializer): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(CompressableBinarySerializer(serializer, compressor))

    private val protobufSerializer by lazy { ProtobufSerializer() }

    /**
     * Jdk Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> jdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.Jdk)

    /**
     * Kryo Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> kryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.Kryo)

    /**
     * Protobuf Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> protobuf(): LettuceBinaryCodec<V> = codec(protobufSerializer)

    /**
     * Fory Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> fory(): LettuceBinaryCodec<V> = codec(BinarySerializers.Fory)


    /**
     * Jdk Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipJdk)

    /**
     * Kryo Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipKryo)

    /**
     * Protobuf Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipProtobuf(): LettuceBinaryCodec<V> = compressedCodec(Compressors.GZip, protobufSerializer)

    /**
     * Fory Serializer와 Gzip Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> gzipFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.GZipFory)


    /**
     * Jdk Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateJdk)

    /**
     * Kryo Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateKryo)

    /**
     * Protobuf Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateProtobuf(): LettuceBinaryCodec<V> = compressedCodec(Compressors.Deflate, protobufSerializer)

    /**
     * Fory Serializer와 Deflate Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> deflateFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.DeflateFory)

    /**
     * Jdk Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Jdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Jdk)

    /**
     * Kryo Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Kryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Kryo)

    /**
     * Protobuf Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Protobuf(): LettuceBinaryCodec<V> = compressedCodec(Compressors.LZ4, protobufSerializer)

    /**
     * Fury Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> lz4Fory(): LettuceBinaryCodec<V> = codec(BinarySerializers.LZ4Fory)

    /**
     * Jdk Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyJdk)

    /**
     * Kryo Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyKryo)

    /**
     * Protobuf Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyProtobuf(): LettuceBinaryCodec<V> = compressedCodec(Compressors.Snappy, protobufSerializer)

    /**
     * Fory Serializer와 Snappy Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> snappyFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.SnappyFory)


    /**
     * Jdk Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdJdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdJdk)

    /**
     * Kryo Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdKryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdKryo)

    /**
     * Protobuf Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdProtobuf(): LettuceBinaryCodec<V> = compressedCodec(Compressors.Zstd, protobufSerializer)

    /**
     * Fory Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdFory)
}
