package io.bluetape4k.redis.redisson.codec

import com.alibaba.fastjson2.JSONB
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

/**
 * Fastjson2 JSONB 포맷으로 직렬화/역직렬화를 수행하는 Redisson [Codec] 구현체입니다.
 *
 * ## 직렬화 포맷
 * 다음 구조의 바이트 배열로 인코딩합니다:
 * ```
 * [4바이트 big-endian: className 길이] + [className UTF-8 바이트] + [JSONB 바이트]
 * ```
 * 클래스 이름을 헤더에 저장하고, JSONB는 명시적 타입(`JSONB.parseObject(bytes, clazz)`)으로 디코딩합니다.
 * `WriteClassName`/`SupportAutoType`의 TYPED_ANY 제한을 우회하며 모든 중첩 타입을 올바르게 복원합니다.
 *
 * 이 포맷은 [io.bluetape4k.fastjson2.FastjsonSerializer]와 **비호환**입니다. 혼용 시 역직렬화 오류가 발생합니다.
 *
 * ## 역직렬화 보안 (Pre-instantiation 검증)
 * 역직렬화는 헤더에서 읽은 클래스 이름을 [validateClassName]으로 검사한 뒤 클래스를 로드합니다.
 * [allowedPackagePrefixes]에 포함되지 않은 클래스는 로드 전에 [SecurityException]을 던집니다.
 *
 * ## 보안 경고
 * - `allowedPackagePrefixes = null`이면 모든 클래스를 허용합니다 (**신뢰된 내부 Redis 환경에서만 사용**).
 * - 외부에 노출된 Redis 또는 다중 테넌트 환경에서는 [allowedPackagePrefixes]를 반드시 지정하십시오:
 *   ```kotlin
 *   val codec = Fastjson2Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
 *   // 또는 factory 사용:
 *   val codec = RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))
 *   ```
 *
 * ## 제한사항
 * - 루트 타입이 `List`, `Map` 등 컬렉션인 경우 원소 타입 정보가 소실됩니다. DTO 래퍼로 감싸서 사용하십시오.
 * - 직렬화/역직렬화 실패 시 [fallbackCodec]으로 자동 전환합니다.
 *
 * @property fallbackCodec 직렬화/역직렬화 실패 시 사용할 대체 Codec (기본값: [RedissonCodecs.Fory])
 * @property classLoader 역직렬화 시 사용할 [ClassLoader] (Redisson 동적 생성용)
 * @property allowedPackagePrefixes 허용할 패키지 prefix 목록. null이면 모든 클래스 허용 (보안 주의)
 */
class Fastjson2Codec(
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    private val allowedPackagePrefixes: Set<String>? = null,
): BaseCodec() {

    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Fory, classLoader)

    constructor(classLoader: ClassLoader, codec: Fastjson2Codec): this(
        copy(classLoader, codec.fallbackCodec),
        classLoader,
        codec.allowedPackagePrefixes,
    )

    companion object: KLogging()

    private fun validateClassName(className: String) {
        if (allowedPackagePrefixes != null) {
            val allowed = allowedPackagePrefixes.any { className.startsWith(it) }
            if (!allowed) {
                throw SecurityException(
                    "Class '$className' is not in the allowed package list. " +
                    "Allowed prefixes: $allowedPackagePrefixes"
                )
            }
        }
    }

    private fun resolveClass(className: String): Class<*> {
        val loader = classLoader ?: Thread.currentThread().contextClassLoader ?: javaClass.classLoader
        return loader.loadClass(className)
    }

    private val encoder: Encoder = Encoder { graph ->
        try {
            val classNameBytes = graph.javaClass.name.toByteArray(Charsets.UTF_8)
            val jsonbBytes = JSONB.toBytes(graph)
            // 포맷: [4바이트 className 길이] + [className 바이트] + [JSONB 바이트]
            val buf = Unpooled.buffer(4 + classNameBytes.size + jsonbBytes.size)
            buf.writeInt(classNameBytes.size)
            buf.writeBytes(classNameBytes)
            buf.writeBytes(jsonbBytes)
            buf
        } catch (e: Exception) {
            log.info(e) { "Encoding failed for Fastjson2Codec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        try {
            if (bytes.size < 4) throw IllegalArgumentException("Invalid Fastjson2Codec format: bytes too short")
            val classNameLen = ((bytes[0].toInt() and 0xFF) shl 24) or
                ((bytes[1].toInt() and 0xFF) shl 16) or
                ((bytes[2].toInt() and 0xFF) shl 8) or
                (bytes[3].toInt() and 0xFF)
            if (classNameLen <= 0 || classNameLen > bytes.size - 4) {
                throw IllegalArgumentException("Invalid Fastjson2Codec format: classNameLen=$classNameLen")
            }
            val className = String(bytes, 4, classNameLen, Charsets.UTF_8)
            validateClassName(className)  // pre-instantiation 보안 검증
            val clazz = resolveClass(className)
            val jsonbBytes = bytes.copyOfRange(4 + classNameLen, bytes.size)
            JSONB.parseObject(jsonbBytes, clazz)
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            log.info(e) { "Decoding failed for Fastjson2Codec. Using fallbackCodec[$fallbackCodec]" }
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

    override fun toString(): String =
        "Fastjson2Codec(fallback=${fallbackCodec.javaClass.simpleName}, allowedPrefixes=$allowedPackagePrefixes)"
}
