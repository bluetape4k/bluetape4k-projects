package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.CodedOutputStream
import com.google.protobuf.Message
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.netty.buffer.getBytes
import io.bluetape4k.protobuf.serializers.ProtobufMessageClassResolver
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

typealias AnyMessage = com.google.protobuf.Any

internal fun releaseOwnedBuffer(copied: ByteBuf, operationFailure: Throwable?) {
    try {
        while (copied.refCnt() > 0) copied.release()
    } catch (cleanupFailure: Throwable) {
        operationFailure?.addSuppressed(cleanupFailure) ?: throw cleanupFailure
    }
}

/**
 * Redisson codec for Protobuf messages.
 *
 * ## Contract
 * - [com.google.protobuf.Message] values are encoded directly into a Netty [ByteBuf].
 * - During decoding, the class name from `Any.typeUrl` is validated against [allowedClassPrefixes]
 *   before class loading.
 * - The default profile is strict: non-Protobuf values and bytes are rejected.
 * - A non-null [fallbackCodec] enables the trusted-internal mixed Protobuf + fallback profile.
 * - A trust-boundary violation throws [SecurityException] instead of falling back silently.
 * - A trusted fallback must finish synchronously and must not hide its temporary input in an arbitrary object graph
 *   or transfer that input to another thread.
 *
 * ```kotlin
 * val codec = RedissonProtobufCodec()
 * // Register the codec in Redisson Config to store Protobuf messages.
 * ```
 *
 * @property fallbackCodec trusted fallback codec for non-Protobuf payloads, or `null` for strict mode.
 * @property allowedClassPrefixes package prefixes allowed for Protobuf `Any.typeUrl` class names.
 */
class RedissonProtobufCodec private constructor(
    private val fallbackCodec: Codec? = null,
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

    constructor(): this(null, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, null)

    constructor(
        fallbackCodec: Codec,
    ): this(fallbackCodec, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, null)

    constructor(
        allowedClassPrefixes: Set<String>,
    ): this(null, allowedClassPrefixes, null)

    constructor(
        fallbackCodec: Codec,
        allowedClassPrefixes: Set<String>,
    ): this(fallbackCodec, allowedClassPrefixes, null)

    // Redisson requires class-loader constructors for dynamic codec creation from configuration.
    constructor(classLoader: ClassLoader): this(null, ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES, classLoader)
    constructor(classLoader: ClassLoader, codec: RedissonProtobufCodec): this(
        fallbackCodec = codec.fallbackCodec?.let { copy(classLoader, it) },
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

        /**
         * Creates a trusted-internal mixed Protobuf + fallback codec.
         *
         * Use this profile only for internal Redis stores whose historical bytes may contain fallback-encoded payloads.
         */
        fun trustedInternal(
            fallbackCodec: Codec = RedissonCodecs.Kryo5,
            allowedClassPrefixes: Set<String> = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES,
        ): RedissonProtobufCodec =
            RedissonProtobufCodec(fallbackCodec, allowedClassPrefixes, null)
    }

    private val messageClassResolver = ProtobufMessageClassResolver()

    private val encoder: Encoder =
        Encoder { graph ->
            if (graph is Message) {
                encodeProtobufMessage(graph)
            } else {
                val trustedFallback = fallbackCodec
                    ?: throw IllegalArgumentException(
                        "Strict Protobuf codec can encode only Protobuf messages. " +
                            "Use RedissonProtobufCodec.trustedInternal() for trusted fallback payloads."
                    )
                log.debug {
                    "Encoding: Protobuf Message가 아닙니다. fallbackCodec[$fallbackCodec] 사용. graph class=${graph.javaClass}"
                }
                trustedFallback.valueEncoder.encode(graph)
            }
        }

    private val decoder: Decoder<Any> =
        Decoder { buf: ByteBuf, state: State? ->
            try {
                decodeProtobuf(buf)
            } catch (failure: SecurityException) {
                throw failure
            } catch (failure: Error) {
                throw failure
            } catch (failure: Throwable) {
                decodeFallback(buf, state, failure)
            }
        }

    override fun getValueEncoder(): Encoder = encoder

    override fun getValueDecoder(): Decoder<Any> = decoder

    private fun decodeProtobuf(buf: ByteBuf): Any {
        val any = if (buf.nioBufferCount() == 1) {
            AnyMessage.parseFrom(buf.nioBuffer(buf.readerIndex(), buf.readableBytes()))
        } else {
            AnyMessage.parseFrom(buf.getBytes(copy = true))
        }
        val className = any.typeUrl.substringAfterLast("/")
        validateClassName(className)
        val effectiveLoader =
            classLoader
                ?: Thread.currentThread().contextClassLoader
                ?: RedissonProtobufCodec::class.java.classLoader
        val clazz = messageClassResolver.resolve(className, effectiveLoader)
        return any.unpack(clazz)
    }

    private fun decodeFallback(buf: ByteBuf, state: State?, failure: Throwable): Any {
        val trustedFallback = fallbackCodec
            ?: throw SecurityException(
                "Payload is not Protobuf Any and no trusted fallback codec is configured.",
                failure,
            )
        log.debug(failure) {
            "Decoding: Protobuf 메시지가 아닙니다. fallbackCodec[$trustedFallback] 사용."
        }
        val copied = Unpooled.wrappedBuffer(buf.getBytes(copy = true))
        var operationFailure: Throwable? = null
        return try {
            trustedFallback.valueDecoder.decode(copied, state).also { result ->
                if (copied.refCnt() != 1) {
                    throw SecurityException("Trusted fallback changed the owned input reference count.")
                }
                if (result is ByteBuf && result.references(copied)) {
                    throw SecurityException("Trusted fallback returned a view of its temporary input.")
                }
            }
        } catch (caught: Throwable) {
            operationFailure = caught
            throw caught
        } finally {
            releaseOwnedBuffer(copied, operationFailure)
        }
    }

    private fun ByteBuf.references(root: ByteBuf): Boolean {
        val visited = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<ByteBuf, Boolean>()
        )
        var current: ByteBuf? = this
        while (current != null) {
            if (current === root) return true
            if (!visited.add(current)) return true
            current = current.unwrap()
        }
        return false
    }

    internal fun encodeProtobufMessage(message: Message): ByteBuf {
        val any = AnyMessage.pack(message)
        val buffer = Unpooled.buffer(any.serializedSize)
        return try {
            val nioBuffer = buffer.nioBuffer(0, any.serializedSize)
            val output = CodedOutputStream.newInstance(nioBuffer)
            any.writeTo(output)
            output.flush()
            buffer.writerIndex(nioBuffer.position())
            buffer
        } catch (e: Throwable) {
            buffer.release()
            throw e
        }
    }

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
