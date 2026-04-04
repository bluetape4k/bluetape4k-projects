package io.bluetape4k.kafka.spring.core

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import java.util.concurrent.atomic.AtomicBoolean

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
): CoroutineScope, Closeable, DisposableBean {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val closed = AtomicBoolean(false)

    override val coroutineContext = scope.coroutineContext

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

    /**
     * 트랜잭션 컨텍스트 안에서 [SenderRecord] Flow를 발송합니다.
     *
     * ```kotlin
     * val records = flowOf(
     *     SenderRecord.create(ProducerRecord("topic", "key", "value"), Unit)
     * )
     * producer.sendTransactionally(records).collect { result ->
     *     println("Sent: ${result.recordMetadata().offset()}")
     * }
     * ```
     *
     * @param records 발송할 [SenderRecord] Flow
     * @return 발송 결과 [SenderResult] Flow
     */
    fun <T> sendTransactionally(records: Flow<SenderRecord<K, V, T>>): Flow<SenderResult<T>> {
        val result = sender.sendTransactionally(Flux.just(records.asFlux()))
        return result.flatMap { it }.asFlow()
    }

    /**
     * 트랜잭션 컨텍스트 안에서 단일 [SenderRecord]를 발송합니다.
     *
     * ```kotlin
     * val record = SenderRecord.create(ProducerRecord("topic", "key", "value"), Unit)
     * val result = producer.sendTransactionally(record)
     * // result.recordMetadata().offset() 이 발송된 offset
     * ```
     *
     * @param record 발송할 [SenderRecord]
     * @return 발송 결과 [SenderResult]
     */
    suspend fun <T> sendTransactionally(record: SenderRecord<K, V, T>): SenderResult<T> {
        return sendTransactionally(flowOf(record)).first()
    }

    /**
     * 지정한 토픽에 값을 발송합니다.
     *
     * ```kotlin
     * val result = producer.send("my-topic", "hello")
     * // result.recordMetadata().topic() == "my-topic"
     * ```
     *
     * @param topic 발송할 토픽
     * @param value 발송할 값
     * @return 발송 결과 [SenderResult]
     */
    suspend fun send(topic: String, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, value))
    }

    /**
     * 지정한 토픽에 키와 값을 발송합니다.
     *
     * ```kotlin
     * val result = producer.send("my-topic", "my-key", "hello")
     * // result.recordMetadata().topic() == "my-topic"
     * ```
     *
     * @param topic 발송할 토픽
     * @param key 발송할 키
     * @param value 발송할 값
     * @return 발송 결과 [SenderResult]
     */
    suspend fun send(topic: String, key: K, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, key, value))
    }

    /**
     * 지정한 토픽의 파티션에 키와 값을 발송합니다.
     *
     * ```kotlin
     * val result = producer.send("my-topic", partition = 0, key = "my-key", value = "hello")
     * // result.recordMetadata().partition() == 0
     * ```
     *
     * @param topic 발송할 토픽
     * @param partition 발송할 파티션
     * @param key 발송할 키
     * @param value 발송할 값
     * @return 발송 결과 [SenderResult]
     */
    suspend fun send(topic: String, partition: Int, key: K, value: V): SenderResult<Unit> {
        return send(ProducerRecord(topic, partition, key, value))
    }

    /**
     * 지정한 토픽으로 메시지를 변환하여 발송합니다.
     *
     * ```kotlin
     * val message = MessageBuilder.withPayload("hello")
     *     .setHeader(KafkaHeaders.TOPIC, "my-topic")
     *     .build()
     * val result = producer.send("my-topic", message)
     * ```
     *
     * @param topic 발송할 토픽
     * @param message 발송할 Spring [Message]
     * @return 발송 결과 [SenderResult]
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun send(topic: String, message: Message<*>): SenderResult<Unit> {
        val producerRecord = messageConverter.fromMessage(message, topic)
        require(producerRecord is ProducerRecord<*, *>) {
            "RecordMessageConverter returned ${producerRecord?.javaClass?.name ?: "null"} for topic=$topic"
        }
        val typedRecord = producerRecord as ProducerRecord<K, V>

        val correlationId = message.headers[KafkaHeaders.CORRELATION_ID, ByteArray::class.java]
        if (correlationId != null) {
            typedRecord.headers().add(KafkaHeaders.CORRELATION_ID, correlationId)
        }
        return send(typedRecord)
    }

    /**
     * [ProducerRecord]를 발송합니다.
     *
     * ```kotlin
     * val record = ProducerRecord("my-topic", "key", "value")
     * val result = producer.send(record)
     * // result.recordMetadata().topic() == "my-topic"
     * ```
     *
     * @param record 발송할 [ProducerRecord]
     * @return 발송 결과 [SenderResult]
     */
    suspend fun send(record: ProducerRecord<K, V>): SenderResult<Unit> {
        return send(SenderRecord.create(record, Unit))
    }

    /**
     * 단일 [SenderRecord]를 발송합니다.
     *
     * ```kotlin
     * val record = SenderRecord.create(ProducerRecord("topic", "key", "value"), Unit)
     * val result = producer.send(record)
     * // result.recordMetadata().offset() 이 발송된 offset
     * ```
     *
     * @param record 발송할 [SenderRecord]
     * @return 발송 결과 [SenderResult]
     */
    suspend fun <T> send(record: SenderRecord<K, V, T>): SenderResult<T> {
        return send(flowOf(record)).first()
    }

    /**
     * [SenderRecord] Flow를 발송하고 결과 Flow를 반환합니다.
     *
     * ```kotlin
     * val records = flowOf(
     *     SenderRecord.create(ProducerRecord("topic", "k1", "v1"), Unit),
     *     SenderRecord.create(ProducerRecord("topic", "k2", "v2"), Unit),
     * )
     * producer.send(records).collect { result ->
     *     println("offset=${result.recordMetadata().offset()}")
     * }
     * ```
     *
     * @param records 발송할 [SenderRecord] Flow
     * @return 발송 결과 [SenderResult] Flow
     */
    fun <T> send(records: Flow<SenderRecord<K, V, T>>): Flow<SenderResult<T>> {
        return sender.send(records.asFlux()).asFlow()
    }

    /**
     * 지정한 토픽의 파티션 정보를 반환합니다.
     *
     * ```kotlin
     * val partitions = producer.partitionsFromProducerFor("my-topic")
     * // partitions.size가 파티션 수
     * ```
     *
     * @param topic 조회할 토픽 이름
     * @return 파티션 정보 목록
     */
    suspend fun partitionsFromProducerFor(topic: String): List<PartitionInfo> {
        return doOnProducer { it.partitionsFor(topic) }
    }

    /**
     * Producer 의 메트릭 정보를 반환합니다.
     *
     * ```kotlin
     * val metrics = producer.metricsFromProducer()
     * val sendCount = metrics.entries.find { it.key.name() == "record-send-total" }?.value?.metricValue()
     * ```
     *
     * @return 메트릭 이름과 값의 맵
     */
    suspend fun metricsFromProducer(): Map<MetricName, Metric> {
        return doOnProducer { producer -> producer.metrics() }
    }

    /**
     * Producer 에 직접 접근하여 [action]을 실행합니다.
     *
     * ```kotlin
     * val partitions = producer.doOnProducer { it.partitionsFor("my-topic") }
     * // partitions.size가 파티션 수
     * ```
     *
     * @param action Producer 에 적용할 액션
     * @return 액션 실행 결과
     */
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
        if (closed.compareAndSet(false, true)) {
            scope.cancel("SuspendKafkaProducerTemplate closed")
            sender.close()
        }
    }
}
