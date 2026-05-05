package io.bluetape4k.kafka.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import org.apache.kafka.common.header.Headers
import java.nio.charset.Charset

/**
 * Kafka 메시지의 Key 와 Value의 타입이 문자열인 경우에 사용하는 [KafkaCodec] 입니다.
 *
 * ```kotlin
 * val codec = StringKafkaCodec()
 * val bytes = codec.serialize("my-topic", "hello")
 * val result = codec.deserialize("my-topic", bytes)
 * // result == "hello"
 * ```
 */
class StringKafkaCodec: AbstractKafkaCodec<String>() {
    companion object: KLogging() {
        @JvmField
        val DefaultEncoding = Charsets.UTF_8

        private const val SERIALIZER_ENCODING = "serializer.encoding"
        private const val KEY_SERIALIZER_ENCODING = "key.$SERIALIZER_ENCODING"
        private const val VALUE_SERIALIZER_ENCODING = "value.$SERIALIZER_ENCODING"

        private const val DESERIALIZER_ENCODING = "deserializer.encoding"
        private const val KEY_DESERIALIZER_ENCODING = "key.$DESERIALIZER_ENCODING"
        private const val VALUE_DESERIALIZER_ENCODING = "value.$DESERIALIZER_ENCODING"
    }

    private var serializerEncoding = DefaultEncoding
    private var deserializerEncoding = DefaultEncoding

    override fun configure(
        configs: MutableMap<String, *>?,
        isKey: Boolean,
    ) {
        configs?.run {
            serializerEncoding = getSerializerEncoding(this, isKey)
            deserializerEncoding = getDeserializerEncoding(this, isKey)
        }
    }

    override fun doSerialize(
        topic: String?,
        headers: Headers?,
        graph: String,
    ): ByteArray = graph.toByteArray(serializerEncoding)

    override fun doDeserialize(
        topic: String?,
        headers: Headers?,
        bytes: ByteArray,
    ): String? = if (bytes.isEmpty()) null else bytes.toString(deserializerEncoding)

    private fun getSerializerEncoding(
        configs: Map<String, *>,
        isKey: Boolean,
    ): Charset {
        val propertyName = if (isKey) KEY_SERIALIZER_ENCODING else VALUE_SERIALIZER_ENCODING
        val encodingValue = configs[propertyName] ?: configs[SERIALIZER_ENCODING]
        return resolveCharsetOrDefault(encodingValue, propertyName)
    }

    private fun getDeserializerEncoding(
        configs: Map<String, *>,
        isKey: Boolean,
    ): Charset {
        val propertyName = if (isKey) KEY_DESERIALIZER_ENCODING else VALUE_DESERIALIZER_ENCODING
        val encodingValue = configs[propertyName] ?: configs[DESERIALIZER_ENCODING]
        return resolveCharsetOrDefault(encodingValue, propertyName)
    }

    /**
     * 설정값을 [Charset] 으로 변환한다. 잘못된 charset 명이면 [DefaultEncoding] 으로 fallback 하되,
     * silent fallback 으로 producer/consumer 인코딩 불일치(mojibake) 가 디버깅 불가능해지지 않도록 WARN 을 남긴다.
     */
    private fun resolveCharsetOrDefault(value: Any?, propertyName: String): Charset =
        when (value) {
            is String -> runCatching { Charset.forName(value) }
                .onFailure { e ->
                    log.warn(e) {
                        "Invalid charset name. property=$propertyName, value=$value. " +
                            "Falling back to ${DefaultEncoding.name()}. Producer/consumer 인코딩 불일치 위험을 확인하세요."
                    }
                }
                .getOrDefault(DefaultEncoding)
            else -> DefaultEncoding
        }
}
