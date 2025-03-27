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
 * Gzip 알고리즘으로 값을 압축/복원하는 Codec
 */
class GzipCodec @JvmOverloads constructor(
    private val innerCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: GzipCodec): this(copy(classLoader, codec.innerCodec))

    companion object: KLogging()

    private val gzip get() = Compressors.GZip

    private val encoder: Encoder = Encoder { graph ->
        val encoded = innerCodec.valueEncoder.encode(graph)
        val bytes = ByteBufUtil.getBytes(encoded, encoded.readerIndex(), encoded.readableBytes(), true)
        encoded.release()
        val res = gzip.compress(bytes)
        Unpooled.wrappedBuffer(res)
    }

    private val decoder: Decoder<Any> = Decoder { byf: ByteBuf, state: State ->
        val bytes = ByteBufUtil.getBytes(byf, byf.readerIndex(), byf.readableBytes(), true)
        val plainBytes = gzip.decompress(bytes)
        val decoded = Unpooled.wrappedBuffer(plainBytes)

        try {
            innerCodec.valueDecoder.decode(decoded, state)
        } finally {
            decoded.release()
        }
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}
