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
 * @param V value type
 * @property serializer [BinarySerializer] 인스턴스
 */
class LettuceBinaryCodec<V: Any>(
    private val serializer: BinarySerializer,
): RedisCodec<String, V>, ToByteBufEncoder<String, V> {

    companion object: KLogging() {
        val EMPTY_BYTEBUFFER: ByteBuffer = ByteBuffer.allocate(0)
    }

    override fun encodeKey(key: String?): ByteBuffer {
        return key?.run { ByteBuffer.wrap(this.toUtf8Bytes()) } ?: EMPTY_BYTEBUFFER
    }

    override fun encodeKey(key: String?, target: ByteBuf) {
        key?.run { target.writeBytes(this.toUtf8Bytes()) }
    }

    override fun encodeValue(value: V): ByteBuffer {
        return ByteBuffer.wrap(serializer.serialize(value))
    }

    override fun encodeValue(value: V, target: ByteBuf?) {
        target?.run { writeBytes(serializer.serialize(value)) }
    }

    override fun decodeKey(bytes: ByteBuffer?): String? {
        return bytes?.getAllBytes()?.toUtf8String()
    }

    override fun decodeValue(bytes: ByteBuffer?): V? {
        return bytes?.getAllBytes()?.run { serializer.deserialize(this) }
    }

    /**
     * 키 또는 값의 직렬화 크기를 추정합니다.
     * - String: UTF-8 인코딩된 바이트 크기를 반환합니다
     * - ByteArray: 배열의 바이트 크기를 반환합니다
     * - V 타입: serializer를 통해 직렬화된 바이트 크기를 반환합니다
     * - 그 외: 기본값 0을 반환합니다
     */
    override fun estimateSize(keyOrValue: Any?): Int {
        return when (keyOrValue) {
            is String -> keyOrValue.toUtf8Bytes().size
            is ByteArray -> keyOrValue.size
            else -> 0
        }
    }

    override fun toString(): String {
        return "LettuceBinaryCodec(serializer=${serializer.javaClass.simpleName})"
    }
}
