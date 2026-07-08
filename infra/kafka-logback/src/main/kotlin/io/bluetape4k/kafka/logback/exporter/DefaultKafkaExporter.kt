package io.bluetape4k.kafka.logback.exporter

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

/**
 * Default Kafka exporter implementation.
 *
 * ## Contract
 * - Uses asynchronous [Producer.send] and forwards callback exceptions to [exceptionHandler].
 * - Handles synchronous non-fatal [Exception] failures with [exceptionHandler] and returns `false`.
 * - Does not catch [Error]; fatal errors propagate to the caller.
 *
 * ```kotlin
 * val exported = DefaultKafkaExporter().export(producer, record, event, handler)
 * // exported == true || exported == false
 * ```
 */
class DefaultKafkaExporter: io.bluetape4k.kafka.logback.exporter.KafkaExporter {

    override fun <K: Any, V: Any, E: Any> export(
        producer: Producer<K, V>,
        record: ProducerRecord<K, V>,
        event: E,
        exceptionHandler: io.bluetape4k.kafka.logback.exporter.ExportExceptionHandler<E>,
    ): Boolean {
        return try {
            producer.send(record) { _, exception ->
                if (exception != null) {
                    exceptionHandler.handle(event, exception)
                }
            }
            return true
        } catch (e: Exception) {
            exceptionHandler.handle(event, e)
            false
        }
    }
}
