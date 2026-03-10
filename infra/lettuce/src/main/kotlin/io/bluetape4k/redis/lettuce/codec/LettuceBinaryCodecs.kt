package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers

/**
 * 다양한 Serializer/Compressor 조합의 [LettuceBinaryCodec] 팩토리 모음.
 *
 * Protobuf 기반 Codec은 [LettuceProtobufCodecs]를 사용하세요.
 */
object LettuceBinaryCodecs {

    val Default: LettuceBinaryCodec<Any> by lazy { lz4Fory() }

    fun <V: Any> codec(serializer: BinarySerializer): LettuceBinaryCodec<V> =
        LettuceBinaryCodec(serializer)

    /**
     * Jdk Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> jdk(): LettuceBinaryCodec<V> = codec(BinarySerializers.Jdk)

    /**
     * Kryo Serializer를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> kryo(): LettuceBinaryCodec<V> = codec(BinarySerializers.Kryo)

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
     * Fory Serializer와 LZ4 Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
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
     * Fory Serializer와 Zstd Compressor를 사용하는 [LettuceBinaryCodec]를 생성합니다.
     */
    fun <V: Any> zstdFory(): LettuceBinaryCodec<V> = codec(BinarySerializers.ZstdFory)
}
