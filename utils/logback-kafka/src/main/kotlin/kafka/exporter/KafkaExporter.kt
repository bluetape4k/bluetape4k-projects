package io.bluetape4k.logback.kafka.exporter

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

interface KafkaExporter {

    fun <K: Any, V: Any, E: Any> export(
        producer: Producer<K, V>,
        record: ProducerRecord<K, V>,
        event: E,
        exceptionHandler: ExportExceptionHandler<E>,
    ): Boolean

}
