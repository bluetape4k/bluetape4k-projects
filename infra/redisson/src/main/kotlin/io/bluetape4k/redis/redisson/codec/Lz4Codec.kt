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
 * LZ4 알고리즘으로 값을 압축/복원하는 래퍼 Codec입니다.
 *
 * ## 배경
 * Redisson 공식 `LZ4CodecV2`에서 예외가 발생하는 문제를 해결하기 위해 자체 구현한 Codec입니다.
 *
 * ## 동작 방식
 * - 인코딩: [innerCodec]으로 직렬화한 바이트를 LZ4 알고리즘으로 압축하여 Redis에 저장합니다.
 * - 디코딩: Redis에서 읽은 LZ4 압축 데이터를 해제한 후 [innerCodec]으로 역직렬화합니다.
 *
 * ## 특징
 * - LZ4는 매우 빠른 압축/해제 속도를 제공하며, CPU 부하가 낮습니다.
 * - Gzip 대비 압축률은 낮지만, 처리 속도가 훨씬 빠릅니다.
 * - 고속 I/O가 필요한 캐시 환경에 적합합니다.
 *
 * ## 사용 예
 * ```kotlin
 * val codec = Lz4Codec(RedissonCodecs.Fory) // Fory 직렬화 + LZ4 압축
 * val config = Config()
 * config.codec = codec
 * ```
 *
 * @property innerCodec 직렬화/역직렬화에 사용할 내부 Codec (기본값: [RedissonCodecs.Default])
 */
class Lz4Codec(
    private val innerCodec: Codec = RedissonCodecs.Default,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this()
    constructor(classLoader: ClassLoader, codec: Lz4Codec): this(copy(classLoader, codec.innerCodec))

    companion object: KLogging()

    private val lz4 = Compressors.LZ4

    private val encoder: Encoder = Encoder { graph ->
        val encoded = innerCodec.valueEncoder.encode(graph)
        val bytes = ByteBufUtil.getBytes(encoded, encoded.readerIndex(), encoded.readableBytes(), true)
        encoded.release()

        Unpooled.wrappedBuffer(lz4.compress(bytes))
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        val decoded = Unpooled.wrappedBuffer(lz4.decompress(bytes))

        try {
            innerCodec.valueDecoder.decode(decoded, state)
        } finally {
            decoded.release()
        }
    }

    override fun getValueEncoder(): Encoder = encoder
    override fun getValueDecoder(): Decoder<Any> = decoder
}
