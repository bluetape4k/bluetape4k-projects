package io.bluetape4k.pulsar.producer

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema

@PublishedApi
internal val log = KotlinLogging.logger {}

/**
 * [Producer] DSL 빌더 ([PulsarClient] 확장).
 *
 * ```kotlin
 * val producer = client.producer(Schema.STRING) {
 *     topic("persistent://public/default/orders")
 *     producerName("order-producer")
 *     compressionType(CompressionType.LZ4)
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ProducerBuilder] 설정 블록
 * @return 생성된 [Producer] 인스턴스
 */
fun <T> PulsarClient.producer(
    schema: Schema<T>,
    setup: ProducerBuilder<T>.() -> Unit,
): Producer<T> = newProducer(schema).apply(setup).create()

/**
 * Producer 생명주기를 블록 스코프로 자동 관리합니다.
 *
 * 블록 종료(정상/예외/취소) 시 [Producer.closeAsync]를 호출해 비동기 close를 보장합니다.
 *
 * ```kotlin
 * client.withProducer(Schema.STRING, { topic("orders") }) {
 *     sendSuspend("hello")
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ProducerBuilder] 설정 블록
 * @param block [Producer]를 receiver로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend inline fun <T, R> PulsarClient.withProducer(
    schema: Schema<T>,
    noinline setup: ProducerBuilder<T>.() -> Unit = {},
    crossinline block: suspend Producer<T>.() -> R,
): R {
    val producer = producer(schema, setup)
    try {
        return block(producer)
    } finally {
        runCatching { producer.closeAsync().awaitSuspending() }
            .onFailure { log.warn(it) { "Producer close 실패" } }
    }
}
