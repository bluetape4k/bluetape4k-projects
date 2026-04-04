package io.bluetape4k.kafka.codec

import com.fasterxml.jackson.databind.json.JsonMapper
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.writeAsBytes
import io.bluetape4k.support.emptyByteArray
import org.apache.kafka.common.header.Headers

/**
 * Kafka 키나 메시지를 JSON으로 직렬화/역직렬화하는 Kafka Codec
 *
 * ```kotlin
 * val codec = JacksonKafkaCodec()
 * val data = mapOf("id" to 1, "name" to "debop")
 * val bytes = codec.serialize("my-topic", null, data)
 * val result = codec.deserialize("my-topic", null, bytes)
 * // result is a Map with id=1, name="debop"
 * ```
 *
 * @param mapper Jackson [JsonMapper] 인스턴스
 */
class JacksonKafkaCodec(
    private val mapper: JsonMapper = Jackson.defaultJsonMapper,
): AbstractKafkaCodec<Any?>() {

    override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray {
        return mapper.writeAsBytes(graph) ?: emptyByteArray
    }

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? {
        val clazz = getValueType(headers)
        return if (bytes.isEmpty()) null
        else mapper.readValue(bytes, clazz)
    }
}
