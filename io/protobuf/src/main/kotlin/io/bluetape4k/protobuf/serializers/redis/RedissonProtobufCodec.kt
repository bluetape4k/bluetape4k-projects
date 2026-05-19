package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.Message
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.netty.buffer.getBytes
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.util.concurrent.ConcurrentHashMap

typealias AnyMessage = com.google.protobuf.Any

/**
 * Redisson codec for Protobuf messages.
 *
 * ## Contract
 * - [com.google.protobuf.Message] values are encoded with `Any.pack(message).toByteArray()`.
 * - During decoding, the class name from `Any.typeUrl` is validated against [allowedClassPrefixes]
 *   before class loading.
 * - Non-Protobuf payloads are delegated to [fallbackCodec].
 * - A trust-boundary violation throws [SecurityException] instead of falling back silently.
 *
 * ```kotlin
 * val codec = RedissonProtobufCodec()
 * // Register the codec in Redisson Config to store Protobuf messages.
 * ```
 *
 * @property fallbackCodec fallback codec for non-Protobuf payloads.
 * @property allowedClassPrefixes package prefixes allowed for Protobuf `Any.typeUrl` class names.
 */
class RedissonProtobufCodec private constructor(
    private val fallbackCodec: Codec = RedissonCodecs.Kryo5,
    private val allowedClassPrefixes: Set<String> = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES,
    private val classLoader: ClassLoader? = null,
): BaseCodec() {
    init {
        if (allowedClassPrefixes != ALLOW_ALL_CLASSES_UNSAFE) {
            require(allowedClassPrefixes.all { it.isNotBlank() }) {
                "allowedClassPrefixes must not contain blank entries."
            }
        }
    }

    constructor(): this(RedissonCodecs.Kryo5, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, null)

    constructor(
        fallbackCodec: Codec,
    ): this(fallbackCodec, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, null)

    constructor(
        allowedClassPrefixes: Set<String>,
    ): this(RedissonCodecs.Kryo5, allowedClassPrefixes, null)

    constructor(
        fallbackCodec: Codec,
        allowedClassPrefixes: Set<String>,
    ): this(fallbackCodec, allowedClassPrefixes, null)

    // Redisson requires class-loader constructors for dynamic codec creation from configuration.
    constructor(classLoader: ClassLoader): this(RedissonCodecs.Kryo5, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, classLoader)
    constructor(classLoader: ClassLoader, codec: RedissonProtobufCodec): this(
        fallbackCodec = copy(classLoader, codec.fallbackCodec),
        allowedClassPrefixes = codec.allowedClassPrefixes,
        classLoader = classLoader,
    )

    companion object: KLogging() {
        /**
         * Explicit migration profile that restores legacy allow-all Protobuf class loading.
         *
         * Use only for fully trusted internal Redis deployments while migrating to an allowlist.
         */
        val ALLOW_ALL_CLASSES_UNSAFE: Set<String> = setOf("*")
    }

    private val classCache = ConcurrentHashMap<String, Class<Message>>()

    private val encoder: Encoder =
        Encoder { graph ->
            if (graph is Message) {
                val bytes = AnyMessage.pack(graph).toByteArray()
                Unpooled.wrappedBuffer(bytes)
            } else {
                log.debug {
                    "Encoding: Protobuf Message가 아닙니다. fallbackCodec[$fallbackCodec] 사용. graph class=${graph.javaClass}"
                }
                fallbackCodec.valueEncoder.encode(graph)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private val decoder: Decoder<Any> =
        Decoder { buf: ByteBuf, state: State? ->
            try {
                val bytes = buf.getBytes(copy = false)
                val any = AnyMessage.parseFrom(bytes)
                val className = any.typeUrl.substringAfterLast("/")
                validateClassName(className)
                val clazz =
                    classCache.computeIfAbsent(className) {
                        Class.forName(it, false, classLoader ?: Thread.currentThread().contextClassLoader)
                            as Class<Message>
                    }
                any.unpack(clazz)
            } catch (e: SecurityException) {
                throw e
            } catch (e: Throwable) {
                log.debug(e) {
                    "Decoding: Protobuf 메시지가 아닙니다. fallbackCodec[$fallbackCodec] 사용."
                }
                fallbackCodec.valueDecoder.decode(Unpooled.wrappedBuffer(buf.resetReaderIndex()), state)
            }
        }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder

    private fun validateClassName(className: String) {
        if (className.isBlank()) {
            throw SecurityException("Protobuf Any typeUrl does not contain a class name.")
        }
        if (
            allowedClassPrefixes != ALLOW_ALL_CLASSES_UNSAFE &&
            allowedClassPrefixes.none { className.matchesAllowedPrefix(it) }
        ) {
            throw SecurityException("Untrusted Protobuf class: $className. Add the package to allowedClassPrefixes.")
        }
    }

    private fun String.matchesAllowedPrefix(prefix: String): Boolean =
        this == prefix || startsWith(prefix.ensurePackagePrefix())

    private fun String.ensurePackagePrefix(): String =
        if (endsWith(".")) this else "$this."
}
