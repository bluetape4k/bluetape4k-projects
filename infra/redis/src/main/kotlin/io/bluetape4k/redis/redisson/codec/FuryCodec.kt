package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

/**
 * Fury 알고리즘으로 직렬화/역직렬화를 수행하는 Codec
 *
 * @see io.bluetape4k.io.serialize.FurySerializer
 * @see io.bluetape4k.io.serialize.BinarySerializers.Fury
 *
 * @property fallbackCodec  대체 Codec ([Kryo5Codec]) 인스턴스
 */
class FuryCodec @JvmOverloads constructor(
    private val fallbackCodec: Codec = RedissonCodecs.Kryo5,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Kryo5)
    constructor(classLoader: ClassLoader, codec: FuryCodec): this(copy(classLoader, codec.fallbackCodec))

    companion object: KLogging()

    private val fury get() = BinarySerializers.Fury

    private val encoder: Encoder = Encoder { graph ->
        try {
            val bytes = fury.serialize(graph)
            Unpooled.wrappedBuffer(bytes)
        } catch (e: Exception) {
            log.warn(e) { "Value is not suitable for FuryCodec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder<Any> { buf: ByteBuf, state: State ->
        try {
            val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
            fury.deserialize(bytes)
        } catch (e: Exception) {
            log.warn(e) { "Value is not suitable for FuryCodec. Using fallbackCodec[$fallbackCodec]" }
            fallbackCodec.valueDecoder.decode(buf, state)
        }
    }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder

}
