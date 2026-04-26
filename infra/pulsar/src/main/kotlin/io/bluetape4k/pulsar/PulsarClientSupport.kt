package io.bluetape4k.pulsar

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.apache.pulsar.client.api.ClientBuilder
import org.apache.pulsar.client.api.PulsarClient

@PublishedApi
internal val log = KotlinLogging.logger {}

/**
 * Apache Pulsar 클라이언트를 DSL 방식으로 생성합니다.
 *
 * ```kotlin
 * val client = pulsarClient("pulsar://localhost:6650") {
 *     connectionTimeout(5, TimeUnit.SECONDS)
 *     operationTimeout(30, TimeUnit.SECONDS)
 * }
 * ```
 *
 * @param serviceUrl 브로커 URL (비어 있으면 [setup] 블록에서 직접 지정)
 * @param setup [ClientBuilder] 추가 설정 블록
 * @return 생성된 [PulsarClient] 인스턴스
 */
fun pulsarClient(
    serviceUrl: String = "",
    setup: ClientBuilder.() -> Unit = {},
): PulsarClient {
    val builder = PulsarClient.builder()
    if (serviceUrl.isNotBlank()) {
        builder.serviceUrl(serviceUrl)
    }
    return builder.apply(setup).build()
}

/**
 * Pulsar 클라이언트 생명주기를 블록 스코프로 자동 관리합니다.
 *
 * 블록 종료(정상/예외/취소) 시 [PulsarClient.closeAsync]를 호출해 비동기 close를 보장합니다.
 *
 * ```kotlin
 * withPulsarClient("pulsar://localhost:6650") {
 *     // PulsarClient 사용
 * }
 * ```
 *
 * @param serviceUrl 브로커 URL
 * @param setup [ClientBuilder] 추가 설정 블록
 * @param block [PulsarClient]를 receiver로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend inline fun <T> withPulsarClient(
    serviceUrl: String,
    noinline setup: ClientBuilder.() -> Unit = {},
    crossinline block: suspend PulsarClient.() -> T,
): T {
    val client = pulsarClient(serviceUrl, setup)
    try {
        return block(client)
    } finally {
        runCatching { client.closeAsync().awaitSuspending() }
            .onFailure { log.warn(it) { "PulsarClient close 실패" } }
    }
}
