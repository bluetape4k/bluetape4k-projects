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
 * Redisson [Codec] implementation for Fastjson2 JSONB values.
 *
 * ## Wire format
 *
 * Encodes values as:
 * ```
 * [4-byte big-endian className length] + [className UTF-8 bytes] + [JSONB bytes]
 * ```
 *
 * The class name is validated before class loading, then JSONB is decoded with an explicit target type.
 * This wire format is not compatible with [io.bluetape4k.fastjson2.FastjsonSerializer].
 *
 * ## Trust boundary
 *
 * - `allowedPackagePrefixes = null` allows all class names and keeps fallback decode enabled.
 * - Set [allowedPackagePrefixes] for exposed or multi-tenant Redis boundaries.
 * - When [allowedPackagePrefixes] is set, fallback decode is disabled by default so non-JSONB binary
 *   payloads cannot bypass the allow-list. Set [allowFallbackDecode] only for trusted migration reads.
 *
 * Example:
 *   ```kotlin
 *   val codec = Fastjson2Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
 *   val codec = RedissonCodecs.fastjson2(setOf("com.mycompany.", "io.bluetape4k."))
 *   ```
 *
 * ## Limitations
 *
 * Root collection element types are not preserved. Wrap collections in DTOs when element type fidelity
 * matters.
 *
 * @property fallbackCodec fallback codec used for encode failures and trusted decode migrations.
 * @property classLoader class loader used by Redisson dynamic codec copies.
 * @property allowedPackagePrefixes package prefixes allowed before class loading, or null for trusted internal use.
 * @property allowFallbackDecode whether decode can fall back to [fallbackCodec] after JSONB envelope failure.
 */
class Fastjson2Codec(
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    private val allowedPackagePrefixes: Set<String>? = null,
    private val allowFallbackDecode: Boolean = allowedPackagePrefixes == null,
): BaseCodec() {

    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Fory, classLoader)

    constructor(classLoader: ClassLoader, codec: Fastjson2Codec): this(
        copy(classLoader, codec.fallbackCodec),
        classLoader,
        codec.allowedPackagePrefixes,
        codec.allowFallbackDecode,
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
            if (!allowFallbackDecode) {
                throw SecurityException(
                    "Fastjson2Codec fallback decode is disabled for allow-listed JSONB payloads. " +
                        "Rejecting non-Fastjson2 binary payload.",
                    e,
                )
            }
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
        "Fastjson2Codec(" +
            "fallback=${fallbackCodec.javaClass.simpleName}, " +
            "allowedPrefixes=$allowedPackagePrefixes, " +
            "allowFallbackDecode=$allowFallbackDecode" +
            ")"
}
