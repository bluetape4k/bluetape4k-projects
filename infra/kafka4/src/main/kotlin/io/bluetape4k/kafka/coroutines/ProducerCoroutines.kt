package io.bluetape4k.kafka.coroutines

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
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
 * flush()는 에러/취소 시에도 onCompletion이 호출되므로 cause 가 없는 정상 완료 시에만
 * 호출한다. 에러 상황에서 flush()를 호출하면 이미 실패한 send 의 결과를 강제 대기하여
 * 불필요하게 블로킹되거나 예외를 덮어쓸 위험이 있다.
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
        // cause != null 이면 에러/취소 — flush() 호출 시 블로킹 위험이 있으므로 정상 완료 시에만 실행
        .onCompletion { cause -> if (cause == null) flush() }
}


/**
 * 복수의 [ProducerRecord] 를 producing 하면서, 마지막 producing 한 결과만 반환하게 한다.
 *
 * flush()는 에러/취소 시에도 onCompletion이 호출되므로 cause 가 없는 정상 완료 시에만
 * 호출한다. 에러 상황에서 flush()를 호출하면 이미 실패한 send 의 결과를 강제 대기하여
 * 불필요하게 블로킹되거나 예외를 덮어쓸 위험이 있다.
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
        // cause != null 이면 에러/취소 — flush() 호출 시 블로킹 위험이 있으므로 정상 완료 시에만 실행
        .onCompletion { cause -> if (cause == null) flush() }
        .last()
}

/**
 * 발송만 하고, 결과 값은 받지 않습니다.
 *
 * needFlush=true 이더라도 에러/취소 상황에서는 flush()를 호출하지 않는다.
 * onCompletion의 cause 파라미터로 정상 완료 여부를 판별하며,
 * 에러 중 flush() 호출은 이미 실패한 send 요청을 강제 대기하여 스레드를 블로킹할 수 있다.
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
 * @param needFlush 정상 완료 후 flush() 호출 여부
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
        // cause != null 이면 에러/취소 — 에러 중 flush() 호출은 불필요한 블로킹을 유발한다
        .onCompletion { cause ->
            if (needFlush && cause == null) {
                flush()
            }
        }
        .collect()
}
