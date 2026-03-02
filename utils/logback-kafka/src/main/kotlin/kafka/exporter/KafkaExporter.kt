package io.bluetape4k.logback.kafka.exporter

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

/**
 * Kafka producer를 이용해 로그 이벤트를 내보내는 전략 인터페이스입니다.
 *
 * ## 동작/계약
 * - [export] 성공 시 `true`, 즉시 실패 시 `false`를 반환합니다.
 * - 비동기 전송 콜백 오류는 [exceptionHandler]로 전달됩니다.
 *
 * ```kotlin
 * val ok = exporter.export(producer, record, event, handler)
 * // ok == true || ok == false
 * ```
 */
interface KafkaExporter {

    /** 이벤트를 Kafka로 전송합니다. */
    fun <K: Any, V: Any, E: Any> export(
        producer: Producer<K, V>,
        record: ProducerRecord<K, V>,
        event: E,
        exceptionHandler: ExportExceptionHandler<E>,
    ): Boolean

}
