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
 * Apache Fory `SCHEMA_CONSISTENT` 모드(FastFory)로 직렬화/역직렬화를 수행하는 Redisson [Codec] 구현체입니다.
 *
 * ## 특징
 * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하여 기본 [ForyCodec](COMPATIBLE 모드)보다 더 빠른 직렬화 속도를 제공합니다.
 * - 직렬화 실패 시 [fallbackCodec]([ForyCodec])으로 자동 전환하여 안정성을 보장합니다.
 * - 순환 참조 추적이 비활성화(`refTracking=false`)되어 있어 단순 객체 그래프에 최적화되어 있습니다.
 *
 * ## 사용 예
 * ```kotlin
 * val config = Config()
 * config.codec = FastForyCodec()
 * val redisson = Redisson.create(config)
 * ```
 *
 * ## ⚠️ 와이어 포맷 경고
 * - `CompatibleMode.SCHEMA_CONSISTENT`를 사용하며, 기본 Fory codec과 **와이어 포맷이 상호 비호환**합니다.
 * - **비대칭 호환성**: 이 codec은 구 Fory(COMPATIBLE) 데이터를 fallback으로 읽을 수 있습니다. 반대(ForyCodec으로 FastFory 데이터 읽기)는 불가합니다.
 * - **휘발성 캐시(Redis, 메모리 캐시) 전용** — 영속 저장에 사용하지 마십시오.
 * - **순환 참조 객체 불가** (refTracking=false).
 * - **스키마 진화 불가** — 필드 추가/제거 시 기존 데이터 역직렬화 실패.
 *
 * **Security warning:** The default registration-off decoder is intended only for trusted Redis payloads.
 * It does not provide a secure deserialization boundary for untrusted input.
 *
 * @property fallbackCodec 직렬화/역직렬화 실패 시 사용할 대체 Codec (기본값: [RedissonCodecs.Fory])
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
