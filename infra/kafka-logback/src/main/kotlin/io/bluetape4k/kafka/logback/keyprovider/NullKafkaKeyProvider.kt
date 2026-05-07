package io.bluetape4k.kafka.logback.keyprovider

/**
 * Kafka Key로 항상 null을 제공하여, 임의의 partition으로 발송하도록 합니다.
 */
class NullKafkaKeyProvider: io.bluetape4k.kafka.logback.keyprovider.KafkaKeyProvider<Any> {

    override fun get(e: Any): ByteArray? = null

}
