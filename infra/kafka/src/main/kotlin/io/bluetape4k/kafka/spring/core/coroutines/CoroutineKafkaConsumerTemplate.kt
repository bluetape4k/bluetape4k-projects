package io.bluetape4k.kafka.spring.core.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.Metric
import org.apache.kafka.common.MetricName
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.TransactionManager

/**
 * Coroutine 환경에서 Kafka Consumer 기능을 제공하는 구현체입니다.
 *
 * ```
 * val receiverOptions = ReceiverOptions.create<String, String>(
 *      mapOf(
 *          "bootstrap.servers" to "localhost:9092",
 *          "group.id" to "test-group",
 *          "auto.offset.reset" to "earliest",
 *          "enable.auto.commit" to "false",
 *          "key.deserializer" to StringDeserializer::class.java,
 *          "value.deserializer" to StringDeserializer::class.java,
 *          "topic" to "test-topic",
 *          "max.poll.records" to "1",
 *          "max.poll.interval.ms" to "1000",
 *          "session.timeout.ms" to "15000",
 *          "heartbeat.interval.ms" to "5000",
 *          "auto.commit.interval.ms" to "1000",
 *      )
 * )
 * val consumer = CoroutineKafkaConsumerTemplate(receiverOptions)
 * consumer.receive().collect { record ->
 *     println("Received: ${record.value()}")
 *     record.receiver.commit(record.receiver.assignment())
 *     // record.receiver.commit(record.receiver.assignment(), record.offset())
 *     // record.receiver.commit(record.receiver.assignment(), record.offset(), record.partition())
 *     // record.receiver.commit(record.receiver.assignment(), record.offset(), record.partition(), record.topic())
 * }
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @property receiver [KafkaReceiver] instance.
 *
 * @see [org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate]
 */
class CoroutineKafkaConsumerTemplate<K, V>(
    private val receiver: KafkaReceiver<K, V>,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <K, V> invoke(
            receiverOptions: ReceiverOptions<K, V>,
        ): CoroutineKafkaConsumerTemplate<K, V> {
            return CoroutineKafkaConsumerTemplate(KafkaReceiver.create(receiverOptions))
        }
    }

    fun receive(): Flow<ReceiverRecord<K, V>> {
        return receiver.receive().asFlow()
    }

    fun receiveAutoAck(): Flow<ConsumerRecord<K, V>> {
        return receiver.receiveAutoAck().concatMap { it }.asFlow()
    }

    /**
     * Returns a {@link Flux} of consumer record batches that may be used for exactly once
     * delivery semantics. A new transaction is started for each inner Flux and it is the
     * responsibility of the consuming application to commit or abort the transaction
     * using {@link TransactionManager#commit()} or {@link TransactionManager#abort()}
     * after processing the Flux. The next batch of consumer records will be delivered only
     * after the previous flux terminates. Offsets of records dispatched on each inner Flux
     * are committed using the provided <code>transactionManager</code> within the transaction
     * started for that Flux.
     * <p> Example usage:
     * <pre>
     * {@code
     * KafkaSender<Integer, Person> sender = sender(senderOptions());
     * ReceiverOptions<Integer, Person> receiverOptions = receiverOptions(Collections.singleton(sourceTopic));
     * KafkaReceiver<Integer, Person> receiver = KafkaReceiver.create(receiverOptions);
     * receiver.receiveExactlyOnce(sender.transactionManager())
     * 	 .concatMap(f -> sendAndCommit(f))
     *	 .onErrorResume(e -> sender.transactionManager().abort().then(Mono.error(e)))
     *	 .doOnCancel(() -> close());
     *
     * Flux<SenderResult<Integer>> sendAndCommit(Flux<ConsumerRecord<Integer, Person>> flux) {
     * 	return sender.send(flux.map(r -> SenderRecord.<Integer, Person, Integer>create(transform(r.value()), r.key())))
     *			.concatWith(sender.transactionManager().commit());
     * }
     * }
     * </pre>
     * @param transactionManager Transaction manager used to begin new transaction for each
     *        inner Flux and commit offsets within that transaction
     * @return Flux of consumer record batches processed within a transaction
     */
    fun receiveExactlyOnce(transactionManager: TransactionManager): Flow<Flow<ConsumerRecord<K, V>>> {
        return receiver.receiveExactlyOnce(transactionManager).map { it.asFlow() }.asFlow()
    }

    private suspend inline fun <T> doOnConsumer(crossinline function: (Consumer<K, V>) -> T): T {
        return receiver.doOnConsumer { function(it) }.awaitSingle()
    }

    suspend fun assignment(): Set<TopicPartition> {
        return doOnConsumer { it.assignment() }
    }

    suspend fun subscroption(): Set<String> {
        return doOnConsumer { it.subscription() }
    }

    suspend fun seek(partition: TopicPartition, offset: Long) {
        doOnConsumer { consumer ->
            consumer.seek(partition, offset)
        }
    }

    suspend fun seekToBegining(vararg partitions: TopicPartition) {
        doOnConsumer { consumer ->
            consumer.seekToBeginning(partitions.asList())
        }
    }

    suspend fun seekToEnd(vararg partitions: TopicPartition) {
        doOnConsumer { consumer ->
            consumer.seekToEnd(partitions.asList())
        }
    }

    suspend fun partition(partition: TopicPartition): Long {
        return doOnConsumer { it.position(partition) }
    }

    suspend fun committed(partitions: Set<TopicPartition>): Map<TopicPartition, OffsetAndMetadata> {
        return doOnConsumer { it.committed(partitions) }
    }

    suspend fun partitionsFromConsumerFor(topic: String): List<PartitionInfo> {
        return doOnConsumer { it.partitionsFor(topic) }
    }

    suspend fun paused(): Set<TopicPartition> {
        return doOnConsumer { it.paused() }
    }

    suspend fun pause(vararg partitions: TopicPartition) {
        doOnConsumer { it.pause(partitions.asList()) }
    }

    suspend fun resume(vararg partitions: TopicPartition) {
        doOnConsumer { it.resume(partitions.asList()) }
    }

    suspend fun metricsFromConsumer(): Map<MetricName, Metric> {
        return doOnConsumer { it.metrics() }
    }

    suspend fun listTopics(): Map<String, List<PartitionInfo>> {
        return doOnConsumer { it.listTopics() }
    }

    suspend fun offsetsForTimes(timestampsToSearch: Map<TopicPartition, Long>): Map<TopicPartition, OffsetAndTimestamp> {
        return doOnConsumer { it.offsetsForTimes(timestampsToSearch) }
    }

    suspend fun beginningOffsets(vararg partitions: TopicPartition): Map<TopicPartition, Long> {
        return doOnConsumer { it.beginningOffsets(partitions.asList()) }
    }

    suspend fun endOffsets(vararg partitions: TopicPartition): Map<TopicPartition, Long> {
        return doOnConsumer { it.endOffsets(partitions.asList()) }
    }
}
