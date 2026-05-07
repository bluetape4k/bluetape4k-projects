package io.bluetape4k.logback.kafka.keyprovider

/**
 * 로그 이벤트에서 Kafka 메시지 키를 생성하는 전략 인터페이스입니다.
 *
 * ## 동작/계약
 * - null 반환 시 Kafka 기본 파티셔닝 정책(무키)에 따릅니다.
 * - 구현체는 동일 이벤트에 대해 결정적 키 생성 여부를 선택할 수 있습니다.
 *
 * ```kotlin
 * val key = provider.get(event)
 * // key == null || key.isNotEmpty()
 * ```
 */
interface KafkaKeyProvider<E: Any> {

    /**
     * 로그 Event를 기준으로 Key를 생성
     *
     * @param e Event 정보
     * @return Kafka Key 값
     */
    fun get(e: E): ByteArray?
}
