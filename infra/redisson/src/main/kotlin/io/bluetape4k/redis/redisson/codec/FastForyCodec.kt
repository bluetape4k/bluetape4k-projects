package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

/**
 * A Redisson [Codec] backed by Apache Fory in `SCHEMA_CONSISTENT` mode.
 *
 * Single-NIO heap/direct input is decoded from a bounded read-only view. Composite/non-NIO input and unsafe view
 * construction use the copied compatibility path. Fory's internal reusable buffer remains, so this is not a
 * zero-copy codec. This raw-codec behavior does not apply to compression wrappers.
 *
 * FastFory encode/decode failures retain the existing [fallbackCodec] contract. This codec can therefore read
 * compatible-mode Fory payloads through fallback, while [ForyCodec] cannot read FastFory payloads. Existing callers
 * need no migration when they keep the same codec mode.
 *
 * ## Example
 * ```kotlin
 * val config = Config()
 * config.codec = FastForyCodec()
 * val redisson = Redisson.create(config)
 * ```
 *
 * ## Wire-format warning
 * - `SCHEMA_CONSISTENT` is not wire-compatible with the default Fory `COMPATIBLE` mode.
 * - Use this codec only for volatile caches with a fixed schema and no cyclic references.
 *
 * **Security warning:** The default registration-off decoder is intended only for trusted Redis payloads.
 * It does not provide a secure deserialization boundary for untrusted input.
 *
 * @property fallbackCodec codec used after a supported FastFory failure (default: [RedissonCodecs.Fory])
 * @see io.bluetape4k.io.serializer.FastForyBinarySerializer
 * @see io.bluetape4k.io.serializer.BinarySerializers.FastFory
 */
class FastForyCodec(
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
): BaseCodec() {

    private var runtime = ForyCodecRuntime(serializerFactory = { BinarySerializers.FastFory })

    // classLoader를 인자로 받는 보조 생성자는 Redisson에서 환경설정 정보를 바탕으로 동적으로 Codec 생성 시에 필요합니다.
    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Fory)
    constructor(classLoader: ClassLoader, codec: FastForyCodec): this(copy(classLoader, codec.fallbackCodec)) {
        runtime = codec.runtime
    }

    companion object: KLogging() {
        internal fun create(
            fallbackCodec: Codec = RedissonCodecs.Fory,
            runtime: ForyCodecRuntime,
        ): FastForyCodec = FastForyCodec(fallbackCodec).also { it.runtime = runtime }
    }

    private val fory by lazy { runtime.serializerFactory() }

    private val encoder: Encoder = Encoder { graph ->
        try {
            val bytes = fory.serialize(graph)
            Unpooled.wrappedBuffer(bytes)
        } catch (e: RuntimeException) {
            // 직렬화 실패 시 fallback — 마이그레이션 기간에는 흔한 경로이므로 debug 레벨
            log.debug(e) { "FastFory encode 실패, fallbackCodec[$fallbackCodec]으로 재시도. class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val directView = tryReadOnlyReadableNioView(buf, runtime.readableViewFactory)
        if (directView == null) {
            val bytes = runtime.copiedBytesFactory(buf)
            try {
                fory.deserialize<Any>(bytes)
            } catch (e: RuntimeException) {
                // 역직렬화 실패 시 fallback — 기존 Fory 포맷 데이터 읽기 경로
                log.debug(e) { "FastFory decode 실패, fallbackCodec[$fallbackCodec]으로 재시도" }
                decodeWithFallbackBuffer(bytes, runtime.fallbackBufferFactory) { fallbackBuf ->
                    fallbackCodec.valueDecoder.decode(fallbackBuf, state)
                }
            }
        } else {
            try {
                fory.deserializeDirectWithLegacyNormalization(directView, buf.readableBytes())
            } catch (e: RuntimeException) {
                // 역직렬화 실패 시 fallback — 기존 Fory 포맷 데이터 읽기 경로
                log.debug(e) { "FastFory decode 실패, fallbackCodec[$fallbackCodec]으로 재시도" }
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
