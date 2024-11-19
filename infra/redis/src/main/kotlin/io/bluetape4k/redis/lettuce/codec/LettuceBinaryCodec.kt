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

    override fun estimateSize(keyOrValue: Any?): Int {
        return when (keyOrValue) {
            is String    -> keyOrValue.length
            is ByteArray -> keyOrValue.size
            else         -> 0
        }
    }

    override fun toString(): String {
        return "LettuceBinaryCodec(serializer=${serializer.javaClass.simpleName})"
    }
}
