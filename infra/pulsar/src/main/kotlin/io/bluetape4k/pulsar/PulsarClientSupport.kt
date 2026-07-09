package io.bluetape4k.pulsar

import io.bluetape4k.logging.KotlinLogging
import org.apache.pulsar.client.api.ClientBuilder
import org.apache.pulsar.client.api.PulsarClient

@PublishedApi
internal val log = KotlinLogging.logger {}

/**
 * Apache Pulsar 클라이언트를 DSL 방식으로 생성합니다.
 *
 * `serviceUrl`이 비어 있으면 [setup] 블록에서 `serviceUrl()`을 반드시 설정해야 합니다.
 * 둘 다 지정하지 않으면 `build()` 시 Pulsar 예외가 발생합니다.
 *
 * ```kotlin
 * val client = pulsarClient("pulsar://localhost:6650") {
 *     connectionTimeout(5, TimeUnit.SECONDS)
 * }
 *
 * // setup-only 모드 (TLS/인증 등 빌더로 완전 설정)
 * val client = pulsarClient {
 *     serviceUrl("pulsar+ssl://broker:6651")
 *     tlsTrustCertsFilePath("/path/to/ca.cert.pem")
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
        closeAsyncNonCancellable("PulsarClient") { client.closeAsync() }
    }
}

/**
 * setup 블록만으로 Pulsar 클라이언트를 생성하고 생명주기를 자동 관리합니다.
 *
 * TLS, 인증 등 [ClientBuilder]에서 직접 URL을 설정하는 경우 사용합니다.
 *
 * ```kotlin
 * withPulsarClient({
 *     serviceUrl("pulsar+ssl://broker:6651")
 *     tlsTrustCertsFilePath("/path/to/ca.cert.pem")
 * }) {
 *     // PulsarClient 사용
 * }
 * ```
 *
 * @param setup [ClientBuilder] 설정 블록 (serviceUrl 포함)
 * @param block [PulsarClient]를 receiver로 받는 suspend 블록
 * @return 블록의 반환값
 */
suspend inline fun <T> withPulsarClient(
    noinline setup: ClientBuilder.() -> Unit,
    crossinline block: suspend PulsarClient.() -> T,
): T = withPulsarClient(serviceUrl = "", setup = setup, block = block)
