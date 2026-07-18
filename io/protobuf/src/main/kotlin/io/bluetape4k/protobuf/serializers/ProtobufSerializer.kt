package io.bluetape4k.protobuf.serializers

import io.bluetape4k.io.serializer.AbstractBinarySerializer
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.ProtoMessage
import io.bluetape4k.protobuf.packMessageTo
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

/**
 * Binary serializer for Protobuf messages packed as `Any`.
 *
 * ## Contract
 * - [ProtoMessage] inputs are encoded as `ProtoAny.pack(message).toByteArray()`.
 * - Decoding validates the class name from `Any.typeUrl` against [allowedClassPrefixes] before loading it.
 * - The default profile is strict: non-Protobuf values and non-Protobuf bytes are rejected.
 * - A non-null [fallback] enables the trusted-internal mixed Protobuf + fallback profile.
 * - [serializeTo] writes directly to a caller-owned buffer for Protobuf values; fallback values retain their
 *   allocating compatibility path. Its preflight failures are raw buffer exceptions and recovery restores position.
 * - [deserializeFrom] reads only the bounded remaining bytes through a duplicate and avoids this serializer's eager
 *   compatibility [ByteArray] copy; array-backed heap inputs may therefore avoid a serializer-owned copy. protobuf-java
 *   may still copy direct or read-only buffers internally, and any allocation benefit requires benchmark evidence.
 *   An explicitly configured trusted fallback receives a bounded [ByteArray] copy.
 *
 * ## Security notes
 * - Only class names matching [allowedClassPrefixes] are loaded from `Any.typeUrl`.
 * - `Class.forName` uses `initialize=false` to avoid running static initializers.
 * - Use [trustedInternalProtobuf] only when all producers and stored bytes are trusted.
 *
 * ```kotlin
 * // Strict shared-boundary profile.
 * val serializer = ProtobufSerializer()
 * val bytes = serializer.serialize(message)
 *
 * // Add an application Protobuf package.
 * val serializer = ProtobufSerializer(
 *     allowedClassPrefixes = setOf("io.bluetape4k.", "com.google.protobuf.", "com.example.proto.")
 * )
 * ```
 */
class ProtobufSerializer(
    private val fallback: BinarySerializer? = null,
    private val allowedClassPrefixes: Set<String> = DEFAULT_ALLOWED_PREFIXES,
): AbstractBinarySerializer() {
    private val messageClassResolver = ProtobufMessageClassResolver()

    init {
        require(allowedClassPrefixes.all { it.isNotBlank() }) {
            "allowedClassPrefixes must not contain blank entries."
        }
    }

    companion object {
        /**
         * Package prefixes allowed for class names extracted from Protobuf `Any.typeUrl`.
         */
        val DEFAULT_ALLOWED_PREFIXES: Set<String> = setOf(
            "io.bluetape4k.",
            "com.google.protobuf."
        )

        /**
         * Creates the trusted-internal mixed Protobuf + fallback serializer.
         *
         * Use this profile only for internal stores whose historical bytes may contain fallback-encoded payloads.
         */
        fun trustedInternalProtobuf(
            fallback: BinarySerializer = BinarySerializers.Kryo,
            allowedClassPrefixes: Set<String> = DEFAULT_ALLOWED_PREFIXES,
        ): ProtobufSerializer =
            ProtobufSerializer(fallback = fallback, allowedClassPrefixes = allowedClassPrefixes)
    }

    override fun doSerialize(graph: Any): ByteArray =
        if (graph is ProtoMessage) {
            ProtoAny.pack(graph).toByteArray()
        } else {
            fallback?.serialize(graph)
                ?: throw IllegalArgumentException(
                    "Strict Protobuf serializer can encode only Protobuf messages. " +
                        "Use ProtobufSerializer.trustedInternalProtobuf() for trusted fallback payloads."
                )
        }

    override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
        if (target.isReadOnly) throw ReadOnlyBufferException()
        if (graph == null) return 0

        val start = target.position()
        try {
            return if (graph is ProtoMessage) {
                packMessageTo(graph, target)
            } else {
                val bytes = serialize(graph)
                if (bytes.size > target.remaining()) throw BufferOverflowException()
                target.put(bytes)
                bytes.size
            }
        } catch (failure: Throwable) {
            target.position(start)
            when (failure) {
                is ReadOnlyBufferException,
                is BufferOverflowException,
                is BinarySerializationException,
                is Error -> throw failure
                else -> throw BinarySerializationException("Fail to serialize. graphType=${graph.javaClass.name}", failure)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        return decodeWithTrustedFallback(ByteBuffer.wrap(bytes)) { bytes }
    }

    override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
        if (!source.hasRemaining()) return null
        val size = source.remaining()
        return try {
            decodeWithTrustedFallback(source.duplicate()) {
                ByteArray(size).also { source.duplicate().get(it) }
            }
        } catch (failure: Error) {
            throw failure
        } catch (failure: Throwable) {
            throw BinarySerializationException("Fail to deserialize. bytesSize=${source.remaining()}", failure)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> decodeWithTrustedFallback(
        source: ByteBuffer,
        fallbackBytes: () -> ByteArray,
    ): T? =
        try {
            decodeProtobuf(source)
        } catch (failure: SecurityException) {
            throw failure
        } catch (failure: Error) {
            throw failure
        } catch (failure: Throwable) {
            val trustedFallback = fallback
                ?: throw SecurityException(
                    "Payload is not Protobuf Any and no trusted fallback serializer is configured.",
                    failure
                )
            log.debug(failure) { "Protobuf deserialization failed; delegating to the trusted fallback serializer." }
            trustedFallback.deserialize(fallbackBytes())
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> decodeProtobuf(source: ByteBuffer): T? {
        val protoAny = ProtoAny.parseFrom(source)
        val className = protoAny.typeUrl.substringAfterLast("/")
        validateClassName(className)
        val classLoader = Thread.currentThread().contextClassLoader ?: ProtobufSerializer::class.java.classLoader
        val clazz = messageClassResolver.resolve(className, classLoader)
        return protoAny.unpack(clazz) as? T
    }

    private fun validateClassName(className: String) {
        if (className.isBlank() || allowedClassPrefixes.none { className.matchesAllowedPrefix(it) }) {
            val cause = IllegalArgumentException(
                "Untrusted Protobuf class: $className. Add the package to allowedClassPrefixes."
            )
            throw SecurityException("Blocked Protobuf deserialization: ${cause.message}", cause)
        }
    }

    private fun String.matchesAllowedPrefix(prefix: String): Boolean =
        this == prefix || startsWith(prefix.ensurePackagePrefix())

    private fun String.ensurePackagePrefix(): String =
        if (endsWith(".")) this else "$this."
}
