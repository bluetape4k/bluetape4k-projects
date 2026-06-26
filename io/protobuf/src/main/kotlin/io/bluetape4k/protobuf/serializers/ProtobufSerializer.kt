package io.bluetape4k.protobuf.serializers

import io.bluetape4k.io.serializer.AbstractBinarySerializer
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.ProtoMessage
import io.bluetape4k.support.isNullOrEmpty
import java.util.concurrent.ConcurrentHashMap

/**
 * Binary serializer for Protobuf messages packed as `Any`.
 *
 * ## Contract
 * - [ProtoMessage] inputs are encoded as `ProtoAny.pack(message).toByteArray()`.
 * - Decoding validates the class name from `Any.typeUrl` against [allowedClassPrefixes] before loading it.
 * - The default profile is strict: non-Protobuf values and non-Protobuf bytes are rejected.
 * - A non-null [fallback] enables the trusted-internal mixed Protobuf + fallback profile.
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
    init {
        require(allowedClassPrefixes.all { it.isNotBlank() }) {
            "allowedClassPrefixes must not contain blank entries."
        }
    }

    companion object {
        private val messageTypes = ConcurrentHashMap<String, Class<out ProtoMessage>>()

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

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        if (bytes.isNullOrEmpty()) {
            return null
        }

        return try {
            val protoAny = ProtoAny.parseFrom(bytes)
            val className = protoAny.typeUrl.substringAfterLast("/")

            // Reject class names outside the configured allowlist before class loading.
            require(allowedClassPrefixes.any { className.matchesAllowedPrefix(it) }) {
                "Untrusted Protobuf class: $className. Add the package to allowedClassPrefixes."
            }

            val clazz =
                messageTypes.getOrPut(className) {
                    // initialize=false prevents static initializer execution while resolving the message class.
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(className, false, Thread.currentThread().contextClassLoader) as Class<ProtoMessage>
                }
            protoAny.unpack(clazz) as? T
        } catch (e: IllegalArgumentException) {
            throw SecurityException("Blocked Protobuf deserialization: ${e.message}", e)
        } catch (e: Throwable) {
            val trustedFallback = fallback
                ?: throw SecurityException(
                    "Payload is not Protobuf Any and no trusted fallback serializer is configured.",
                    e
                )
            log.debug(e) { "Protobuf deserialization failed; delegating to the trusted fallback serializer." }
            trustedFallback.deserialize(bytes)
        }
    }

    private fun String.matchesAllowedPrefix(prefix: String): Boolean =
        this == prefix || startsWith(prefix.ensurePackagePrefix())

    private fun String.ensurePackagePrefix(): String =
        if (endsWith(".")) this else "$this."
}
