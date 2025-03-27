package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
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
 * Zstd 알고리즘으로 압축을 수행하는 Codec
 */
class ZstdCodec @JvmOverloads constructor(
    private val innerCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: ZstdCodec): this(copy(classLoader, codec.innerCodec))

    companion object: KLogging()

    private val zstd get() = Compressors.Zstd

    private val encoder: Encoder = Encoder { graph ->
        val encoded = innerCodec.valueEncoder.encode(graph)
        val bytes = ByteBufUtil.getBytes(encoded, encoded.readerIndex(), encoded.readableBytes(), true)
        encoded.release()

        Unpooled.wrappedBuffer(zstd.compress(bytes))
    }

    private val decoder: Decoder<Any> = Decoder<Any> { buf: ByteBuf, state: State ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        val decoded = Unpooled.wrappedBuffer(zstd.decompress(bytes))

        try {
            innerCodec.valueDecoder.decode(decoded, state)
        } finally {
            decoded.release()
        }
    }


    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}
