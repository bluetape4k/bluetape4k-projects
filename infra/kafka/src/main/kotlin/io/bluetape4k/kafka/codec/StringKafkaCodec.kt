package io.bluetape4k.kafka.codec

import io.bluetape4k.logging.KLogging
import org.apache.kafka.common.header.Headers
import java.nio.charset.Charset

/**
 * Kafka 메시지의 Key 와 Value의 타입이 문자열인 경우에 사용하는 [KafkaCodec] 입니다.
 */
class StringKafkaCodec: AbstractKafkaCodec<String>() {

    companion object: KLogging() {
        @JvmField
        val DefaultEncoding = Charsets.UTF_8

        private const val SERIALIZER_ENCODING = "serializer.encoding"
        private const val KEY_SERIALIZER_ENCODING = "key.$SERIALIZER_ENCODING"
        private const val VALUE_SERIALIZER_ENCODING = "value.$SERIALIZER_ENCODING"

        private const val DESERIALIZER_ENCODING = "deserializer.encoding"
        private const val KEY_DESERIALIZER_ENCODING = "key.$SERIALIZER_ENCODING"
        private const val VALUE_DESERIALIZER_ENCODING = "value.$SERIALIZER_ENCODING"
    }

    private var serializerEncoding = DefaultEncoding
    private var deserializerEncoding = DefaultEncoding

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        configs?.run {
            serializerEncoding = getSerializerEncoding(this, isKey)
            deserializerEncoding = getDeserializerEncoding(this, isKey)
        }
    }

    override fun doSerialize(topic: String?, headers: Headers?, graph: String): ByteArray {
        return graph.toByteArray(serializerEncoding)
    }

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): String? {
        return if (bytes.isEmpty()) null else bytes.toString(deserializerEncoding)
    }

    private fun getSerializerEncoding(configs: Map<String, *>, isKey: Boolean): Charset {
        val propertyName = if (isKey) KEY_SERIALIZER_ENCODING else VALUE_SERIALIZER_ENCODING
        val encodingValue = configs[propertyName] ?: configs[SERIALIZER_ENCODING]

        return when (encodingValue) {
            is String -> runCatching { Charset.forName(encodingValue) }.getOrDefault(DefaultEncoding)
            else      -> DefaultEncoding
        }
    }

    private fun getDeserializerEncoding(configs: Map<String, *>, isKey: Boolean): Charset {
        val propertyName = if (isKey) KEY_DESERIALIZER_ENCODING else VALUE_DESERIALIZER_ENCODING
        val encodingValue = configs[propertyName] ?: configs[DESERIALIZER_ENCODING]

        return when (encodingValue) {
            is String -> runCatching { Charset.forName(encodingValue) }.getOrDefault(DefaultEncoding)
            else      -> DefaultEncoding
        }
    }
}
