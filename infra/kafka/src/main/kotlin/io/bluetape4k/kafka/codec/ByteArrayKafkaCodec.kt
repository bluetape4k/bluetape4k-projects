package io.bluetape4k.kafka.codec

import org.apache.kafka.common.header.Headers

/**
 * 직렬화를 ByteArray로 수행하는 Kafka Codec
 */
class ByteArrayKafkaCodec: AbstractKafkaCodec<ByteArray>() {

    override fun doSerialize(topic: String?, headers: Headers?, graph: ByteArray): ByteArray {
        return graph
    }

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): ByteArray {
        return bytes
    }
}
