package io.bluetape4k.kafka.spring.core.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.awaitSingle
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.Metric
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.PartitionInfo
import org.springframework.beans.factory.DisposableBean
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.converter.MessagingMessageConverter
import org.springframework.kafka.support.converter.RecordMessageConverter
import org.springframework.messaging.Message
import reactor.core.publisher.Flux
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kafka.sender.SenderResult
import reactor.kafka.sender.TransactionManager
import java.io.Closeable

/**
 * Coroutine 환경에서 사용하는 Kafka ProducerTemplate 기능을 제공합니다.
 *
 * @param K 키 타입
 * @param V 값 타입
 * @property sender [KafkaSender] instance.
 * @property messageConverter [RecordMessageConverter] instance.
 *
 * @see [org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate]
 */
class SuspendKafkaProducerTemplate<K, V> private constructor(
    private val sender: KafkaSender<K, V>,
    private val messageConverter: RecordMessageConverter,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()), Closeable, DisposableBean {

    companion object: KLoggingChannel() {
        /**
         * Create an instance of [SuspendKafkaProducerTemplate] with the provided configuration.
         *
         * @param senderOptions [SenderOptions] instance.
         * @param messageConverter [RecordMessageConverter] instance.
         * @return [SuspendKafkaProducerTemplate] instance.
         */
        @JvmStatic
        operator fun <K, V> invoke(
            senderOptions: SenderOptions<K, V>,
            messageConverter: RecordMessageConverter = MessagingMessageConverter(),
        ): SuspendKafkaProducerTemplate<K, V> {
            return SuspendKafkaProducerTemplate(KafkaSender.create(senderOptions), messageConverter)
        }

        @JvmStatic
        operator fun <K, V> invoke(
            sender: KafkaSender<K, V>,
            messageConverter: RecordMessageConverter = MessagingMessageConverter(),
        ): SuspendKafkaProducerTemplate<K, V> {
            return SuspendKafkaProducerTemplate(sender, messageConverter)
        }
    }

    fun <T> sendTransactionally(records: Flow<SenderRecord<K, V, T>>): Flow<SenderResult<T>> {
        val result = sender.sendTransactionally(Flux.just(records.asFlux()))
        return result.flatMap { it }.asFlow()
    }

    suspend fun <T> sendTransactionally(record: SenderRecord<K, V, T>): SenderResult<T> {
        return sendTransactionally(flowOf(record)).first()
    }

    suspend fun send(topic: String, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, value))
    }

    suspend fun send(topic: String, key: K, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, key, value))
    }

    suspend fun send(topic: String, partition: Int, key: K, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, partition, key, value))
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun send(topic: String, message: Message<*>): SenderResult<Unit> {
        val producerRecord =
            messageConverter.fromMessage(message, topic) as ProducerRecord<K, V>

        val correlationId = message.headers[KafkaHeaders.CORRELATION_ID, ByteArray::class.java]
        if (correlationId != null) {
            producerRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId)
        }
        return send(producerRecord)
    }

    suspend fun send(record: ProducerRecord<K, V>): SenderResult<Unit> {
        return send(SenderRecord.create(record, Unit))
    }

    suspend fun <T> send(record: SenderRecord<K, V, T>): SenderResult<T> {
        return send(flowOf(record)).first()
    }

    fun <T> send(records: Flow<SenderRecord<K, V, T>>): Flow<SenderResult<T>> {
        return sender.send(records.asFlux()).asFlow()
    }

    suspend fun partitionsFromProducerFor(topic: String): List<PartitionInfo> {
        return doOnProducer { it.partitionsFor(topic) }
    }

    suspend fun metricsFromProducer(): Map<MetricName, Metric> {
        return doOnProducer { producer -> producer.metrics() }
    }

    suspend fun <T> doOnProducer(action: (producer: Producer<K, V>) -> T): T {
        return sender.doOnProducer { action(it) }.awaitSingle()
    }

    val transactionManager: TransactionManager
        get() = sender.transactionManager()

    override fun close() {
        doClose()
    }

    override fun destroy() {
        doClose()
    }

    private fun doClose() {
        sender.close()
    }
}
