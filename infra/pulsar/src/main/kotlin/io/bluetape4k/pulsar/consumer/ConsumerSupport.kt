package io.bluetape4k.pulsar.consumer

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema

@PublishedApi
internal val log = KotlinLogging.logger {}

/**
 * [Consumer] DSL 빌더 ([PulsarClient] 확장).
 *
 * ```kotlin
 * val consumer = client.consumer(Schema.STRING) {
 *     topic("persistent://public/default/orders")
 *     subscriptionName("order-processor")
 *     subscriptionType(SubscriptionType.Exclusive)
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ConsumerBuilder] 설정 블록
 * @return 생성된 [Consumer] 인스턴스
 */
fun <T> PulsarClient.consumer(
    schema: Schema<T>,
    setup: ConsumerBuilder<T>.() -> Unit,
): Consumer<T> = newConsumer(schema).apply(setup).subscribe()

/**
 * Consumer 생명주기를 블록 스코프로 자동 관리합니다.
 *
 * 블록 종료(정상/예외/취소) 시 [Consumer.closeAsync]를 호출해 비동기 close를 보장합니다.
 *
 * ```kotlin
 * client.withConsumer(Schema.STRING, {
 *     topic("orders"); subscriptionName("sub-1")
 * }) {
 *     val msg = receiveSuspend()
 *     acknowledgeSuspend(msg)
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ConsumerBuilder] 설정 블록
 * @param block [Consumer]를 receiver로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend inline fun <T, R> PulsarClient.withConsumer(
    schema: Schema<T>,
    noinline setup: ConsumerBuilder<T>.() -> Unit = {},
    crossinline block: suspend Consumer<T>.() -> R,
): R {
    val consumer = consumer(schema, setup)
    try {
        return block(consumer)
    } finally {
        runCatching { consumer.closeAsync().awaitSuspending() }
            .onFailure { log.warn(it) { "Consumer close 실패" } }
    }
}
