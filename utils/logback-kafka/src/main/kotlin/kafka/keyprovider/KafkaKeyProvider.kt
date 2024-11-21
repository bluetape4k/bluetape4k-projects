package io.bluetape4k.logback.kafka.keyprovider

interface KafkaKeyProvider<E> {

    /**
     * 로그 Event를 기준으로 Key를 생성
     *
     * @param e Event 정보
     * @return Kafka Key 값
     */
    fun get(e: E): ByteArray?
}
