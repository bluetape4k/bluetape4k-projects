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
 * Zstd(Zstandard) 알고리즘으로 값을 압축/복원하는 래퍼 Codec입니다.
 *
 * ## 동작 방식
 * - 인코딩: [innerCodec]으로 직렬화한 바이트를 Zstd 알고리즘으로 압축하여 Redis에 저장합니다.
 * - 디코딩: Redis에서 읽은 Zstd 압축 데이터를 해제한 후 [innerCodec]으로 역직렬화합니다.
 *
 * ## 특징
 * - Zstd는 LZ4보다 높은 압축률을 제공하면서도 Gzip보다 빠른 속도를 제공합니다.
 * - 압축률과 속도의 균형이 뛰어나 대용량 데이터 캐싱에 적합합니다.
 * - Facebook이 개발한 오픈소스 압축 알고리즘으로, 다양한 압축 레벨을 지원합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val codec = ZstdCodec(RedissonCodecs.Fory) // Fory 직렬화 + Zstd 압축
 * val config = Config()
 * config.codec = codec
 * ```
 *
 * @property innerCodec 직렬화/역직렬화에 사용할 내부 Codec (기본값: [RedissonCodecs.Default])
 */
class ZstdCodec(
    private val innerCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: ZstdCodec): this(copy(classLoader, codec.innerCodec))

    companion object: KLogging()

    private val zstd = Compressors.Zstd

    private val encoder: Encoder = Encoder { graph ->
        val encoded = innerCodec.valueEncoder.encode(graph)
        val bytes = ByteBufUtil.getBytes(encoded, encoded.readerIndex(), encoded.readableBytes(), true)
        encoded.release()

        Unpooled.wrappedBuffer(zstd.compress(bytes))
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
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
