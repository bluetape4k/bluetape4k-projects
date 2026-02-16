package io.bluetape4k.kafka.coroutines

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

/**
 * Coroutine 환경 하에서 Producer를 이용하여 메시지를 producing 합니다.
 *
 * ```
 * val producer = producerOf(
 *    mapOf(
 *      "bootstrap.servers" to "localhost:9092",
 *      "acks" to "all",
 *      "retries" to 3,
 *    ),
 *    StringSerializer(),
 *    StringSerializer(),
 * )
 * val record = ProducerRecord("test-topic", "test-key", "test-value")
 * producer.suspendSend(record)
 * ```
 *
 *
 * @param record 발행할 메시지 ([ProducerRecord])
 * @return 발행 결과를 표현하는 [RecordMetadata] instance
 */
suspend fun <K, V> Producer<K, V>.suspendSend(record: ProducerRecord<K, V>): RecordMetadata {
    return send(record).awaitSuspending()
}

/**
 * 복수의 [ProducerRecord] 를 producing 하면서, 결과들을 Flow로 반환하도록 합니다.
 *
 * ```
 * val records = flow {
 *      emit(ProducerRecord("test-topic", "test-key", "test-value"))
 *      emit(ProducerRecord("test-topic", "test-key2", "test-value2"))
 *      emit(ProducerRecord("test-topic", "test-key3", "test-value3"))
 * }
 * producer.sendFlow(records)
 * ```
 *
 * @param records producing 할 record의 flow
 * @return producing 된 결과 ([RecordMetadata])의 flow
 */
suspend fun <K, V> Producer<K, V>.sendAsFlow(records: Flow<ProducerRecord<K, V>>): Flow<RecordMetadata> {
    return records
        .buffer()
        .async {
            suspendSend(it)
        }
        .onCompletion { flush() }
}


/**
 * 복수의 [ProducerRecord] 를 producing 하면서, 마지막 producing 한 결과만 반환하게 한다.
 *
 * ```
 * val records = flow {
 *      emit(ProducerRecord("test-topic", "test-key", "test-value"))
 *      emit(ProducerRecord("test-topic", "test-key2", "test-value2"))
 *      emit(ProducerRecord("test-topic", "test-key3", "test-value3"))
 * }
 * producer.sendFlowParallel(records)
 * ```
 *
 * @param records producing 할 record의 flow
 * @return 마지막 record에 대한 producing 한 결과
 */
suspend fun <K, V> Producer<K, V>.sendAsFlowParallel(
    records: Flow<ProducerRecord<K, V>>,
): RecordMetadata {
    return records
        .buffer()
        .async {
            suspendSend(it)
        }
        .onCompletion { flush() }
        .last()
}

/**
 * 발송만 하고, 결과 값은 받지 않습니다.
 *
 * ```
 * val records = flow {
 *      emit(ProducerRecord("test-topic", "test-key", "test-value"))
 *      emit(ProducerRecord("test-topic", "test-key2", "test-value2"))
 *      emit(ProducerRecord("test-topic", "test-key3", "test-value3"))
 * }
 * producer.sendAndForget(records, needFlush = false)
 * ```
 *
 * @param records producing 할 record의 flow
 */
suspend fun <K, V> Producer<K, V>.sendAndForget(
    records: Flow<ProducerRecord<K, V>>,
    needFlush: Boolean = false,
) {
    records
        .buffer()
        .async {
            send(it).awaitSuspending()
        }
        .collectLatest {
            if (needFlush) {
                flush()
            }
        }
}
