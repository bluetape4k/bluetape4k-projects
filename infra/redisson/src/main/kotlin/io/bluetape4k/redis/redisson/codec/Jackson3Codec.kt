package io.bluetape4k.redis.redisson.codec

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
import tools.jackson.databind.json.JsonMapper

/**
 * Redisson [Codec] implementation for Jackson 3 JSON envelope values.
 *
 * ## Wire format
 *
 * Encodes values as a JSON envelope:
 * ```json
 * {"_type": "com.example.Foo", "_data": {...}}
 * ```
 * The `_type` field stores the FQCN and is validated before class loading.
 *
 * ## Trust boundary
 *
 * - `allowedPackagePrefixes = null` allows all class names and keeps fallback decode enabled.
 * - Set [allowedPackagePrefixes] for exposed or multi-tenant Redis boundaries.
 * - When [allowedPackagePrefixes] is set, fallback decode is disabled by default so non-JSON binary
 *   payloads cannot bypass the allow-list. Set [allowFallbackDecode] only for trusted migration reads.
 *
 * Example:
 *   ```kotlin
 *   val codec = Jackson3Codec(allowedPackagePrefixes = setOf("com.mycompany.", "io.bluetape4k."))
 *   val codec = RedissonCodecs.jackson3(setOf("com.mycompany.", "io.bluetape4k."))
 *   ```
 *
 * ## Limitations
 *
 * Root collection element types are not preserved. Wrap collections in DTOs when element type fidelity
 * matters.
 *
 * @property mapper Jackson 3 mapper.
 * @property fallbackCodec fallback codec used for encode failures and trusted decode migrations.
 * @property classLoader class loader used by Redisson dynamic codec copies.
 * @property allowedPackagePrefixes package prefixes allowed before class loading, or null for trusted internal use.
 * @property allowFallbackDecode whether decode can fall back to [fallbackCodec] after JSON envelope failure.
 */
class Jackson3Codec(
    private val mapper: JsonMapper = io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
    private val fallbackCodec: Codec = RedissonCodecs.Fory,
    private val classLoader: ClassLoader? = null,
    private val allowedPackagePrefixes: Set<String>? = null,
    private val allowFallbackDecode: Boolean = allowedPackagePrefixes == null,
): BaseCodec() {

    @Suppress("UNUSED_PARAMETER")
    constructor(classLoader: ClassLoader): this(
        io.bluetape4k.jackson3.Jackson.defaultJsonMapper,
        RedissonCodecs.Fory,
        classLoader,
    )

    constructor(classLoader: ClassLoader, codec: Jackson3Codec): this(
        codec.mapper,
        copy(classLoader, codec.fallbackCodec),
        classLoader,
        codec.allowedPackagePrefixes,
        codec.allowFallbackDecode,
    )

    companion object: KLogging() {
        private const val TYPE_FIELD = "_type"
        private const val DATA_FIELD = "_data"
    }

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

    private val encoder: Encoder = Encoder { graph ->
        try {
            val node = mapper.createObjectNode()
            node.put(TYPE_FIELD, graph.javaClass.name)
            node.set(DATA_FIELD, mapper.valueToTree(graph))
            val bytes = mapper.writeValueAsBytes(node)
            Unpooled.wrappedBuffer(bytes)
        } catch (e: Exception) {
            log.info(e) { "Encoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]. Value class=${graph.javaClass}" }
            fallbackCodec.valueEncoder.encode(graph)
        }
    }

    private val decoder: Decoder<Any> = Decoder { buf: ByteBuf, state: State? ->
        val bytes = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true)
        try {
            val tree = mapper.readTree(bytes)
            val typeNode = tree.get(TYPE_FIELD)
            if (typeNode != null && typeNode.isString) {
                val className = typeNode.asString()
                validateClassName(className)
                val cl = classLoader ?: Thread.currentThread().contextClassLoader ?: javaClass.classLoader
                val clazz = Class.forName(className, false, cl)
                mapper.treeToValue(tree.get(DATA_FIELD), clazz)
            } else {
                throw IllegalStateException("Missing or non-textual '$TYPE_FIELD' field in JSON envelope")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            if (!allowFallbackDecode) {
                throw SecurityException(
                    "Jackson3Codec fallback decode is disabled for allow-listed JSON payloads. " +
                        "Rejecting non-Jackson3 binary payload.",
                    e,
                )
            }
            log.info(e) { "Decoding failed for Jackson3Codec. Using fallbackCodec[$fallbackCodec]" }
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
        "Jackson3Codec(" +
            "fallback=${fallbackCodec.javaClass.simpleName}, " +
            "allowedPrefixes=$allowedPackagePrefixes, " +
            "allowFallbackDecode=$allowFallbackDecode" +
            ")"
}
