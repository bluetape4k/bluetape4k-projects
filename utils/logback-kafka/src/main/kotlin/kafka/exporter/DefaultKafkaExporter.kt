package io.bluetape4k.logback.kafka.exporter

import org.apache.kafka.clients.producer.BufferExhaustedException
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.TimeoutException

class DefaultKafkaExporter: KafkaExporter {

    override fun <K, V, E> export(
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
