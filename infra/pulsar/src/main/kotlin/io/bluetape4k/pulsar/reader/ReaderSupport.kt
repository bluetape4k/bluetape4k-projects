package io.bluetape4k.pulsar.reader

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Reader
import org.apache.pulsar.client.api.ReaderBuilder
import org.apache.pulsar.client.api.Schema

@PublishedApi
internal val log = KotlinLogging.logger {}

/**
 * [Reader] DSL 빌더 ([PulsarClient] 확장).
 *
 * Consumer와 달리 subscription 없이 토픽을 직접 읽습니다. ack 불필요.
 *
 * ```kotlin
 * val reader = client.reader(Schema.STRING) {
 *     topic("persistent://public/default/orders")
 *     startMessageId(MessageId.earliest)
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ReaderBuilder] 설정 블록
 * @return 생성된 [Reader] 인스턴스
 */
fun <T> PulsarClient.reader(
    schema: Schema<T>,
    setup: ReaderBuilder<T>.() -> Unit,
): Reader<T> = newReader(schema).apply(setup).create()

/**
 * Reader 생명주기를 블록 스코프로 자동 관리합니다.
 *
 * 블록 종료(정상/예외/취소) 시 [Reader.closeAsync]를 호출해 비동기 close를 보장합니다.
 *
 * ```kotlin
 * client.withReader(Schema.STRING, {
 *     topic("orders"); startMessageId(MessageId.earliest)
 * }) {
 *     val msg = readNextSuspend()
 *     println(msg.value)
 * }
 * ```
 *
 * @param schema 메시지 스키마
 * @param setup [ReaderBuilder] 설정 블록
 * @param block [Reader]를 receiver로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend inline fun <T, R> PulsarClient.withReader(
    schema: Schema<T>,
    noinline setup: ReaderBuilder<T>.() -> Unit = {},
    crossinline block: suspend Reader<T>.() -> R,
): R {
    val reader = reader(schema, setup)
    try {
        return block(reader)
    } finally {
        runCatching { reader.closeAsync().awaitSuspending() }
            .onFailure { log.warn(it) { "Reader close 실패" } }
    }
}
