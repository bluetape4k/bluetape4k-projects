package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.redisson.codec.Kryo5Codec

/**
 * Apache Fory 알고리즘으로 직렬화/역직렬화를 수행하는 Redisson [Codec] 구현체입니다.
 *
 * ## 특징
 * - Apache Fory는 JDK 직렬화나 Kryo5 대비 2~10배 빠른 직렬화 속도를 제공합니다.
 * - 스키마 없이 대부분의 Java/Kotlin 타입을 자동으로 처리합니다.
 * - 직렬화 실패 시 [fallbackCodec]([Kryo5Codec])으로 자동 전환하여 안정성을 보장합니다.
 *
 * ## 주의사항
 * - Fory 직렬화는 클래스 구조 변경에 민감합니다. 클래스를 변경할 경우 기존 데이터와 호환성 문제가 발생할 수 있습니다.
 * - 직렬화 대상 클래스가 Fory에서 지원되지 않으면 [fallbackCodec]으로 처리됩니다.
 *
 * ## 사용 예
 * ```kotlin
 * val config = Config()
 * config.codec = ForyCodec()
 * val redisson = Redisson.create(config)
 * ```
 *
 * @property fallbackCodec 직렬화/역직렬화 실패 시 사용할 대체 Codec (기본값: [Kryo5Codec])
 * @see io.bluetape4k.io.serializer.ForyBinarySerializer
 * @see io.bluetape4k.io.serializer.BinarySerializers.Fory
 */
class ForyCodec(
    private val fallbackCodec: Codec = RedissonCodecs.Kryo5,
): BaseCodec() {

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Kryo5)
    constructor(classLoader: ClassLoader, codec: ForyCodec): this(copy(classLoader, codec.fallbackCodec))

    companion object: KLogging()

    private val fory by lazy { BinarySerializers.Fory }

    private val encoder: Encoder = Encoder { graph ->
        try {
            val bytes = fory.serialize(graph)
            Unpooled.wrappedBuffer(bytes)
        } catch (e: Exception) {
            log.info(e) { "Encoding: Value is not suitable for ForyCodec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        try {
            fory.deserialize(bytes)
        } catch (e: Exception) {
            log.info(e) { "Decoding: Value is not suitable for ForyCodec. Using fallbackCodec[$fallbackCodec]" }
            val fallbackBuf = Unpooled.wrappedBuffer(bytes)
            try {
                fallbackCodec.valueDecoder.decode(fallbackBuf, state)
            } finally {
                fallbackBuf.release()
            }
        }
    }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder

}
