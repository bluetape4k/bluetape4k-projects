package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.CodedOutputStream
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.CompressableBinarySerializer
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.ProtoMessage
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.netty.buffer.ByteBuf
import java.io.OutputStream
import java.util.Objects

/**
 * Protobuf Serializer를 사용하는 [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec] 팩토리 모음.
 *
 * Protobuf가 classpath에 있을 때만 이 object를 참조하세요.
 * [io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs]와 달리 Protobuf 의존성이 없으면 클래스 로드 시 [NoClassDefFoundError]가 발생합니다.
 */
object LettuceProtobufCodecs {

    private val strictSerializer: ProtobufSerializer by lazy { ProtobufSerializer() }
    private val trustedInternalSerializer: ProtobufSerializer by lazy { ProtobufSerializer.trustedInternalProtobuf() }

    private fun interface PackedAnyWriter {
        fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int
    }

    private object AbsolutePackedAnyWriter: PackedAnyWriter {
        override fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int =
            writePackedAny(packed, target, start, end)
    }

    private class DirectProtobufLettuceCodec<V: Any> private constructor(
        serializer: ProtobufSerializer,
        private val writer: PackedAnyWriter = AbsolutePackedAnyWriter,
    ): LettuceBinaryCodec<V>(serializer) {

        companion object {
            fun <V: Any> create(serializer: ProtobufSerializer): LettuceBinaryCodec<V> =
                DirectProtobufLettuceCodec(serializer)
        }

        override fun encodeValue(value: V, target: ByteBuf?) {
            if (target == null) return
            if (value !is ProtoMessage) return super.encodeValue(value, target)

            val packed = try {
                ProtoAny.pack(value)
            } catch (failure: Throwable) {
                throw BinarySerializationException(
                    "Fail to serialize. graphType=${value.javaClass.name}",
                    failure,
                )
            }
            val size = packed.serializedSize
            val start = target.writerIndex()
            target.ensureWritable(size)
            val written = writer.write(packed, target, start, start + size)
            check(written == size) {
                "Packed Any writer wrote $written bytes, expected $size"
            }
            target.writerIndex(start + size)
        }
    }

    private class BoundedByteBufOutputStream(
        private val target: ByteBuf,
        private val start: Int,
        private val end: Int,
    ): OutputStream() {
        var written: Int = 0
            private set

        init {
            require(start >= 0) { "start must not be negative" }
            require(end >= start) { "end must not precede start" }
            require(end <= target.capacity()) { "end exceeds target capacity" }
        }

        override fun write(value: Int) {
            checkWritable(1)
            target.setByte(start + written, value)
            written++
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            Objects.checkFromIndexSize(offset, length, bytes.size)
            checkWritable(length)
            target.setBytes(start + written, bytes, offset, length)
            written += length
        }

        private fun checkWritable(length: Int) {
            if (length > end - start - written) {
                throw IndexOutOfBoundsException(
                    "write exceeds bounded ByteBuf range [$start, $end)",
                )
            }
        }
    }

    private fun writePackedAny(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int {
        val output = BoundedByteBufOutputStream(target, start, end)
        val coded = CodedOutputStream.newInstance(output, 0)
        packed.writeTo(coded)
        coded.flush()
        return output.written
    }

    /**
     * Creates a strict Protobuf codec optimized for a direct ByteBuf target.
     *
     * The target remains caller-owned. This removes the payload-sized handoff for uncompressed Protobuf values but
     * does not promise zero-copy operation. The unchanged ByteBuffer API and all compressed or custom-prefix codecs
     * retain their compatibility path. On failure, failure-aftercare belongs to the caller because attempted bytes or
     * capacity growth may remain even though the writer index is not advanced.
     * The default allowlist accepts Bluetape and Google Protobuf message packages. Callers that need another package
     * prefix must construct [ProtobufSerializer] and [LettuceBinaryCodec] directly; that custom-prefix path retains
     * the allocating compatibility implementation.
     *
     * ```kotlin
     * val codec = LettuceProtobufCodecs.protobuf<MyBluetapeMessage>()
     * // codec != null
     * ```
     */
    fun <V: Any> protobuf(): LettuceBinaryCodec<V> =
        DirectProtobufLettuceCodec.create(strictSerializer)

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
     * Creates a trusted-internal mixed Protobuf + Kryo fallback codec optimized for a direct ByteBuf target.
     *
     * Use this factory only for internal stores where every producer and stored payload is trusted. Do not use it at
     * a shared or untrusted boundary.
     *
     * The target is caller-owned and has the same failure-aftercare contract as [protobuf]. The unchanged ByteBuffer
     * API, compressed factories, and any caller-configured custom-prefix serializer retain the compatibility path.
     */
    fun <V: Any> trustedInternalProtobuf(): LettuceBinaryCodec<V> =
        DirectProtobufLettuceCodec.create(trustedInternalSerializer)

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
