package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.io.getAllBytes
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.ToByteBufEncoder
import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

/**
 * Lettuce [RedisCodec] 구현체로, Value를 [JsonSerializer]를 이용하여 JSON 포맷으로 직렬화/역직렬화합니다.
 *
 * [LettuceBinaryCodec]과 달리 역직렬화 시 대상 타입([valueType])을 명시적으로 지정하므로,
 * FQCN을 바이트 스트림에 임베딩하지 않아도 타입 안전 역직렬화가 가능합니다.
 *
 * ## 주요 특징
 * - Key: UTF-8 인코딩 String
 * - Value: [serializer]가 생성하는 JSON 바이트 배열 (UTF-8 텍스트 JSON 또는 JSONB)
 * - [valueType]을 [JsonSerializer.deserialize]에 직접 전달하므로 AutoType 클래스 로드 불필요
 *
 * ## 제한사항
 * - 루트 타입이 `List<Foo>` 등 제네릭 컬렉션인 경우 타입 정보가 소실됩니다.
 *   DTO 래퍼로 감싸거나 [LettuceBinaryCodec] 을 대신 사용하세요.
 *
 * ```kotlin
 * val codec = LettuceJsonCodec(JacksonSerializer(), CustomData::class.java)
 * val client = RedisClient.create("redis://localhost:6379")
 * val connection = client.connect(codec)
 * connection.sync().set("key", myData)
 * val value = connection.sync().get("key")
 * // value == myData
 * ```
 *
 * @param V value type
 * @property serializer JSON 직렬화/역직렬화를 수행하는 [JsonSerializer] 인스턴스
 * @property valueType 역직렬화 대상 클래스 타입
 */
class LettuceJsonCodec<V: Any>(
    val serializer: JsonSerializer,
    val valueType: Class<V>,
): RedisCodec<String, V>, ToByteBufEncoder<String, V> {

    companion object: KLogging() {
        val EMPTY_BYTEBUFFER: ByteBuffer = ByteBuffer.allocate(0)
    }

    override fun encodeKey(key: String?): ByteBuffer =
        key?.run { ByteBuffer.wrap(this.toUtf8Bytes()) } ?: EMPTY_BYTEBUFFER

    override fun encodeKey(key: String?, target: ByteBuf) {
        key?.run { target.writeBytes(this.toUtf8Bytes()) }
    }

    override fun encodeValue(value: V): ByteBuffer =
        ByteBuffer.wrap(serializer.serialize(value))

    /**
     * Encodes [value] into the caller-owned [target].
     *
     * null target은 no-op입니다. 읽기 전용 target은 serializer dispatch 전에 실패하며 관찰 가능한 상태를 보존합니다
     * indices, marks, and reference count. Success commits the writer index only after the complete wire is written;
     * failure may leave attempted content or capacity changes but does not commit the writer index.
     */
    override fun encodeValue(value: V, target: ByteBuf?) {
        if (target == null) return
        if (target.isReadOnly) throw ReadOnlyBufferException()

        val output = BoundedByteBufOutputStream(target)
        try {
            val reported = serializer.serializeJsonToStream(value, output)
            val actual = output.writtenBytes()
            check(reported == actual) {
                "Serializer reported $reported bytes but wrote $actual bytes."
            }
            output.verifySnapshot()
            target.writerIndex(Math.addExact(output.startIndex(), actual))
        } finally {
            output.seal()
        }
    }

    override fun decodeKey(bytes: ByteBuffer?): String? =
        bytes?.getAllBytes()?.toUtf8String()

    override fun decodeValue(bytes: ByteBuffer?): V? =
        bytes?.let { serializer.deserializeFrom(it.boundedReadView(), valueType) }

    /**
     * 키 또는 값의 직렬화 크기를 추정합니다.
     *
     * JSON 직렬화 결과 크기는 인코딩 전에 알 수 없으므로 V 타입은 `-1`을 반환하여
     * Netty가 동적으로 버퍼를 확장하도록 합니다.
     * (실제 직렬화를 두 번 수행하는 성능 손실을 방지하기 위해 추정을 포기합니다.)
     *
     * - String: UTF-8 인코딩된 바이트 크기
     * - ByteArray: 배열 크기
     * - ByteBuffer: 남은 바이트 수
     * - V 타입: -1 (Netty가 동적으로 확장)
     */
    override fun estimateSize(keyOrValue: Any?): Int = when (keyOrValue) {
        null          -> 0
        is String     -> keyOrValue.toUtf8Bytes().size
        is ByteArray  -> keyOrValue.size
        is ByteBuffer -> keyOrValue.remaining()
        else          -> -1
    }

    override fun toString(): String =
        "LettuceJsonCodec(serializer=${serializer.javaClass.simpleName}, valueType=${valueType.simpleName})"
}

private fun ByteBuffer.boundedReadView(): ByteBuffer =
    duplicate().slice().asReadOnlyBuffer().order(order())
