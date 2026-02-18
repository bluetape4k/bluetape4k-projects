package io.bluetape4k.kafka.codec

import org.apache.kafka.common.header.Headers

/**
 * ByteArray를 직렬화/역직렬화하는 Kafka Codec입니다.
 *
 * 이 Codec은 데이터를 변환하지 않고 그대로 전달하므로,
 * 이미 직렬화된 바이너리 데이터를 Kafka로 전송할 때 유용합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val codec = ByteArrayKafkaCodec()
 * val bytes = "Hello".toByteArray()
 * val serialized = codec.serialize("topic", bytes)
 * val deserialized = codec.deserialize("topic", serialized)
 * ```
 *
 * @see KafkaCodecs.ByteArray
 */
class ByteArrayKafkaCodec: AbstractKafkaCodec<ByteArray>() {

    override fun doSerialize(
        topic: String?,
        headers: Headers?,
        graph: ByteArray,
    ): ByteArray = graph

    override fun doDeserialize(
        topic: String?,
        headers: Headers?,
        bytes: ByteArray,
    ): ByteArray = bytes
}
