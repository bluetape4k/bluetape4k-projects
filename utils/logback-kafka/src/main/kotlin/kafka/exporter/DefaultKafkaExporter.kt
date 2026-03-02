package io.bluetape4k.logback.kafka.exporter

import org.apache.kafka.clients.producer.BufferExhaustedException
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TimeoutException

/**
 * 기본 Kafka 전송 구현체입니다.
 *
 * ## 동작/계약
 * - [Producer.send] 비동기 전송을 사용하고 콜백 예외를 [exceptionHandler]로 전달합니다.
 * - 즉시 발생한 [BufferExhaustedException], [TimeoutException]은 handler 호출 후 `false`를 반환합니다.
 *
 * ```kotlin
 * val exported = DefaultKafkaExporter().export(producer, record, event, handler)
 * // exported == true || exported == false
 * ```
 */
class DefaultKafkaExporter: KafkaExporter {

    override fun <K: Any, V: Any, E: Any> export(
        producer: Producer<K, V>,
        record: ProducerRecord<K, V>,
        event: E,
        exceptionHandler: ExportExceptionHandler<E>,
    ): Boolean {
        return try {
            producer.send(record) { _, exception ->
                if (exception != null) {
                    exceptionHandler.handle(event, exception)
                }
            }
            return true
        } catch (e: Throwable) {
            if (e is BufferExhaustedException || e is TimeoutException) {
                exceptionHandler.handle(event, e)
            }
            false
        }
    }
}
