package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.compressor.Compressors
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
 * Gzip 알고리즘으로 값을 압축/복원하는 래퍼 Codec입니다.
 *
 * ## 동작 방식
 * - 인코딩: [innerCodec]으로 직렬화한 바이트를 Gzip으로 압축하여 Redis에 저장합니다.
 * - 디코딩: Redis에서 읽은 Gzip 압축 데이터를 해제한 후 [innerCodec]으로 역직렬화합니다.
 *
 * ## 특징
 * - 압축률이 높아 저장 공간을 효율적으로 절감할 수 있습니다.
 * - 단, Gzip은 압축/해제 속도가 LZ4나 Zstd에 비해 느립니다. 고속 처리가 필요한 경우 [Lz4Codec]이나 [ZstdCodec]을 권장합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val codec = GzipCodec(RedissonCodecs.Kryo5) // Kryo5 직렬화 + Gzip 압축
 * val config = Config()
 * config.codec = codec
 * ```
 *
 * @property innerCodec 직렬화/역직렬화에 사용할 내부 Codec (기본값: [RedissonCodecs.Default])
 */
class GzipCodec(
    private val innerCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: GzipCodec): this(copy(classLoader, codec.innerCodec))

    companion object: KLogging()

    private val gzip = Compressors.GZip

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
