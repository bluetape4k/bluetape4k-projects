package io.bluetape4k.kafka.spring.core

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import org.springframework.beans.factory.DisposableBean
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.TransactionManager
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

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
 * val consumer = SuspendKafkaConsumerTemplate(receiverOptions)
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
class SuspendKafkaConsumerTemplate<K, V> private constructor(
    private val receiver: KafkaReceiver<K, V>,
) : CoroutineScope, Closeable, DisposableBean {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val closed = AtomicBoolean(false)

    override val coroutineContext = scope.coroutineContext

    companion object : KLoggingChannel() {
        @JvmStatic
        operator fun <K, V> invoke(receiverOptions: ReceiverOptions<K, V>): SuspendKafkaConsumerTemplate<K, V> =
            SuspendKafkaConsumerTemplate(KafkaReceiver.create(receiverOptions))

        @JvmStatic
        operator fun <K, V> invoke(receiver: KafkaReceiver<K, V>): SuspendKafkaConsumerTemplate<K, V> =
            SuspendKafkaConsumerTemplate(receiver)
    }

    /**
     * 레코드를 수동 acknowledgment 방식으로 수신합니다.
     *
     * ```kotlin
     * val consumer = SuspendKafkaConsumerTemplate(receiverOptions)
     * consumer.receive().collect { record ->
     *     println("Received: ${record.value()}")
     *     record.receiverOffset().acknowledge()
     * }
     * ```
     */
    fun receive(): Flow<ReceiverRecord<K, V>> = receiver.receive().asFlow()

    /**
     * 레코드를 자동 acknowledgment 방식으로 수신합니다.
     *
     * ```kotlin
     * val consumer = SuspendKafkaConsumerTemplate(receiverOptions)
     * consumer.receiveAutoAck().collect { record ->
     *     println("Received: ${record.value()}")
     *     // offset은 자동으로 커밋됨
     * }
     * ```
     */
    fun receiveAutoAck(): Flow<ConsumerRecord<K, V>> = receiver.receiveAutoAck().concatMap { it }.asFlow()

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
    fun receiveExactlyOnce(transactionManager: TransactionManager): Flow<Flow<ConsumerRecord<K, V>>> =
        receiver.receiveExactlyOnce(transactionManager).map { it.asFlow() }.asFlow()

    private suspend inline fun <T> doOnConsumer(
        crossinline function: (Consumer<K, V>) -> T,
    ): T = receiver.doOnConsumer { function(it) }.awaitSingle()

    /**
     * 지정한 topic 목록으로 consumer를 구독시킵니다.
     *
     * @param topics 구독할 topic 목록
     */
    suspend fun subscribe(vararg topics: String) {
        topics.requireNotEmpty("topics")
        topics.forEach { it.requireNotBlank("topics") }
        doOnConsumer { it.subscribe(topics.asList()) }
    }

    /**
     * 정규식 패턴과 일치하는 topic을 구독시킵니다.
     *
     * @param pattern 구독할 topic 패턴
     */
    suspend fun subscribe(pattern: Pattern) {
        doOnConsumer { it.subscribe(pattern) }
    }

    /**
     * 지정한 파티션들을 직접 할당합니다.
     *
     * @param partitions 직접 할당할 파티션 목록
     */
    suspend fun assign(vararg partitions: TopicPartition) {
        doOnConsumer { it.assign(partitions.asList()) }
    }

    /**
     * 현재 구독/할당을 모두 해제합니다.
     */
    suspend fun unsubscribe() {
        doOnConsumer { it.unsubscribe() }
    }

    /**
     * 현재 consumer에 할당된 파티션 목록을 반환합니다.
     */
    suspend fun assignment(): Set<TopicPartition> = doOnConsumer { it.assignment() }

    /**
     * 현재 consumer의 구독 topic 목록을 반환합니다.
     */
    suspend fun subscription(): Set<String> = doOnConsumer { it.subscription() }

    /**
     * 지정한 파티션의 offset으로 이동합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * consumer.seek(partition, 100L)
     * // 이후 poll은 offset 100부터 시작됩니다
     * ```
     *
     * @param partition 이동할 파티션
     * @param offset 이동할 offset
     */
    suspend fun seek(
        partition: TopicPartition,
        offset: Long,
    ) {
        doOnConsumer { consumer ->
            consumer.seek(partition, offset)
        }
    }

    /**
     * 지정한 timestamp 시점에 가장 가까운 offset으로 이동합니다.
     *
     * @param partition 이동할 대상 파티션
     * @param timestamp 찾을 timestamp(epoch millis)
     * @return seek에 사용한 offset. 해당 timestamp가 없으면 `null`
     */
    suspend fun seekToTimestamp(
        partition: TopicPartition,
        timestamp: Long,
    ): Long? {
        val offset = offsetsForTimes(mapOf(partition to timestamp))[partition]?.offset()
        if (offset != null) {
            seek(partition, offset)
        }
        return offset
    }

    /**
     * 지정한 파티션들의 offset을 처음으로 이동합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * consumer.seekToBeginning(partition)
     * // 이후 poll은 파티션의 처음 offset부터 시작됩니다
     * ```
     *
     * @param partitions 처음으로 이동할 파티션 목록
     */
    suspend fun seekToBeginning(vararg partitions: TopicPartition) {
        doOnConsumer { consumer ->
            consumer.seekToBeginning(partitions.asList())
        }
    }

    /**
     * 지정한 파티션들의 offset을 끝으로 이동합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * consumer.seekToEnd(partition)
     * // 이후 poll은 파티션의 끝 offset부터 시작됩니다
     * ```
     *
     * @param partitions 끝으로 이동할 파티션 목록
     */
    suspend fun seekToEnd(vararg partitions: TopicPartition) {
        doOnConsumer { consumer ->
            consumer.seekToEnd(partitions.asList())
        }
    }

    /**
     * 지정한 파티션의 현재 fetch position을 반환합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * val pos = consumer.partition(partition)
     * // pos는 다음 fetch 할 offset
     * ```
     *
     * @param partition 조회할 파티션
     * @return 현재 position (다음 fetch할 offset)
     */
    suspend fun partition(partition: TopicPartition): Long = doOnConsumer { it.position(partition) }

    /**
     * 지정한 파티션의 현재 position을 반환합니다.
     */
    suspend fun position(partition: TopicPartition): Long = doOnConsumer { it.position(partition) }

    /**
     * 지정한 파티션들의 커밋된 offset 정보를 반환합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * val committed = consumer.committed(setOf(partition))
     * // committed[partition]?.offset() 이 마지막으로 커밋된 offset
     * ```
     *
     * @param partitions 조회할 파티션 집합
     * @return 파티션별 커밋 정보 맵
     */
    suspend fun committed(partitions: Set<TopicPartition>): Map<TopicPartition, OffsetAndMetadata> =
        doOnConsumer {
            it.committed(partitions)
        }

    /**
     * 지정한 offset들을 동기 커밋합니다.
     *
     * @param offsets 커밋할 topic-partition별 offset metadata
     */
    suspend fun commit(offsets: Map<TopicPartition, OffsetAndMetadata>) {
        doOnConsumer { it.commitSync(offsets) }
    }

    /**
     * 지정한 파티션들의 현재 position을 offset으로 계산해 동기 커밋합니다.
     *
     * 파티션을 생략하면 현재 assignment 전체를 커밋합니다.
     *
     * @param partitions 현재 position을 커밋할 파티션 목록
     * @return 실제로 커밋한 offset metadata 맵
     */
    suspend fun commitCurrentOffsets(vararg partitions: TopicPartition): Map<TopicPartition, OffsetAndMetadata> =
        doOnConsumer { consumer ->
            val targets = if (partitions.isNotEmpty()) partitions.toSet() else consumer.assignment()
            if (targets.isEmpty()) {
                return@doOnConsumer emptyMap()
            }
            val currentAssignment = consumer.assignment()
            require(targets.all { it in currentAssignment }) {
                "Cannot commit offsets for unassigned partitions. targets=$targets, assignment=$currentAssignment"
            }
            val offsets = targets.associateWith { topicPartition ->
                OffsetAndMetadata(consumer.position(topicPartition))
            }
            consumer.commitSync(offsets)
            offsets
        }

    /**
     * 지정한 토픽의 파티션 정보를 반환합니다.
     *
     * ```kotlin
     * val partitions = consumer.partitionsFromConsumerFor("my-topic")
     * // partitions.size가 파티션 수
     * ```
     *
     * @param topic 조회할 토픽 이름
     * @return 파티션 정보 목록
     */
    suspend fun partitionsFromConsumerFor(topic: String): List<PartitionInfo> = doOnConsumer { it.partitionsFor(topic) }

    /**
     * 현재 일시 정지된 파티션 집합을 반환합니다.
     *
     * ```kotlin
     * val paused = consumer.paused()
     * // paused에 일시 정지된 파티션이 포함됨
     * ```
     *
     * @return 일시 정지된 파티션 집합
     */
    suspend fun paused(): Set<TopicPartition> = doOnConsumer { it.paused() }

    /**
     * 지정한 파티션들을 일시 정지합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * consumer.pause(partition)
     * // 이후 poll에서 해당 파티션은 레코드를 반환하지 않습니다
     * ```
     *
     * @param partitions 일시 정지할 파티션 목록
     */
    suspend fun pause(vararg partitions: TopicPartition) {
        doOnConsumer { it.pause(partitions.asList()) }
    }

    /**
     * 일시 정지된 파티션들을 재개합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * consumer.resume(partition)
     * // 이후 poll에서 해당 파티션이 다시 레코드를 반환합니다
     * ```
     *
     * @param partitions 재개할 파티션 목록
     */
    suspend fun resume(vararg partitions: TopicPartition) {
        doOnConsumer { it.resume(partitions.asList()) }
    }

    /**
     * Consumer 의 메트릭 정보를 반환합니다.
     *
     * ```kotlin
     * val metrics = consumer.metricsFromConsumer()
     * val fetchRate = metrics.entries.find { it.key.name() == "fetch-rate" }?.value?.metricValue()
     * ```
     *
     * @return 메트릭 이름과 값의 맵
     */
    suspend fun metricsFromConsumer(): Map<MetricName, Metric> = doOnConsumer { it.metrics() }

    /**
     * Consumer가 접근 가능한 모든 토픽 목록을 반환합니다.
     *
     * ```kotlin
     * val topics = consumer.listTopics()
     * // topics.keys에 토픽 이름 목록이 포함됨
     * ```
     *
     * @return 토픽 이름과 파티션 정보 목록의 맵
     */
    suspend fun listTopics(): Map<String, List<PartitionInfo>> = doOnConsumer { it.listTopics() }

    /**
     * 지정한 timestamp에 해당하는 파티션별 offset 정보를 반환합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * val ts = System.currentTimeMillis() - 60_000
     * val offsets = consumer.offsetsForTimes(mapOf(partition to ts))
     * // offsets[partition]?.offset() 이 ts 시점의 offset
     * ```
     *
     * @param timestampsToSearch 파티션별 timestamp 맵
     * @return 파티션별 offset 정보 맵
     */
    suspend fun offsetsForTimes(
        timestampsToSearch: Map<TopicPartition, Long>,
    ): Map<TopicPartition, OffsetAndTimestamp> =
        doOnConsumer {
            it.offsetsForTimes(timestampsToSearch)
        }

    /**
     * 지정한 파티션들의 처음 offset을 반환합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * val offsets = consumer.beginningOffsets(partition)
     * // offsets[partition] 이 처음 offset
     * ```
     *
     * @param partitions 조회할 파티션 목록
     * @return 파티션별 처음 offset 맵
     */
    suspend fun beginningOffsets(vararg partitions: TopicPartition): Map<TopicPartition, Long> =
        doOnConsumer {
            it.beginningOffsets(partitions.asList())
        }

    /**
     * 지정한 파티션들의 끝 offset을 반환합니다.
     *
     * ```kotlin
     * val partition = TopicPartition("my-topic", 0)
     * val offsets = consumer.endOffsets(partition)
     * // offsets[partition] 이 끝 offset (다음에 쓸 위치)
     * ```
     *
     * @param partitions 조회할 파티션 목록
     * @return 파티션별 끝 offset 맵
     */
    suspend fun endOffsets(vararg partitions: TopicPartition): Map<TopicPartition, Long> =
        doOnConsumer {
            it.endOffsets(partitions.asList())
        }

    override fun close() {
        doClose()
    }

    override fun destroy() {
        doClose()
    }

    private fun doClose() {
        if (closed.compareAndSet(false, true)) {
            scope.cancel("SuspendKafkaConsumerTemplate closed")
            (receiver as? AutoCloseable)?.close()
        }
    }
}
