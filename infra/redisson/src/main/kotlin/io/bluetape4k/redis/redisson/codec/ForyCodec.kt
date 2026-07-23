package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import org.redisson.codec.Kryo5Codec

/**
 * A Redisson [Codec] backed by Apache Fory.
 *
 * Single-NIO heap/direct input is decoded from a bounded read-only view without changing caller-owned `ByteBuf`
 * indices, marks, or reference count. Composite/non-NIO input and unsafe view construction use the copied
 * compatibility path. Fory's internal reusable buffer remains, so the direct view is not a zero-copy codec.
 *
 * Encode and decode failures retain the existing [fallbackCodec] contract. Existing callers need no API or payload
 * migration. This raw-codec behavior does not apply to compression wrappers.
 *
 * **Security warning:** The default registration-off decoder is intended only for trusted Redis payloads.
 * It does not provide a secure deserialization boundary for untrusted input.
 *
 * ## Example
 * ```kotlin
 * val config = Config()
 * config.codec = ForyCodec()
 * val redisson = Redisson.create(config)
 * ```
 *
 * @property fallbackCodec codec used after a supported Fory failure (default: [Kryo5Codec])
 * @see io.bluetape4k.io.serializer.ForyBinarySerializer
 * @see io.bluetape4k.io.serializer.BinarySerializers.Fory
 */
class ForyCodec(
    private val fallbackCodec: Codec = RedissonCodecs.Kryo5,
): BaseCodec() {

    private var runtime = ForyCodecRuntime(serializerFactory = { BinarySerializers.Fory })

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Kryo5)
    constructor(classLoader: ClassLoader, codec: ForyCodec): this(copy(classLoader, codec.fallbackCodec)) {
        runtime = codec.runtime
    }

    companion object: KLogging() {
        internal fun create(
            fallbackCodec: Codec = RedissonCodecs.Kryo5,
            runtime: ForyCodecRuntime,
        ): ForyCodec = ForyCodec(fallbackCodec).also { it.runtime = runtime }
    }

    private val fory by lazy { runtime.serializerFactory() }

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
        val directView = tryReadOnlyReadableNioView(buf, runtime.readableViewFactory)
        if (directView == null) {
            val bytes = runtime.copiedBytesFactory(buf)
            try {
                fory.deserialize<Any>(bytes)
            } catch (e: Exception) {
                log.info(e) { "Decoding: Value is not suitable for ForyCodec. Using fallbackCodec[$fallbackCodec]" }
                decodeWithFallbackBuffer(bytes, runtime.fallbackBufferFactory) { fallbackBuf ->
                    fallbackCodec.valueDecoder.decode(fallbackBuf, state)
                }
            }
        } else {
            try {
                fory.deserializeDirectWithLegacyNormalization(directView, buf.readableBytes())
            } catch (e: Exception) {
                log.info(e) { "Decoding: Value is not suitable for ForyCodec. Using fallbackCodec[$fallbackCodec]" }
                val bytes = runtime.copiedBytesFactory(buf)
                decodeWithFallbackBuffer(bytes, runtime.fallbackBufferFactory) { fallbackBuf ->
                    fallbackCodec.valueDecoder.decode(fallbackBuf, state)
                }
            }
        }
    }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder

}
