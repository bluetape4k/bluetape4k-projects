package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.Message
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.netty.buffer.getBytes
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.util.concurrent.ConcurrentHashMap

typealias AnyMessage = com.google.protobuf.Any

/**
 * Protobuf 메시지를 Redisson에서 사용하기 위한 Codec입니다.
 *
 * ## 동작/계약
 * - [com.google.protobuf.Message] 인스턴스는 `Any.pack(message).toByteArray()`로 직렬화합니다.
 * - 역직렬화 시 `typeUrl`의 클래스명을 기반으로 Message 타입을 캐시해 언패킹합니다.
 * - Protobuf 경로 실패 시 [fallbackCodec]에 위임합니다.
 *
 * ```kotlin
 * val codec = RedissonProtobufCodec()
 * // Redisson Config에 codec을 등록하면 Protobuf 직렬화가 적용됩니다.
 * ```
 *
 * @property fallbackCodec Protobuf 처리 실패 시 사용하는 fallback Codec입니다.
 */
class RedissonProtobufCodec(
    private val fallbackCodec: Codec = RedissonCodecs.Jdk,
): BaseCodec() {
    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: RedissonProtobufCodec): this(copy(classLoader, codec.fallbackCodec))

    companion object: KLogging() {
        private val classCache = ConcurrentHashMap<String, Class<Message>>()
    }

    private val encoder: Encoder =
        Encoder { graph ->
            if (graph is Message) {
                val bytes = AnyMessage.pack(graph).toByteArray()
                Unpooled.wrappedBuffer(bytes)
            } else {
                log.debug {
                    "Encoding: Protobuf Message가 아닙니다. fallbackCodec[$fallbackCodec] 사용. graph class=${graph.javaClass}"
                }
                fallbackCodec.valueEncoder.encode(graph)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private val decoder: Decoder<Any> =
        Decoder { buf: ByteBuf, state: State? ->
            try {
                val bytes = buf.getBytes(copy = false)
                val any = AnyMessage.parseFrom(bytes)
                val className = any.typeUrl.substringAfterLast("/")
                val clazz =
                    classCache.computeIfAbsent(className) {
                        Class.forName(it) as Class<Message>
                    }
                any.unpack(clazz)
            } catch (e: Throwable) {
                log.debug(e) {
                    "Decoding: Protobuf 메시지가 아닙니다. fallbackCodec[$fallbackCodec] 사용."
                }
                fallbackCodec.valueDecoder.decode(Unpooled.wrappedBuffer(buf.resetReaderIndex()), state)
            }
        }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder
}
