package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.getAllBytes
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.ToByteBufEncoder
import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer

/**
 * Lettuce [RedisCodec] 구현체
 * Value 를 [BinarySerializer]를 이용하여 직렬화/역직렬화합니다. (압축 기능도 제공합니다)
 *
 * ```kotlin
 * val codec = LettuceBinaryCodec<MyData>(BinarySerializers.LZ4Kryo)
 * val client = RedisClient.create("redis://localhost:6379")
 * val connection = client.connect(codec)
 * connection.sync().set("key", myData)
 * val value = connection.sync().get("key")
 * // value == myData
 * ```
 *
 * @param V value type
 * @property serializer [BinarySerializer] 인스턴스
 */
open class LettuceBinaryCodec<V: Any>(
    val serializer: BinarySerializer,
): RedisCodec<String, V>, ToByteBufEncoder<String, V> {

    companion object: KLogging() {
        val EMPTY_BYTEBUFFER: ByteBuffer = ByteBuffer.allocate(0)
    }

    final override fun encodeKey(key: String?): ByteBuffer {
        return key?.run { ByteBuffer.wrap(this.toUtf8Bytes()) } ?: EMPTY_BYTEBUFFER
    }

    final override fun encodeKey(key: String?, target: ByteBuf) {
        key?.run { target.writeBytes(this.toUtf8Bytes()) }
    }

    final override fun encodeValue(value: V): ByteBuffer {
        return ByteBuffer.wrap(serializer.serialize(value))
    }

    /**
     * Encodes [value] into the caller-owned [target].
     *
     * A null target is a no-op. The caller owns the target and its lifetime. A successful override must commit the
     * writer index only after the complete value has been written. A failed override may leave attempted bytes or
     * capacity changes behind, but it must not advance the writer index. Subclasses are responsible for preserving
     * wire-format and security compatibility with their configured [serializer].
     */
    override fun encodeValue(value: V, target: ByteBuf?) {
        target?.run { writeBytes(serializer.serialize(value)) }
    }

    final override fun decodeKey(bytes: ByteBuffer?): String? {
        return bytes?.getAllBytes()?.toUtf8String()
    }

    final override fun decodeValue(bytes: ByteBuffer?): V? {
        return bytes?.getAllBytes()?.run { serializer.deserialize(this) }
    }

    /**
     * 키 또는 값의 직렬화 크기를 추정합니다.
     *
     * 개선: 기존엔 V 타입에 대해 `serializer.serialize(value).size` 로 실제 직렬화를 수행했는데,
     *       Lettuce 는 실제 인코딩 시 [encodeValue] 를 다시 호출하므로 put 한 번당 직렬화가 2 회 일어나는
     *       심각한 성능 문제가 있었습니다 (LZ4/Zstd/Kryo 는 직렬화 비용이 큼).
     *       → V 타입은 크기 추정을 포기하고 -1 을 반환하여 Netty 가 기본 버퍼를 할당하도록 합니다.
     *
     * - String: UTF-8 인코딩된 바이트 크기
     * - ByteArray: 배열 크기
     * - ByteBuffer: 남은 바이트 수
     * - V 타입: -1 (estimate 불가 표시, Netty 가 동적으로 확장)
     */
    final override fun estimateSize(keyOrValue: Any?): Int = when (keyOrValue) {
        null          -> 0
        is String     -> keyOrValue.toUtf8Bytes().size
        is ByteArray  -> keyOrValue.size
        is ByteBuffer -> keyOrValue.remaining()
        else          -> -1
    }

    final override fun toString(): String {
        return "LettuceBinaryCodec(serializer=${serializer.javaClass.simpleName})"
    }
}
