package io.bluetape4k.kafka.codec

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.support.emptyByteArray
import org.apache.kafka.common.header.Headers
import tools.jackson.databind.json.JsonMapper

/**
 * Kafka codec that serializes and deserializes messages as JSON using Jackson.
 *
 * ```kotlin
 * // Safe: only allow DTOs in your own package
 * val codec = JacksonKafkaCodec(allowedTypePackages = setOf("com.example.dto"))
 * val bytes = codec.serialize("my-topic", null, MyDto("hello"))
 * val result = codec.deserialize("my-topic", null, bytes) as MyDto
 *
 * // Unsafe legacy mode (allow any class from the header)
 * val legacyCodec = JacksonKafkaCodec(allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE)
 * ```
 *
 * @param mapper Jackson [JsonMapper] instance
 * @param allowedTypePackages package prefix allowlist for class loading from the type header;
 *   empty set (default) denies all — safe for untrusted topics
 */
class JacksonKafkaCodec(
    private val mapper: JsonMapper = Jackson.defaultJsonMapper,
    override val allowedTypePackages: Set<String> = emptySet(),
): AbstractKafkaCodec<Any?>() {

    override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray {
        return graph?.let { mapper.writeValueAsBytes(it) } ?: emptyByteArray
    }

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? {
        val clazz = getValueType(headers)
        return if (bytes.isEmpty()) null
        else mapper.readValue(bytes, clazz)
    }
}
