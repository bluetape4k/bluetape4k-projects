package io.bluetape4k.redis.redisson.codec

import com.google.protobuf.Message
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.netty.buffer.getBytes
import io.bluetape4k.redis.redisson.RedissonCodecs
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
 * Proto Buffer 객체를 Redisson 에서 사용하기 위한 Codec
 *
 * @property fallbackCodec 내부 코덱
 */
class ProtobufCodec(
    private val fallbackCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: ProtobufCodec): this(copy(classLoader, codec.fallbackCodec))

    companion object: KLogging() {
        private val classCache = ConcurrentHashMap<String, Class<Message>>()
    }

    private val encoder: Encoder = Encoder { graph ->
        if (graph is Message) {
            val bytes = AnyMessage.pack(graph).toByteArray()
            Unpooled.wrappedBuffer(bytes)
        } else {
            log.info { "Encoding: Value is not Protobuf Message instance. use fallbackCodec[$fallbackCodec] graph class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        try {
            val bytes = buf.getBytes(copy = false)
            val any = AnyMessage.parseFrom(bytes)
            val className = any.typeUrl.substringAfterLast("/")
            val clazz = classCache.computeIfAbsent(className) {
                Class.forName(it) as Class<Message>
            }
            any.unpack(clazz)
        } catch (e: Throwable) {
            log.info(e) { "Decoding: Fail to decode as Protobuf message. it is not Protobuf Message, use fallbackCodec[$fallbackCodec]" }
            fallbackCodec.valueDecoder.decode(Unpooled.wrappedBuffer(buf.resetReaderIndex()), state)
        }
    }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder
}
