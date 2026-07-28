package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.compressor.GZipCompressor
import io.bluetape4k.logging.KLogging
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

/**
 * 다른 Redisson [Codec]을 GZip compression으로 감싸는 codec입니다.
 *
 * ## 동작
 * - Encoding: [innerCodec]으로 serialize한 뒤 GZip으로 압축한 bytes를 Redis에 저장합니다.
 * - Decoding: [maxDecompressedSize] 경계 안에서 Redis bytes를 압축 해제한 뒤 [innerCodec]에 위임합니다.
 *
 * GZip은 압축률이 높지만 LZ4나 Zstd보다 느립니다. 처리량이 중요한 cache path에서는 [Lz4Codec]이나
 * [ZstdCodec]을 우선 사용합니다.
 *
 * ```kotlin
 * val codec = GzipCodec(
 *     innerCodec = RedissonCodecs.Kryo5,
 *     maxDecompressedSize = 64 * 1024 * 1024,
 * )
 * val config = Config()
 * config.codec = codec
 * ```
 *
 * @property innerCodec serialize와 deserialize에 사용할 delegate codec입니다.
 * @property maxDecompressedSize 압축 해제된 Redis value가 허용할 수 있는 최대 byte 크기입니다.
 */
class GzipCodec @JvmOverloads constructor(
    private val innerCodec: Codec = RedissonCodecs.Default,
    val maxDecompressedSize: Int = GZipCompressor.DEFAULT_MAX_DECOMPRESSED_SIZE,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: GzipCodec): this(
        innerCodec = copy(classLoader, codec.innerCodec),
        maxDecompressedSize = codec.maxDecompressedSize,
    )

    companion object: KLogging()

    private val gzip = GZipCompressor(maxDecompressedSize = maxDecompressedSize)

    private val encoder: Encoder = Encoder { graph ->
        val encoded = innerCodec.valueEncoder.encode(graph)
        val bytes = ByteBufUtil.getBytes(encoded, encoded.readerIndex(), encoded.readableBytes(), true)
        encoded.release()

        Unpooled.wrappedBuffer(gzip.compress(bytes))
    }

    private val decoder: Decoder<Any> = Decoder { byf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(byf, byf.readerIndex(), byf.readableBytes(), true)
        val decoded = Unpooled.wrappedBuffer(gzip.decompress(bytes))

        try {
            innerCodec.valueDecoder.decode(decoded, state)
        } finally {
            decoded.release()
        }
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}
