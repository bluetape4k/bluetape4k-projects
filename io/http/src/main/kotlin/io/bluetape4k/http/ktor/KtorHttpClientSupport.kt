package io.bluetape4k.http.ktor

import io.bluetape4k.support.requirePositiveNumber
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Serializable
import java.time.Duration

/**
 * Ktor Client helper에서 사용하는 기본 JSON 설정입니다.
 *
 * ## 동작 계약
 * - 알 수 없는 JSON field는 무시하여 server가 field를 추가해도 client가 견딜 수 있게 합니다.
 * - 테스트와 예제에서 DTO round trip이 안정적으로 유지되도록 기본값을 인코딩합니다.
 * - 일반적인 HTTP API payload 관례에 맞춰 명시적 `null` 값은 생략합니다.
 */
val defaultKtorClientJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/**
 * [ktorJsonHttpClientOf]가 설치하는 timeout 설정입니다.
 *
 * ## 동작 계약
 * - 모든 duration은 양수여야 합니다.
 * - 값은 Ktor timeout plugin의 millisecond 설정으로 변환됩니다.
 * - 요청별 timeout이 필요한 호출자는 Ktor request-level 설정을 계속 사용해야 합니다.
 *
 * @property requestTimeout 전체 요청 완료까지 허용되는 client-level timeout입니다.
 * @property connectTimeout 원격 endpoint와 connection을 맺을 때 허용되는 timeout입니다.
 * @property socketTimeout socket read/write가 응답 없이 대기할 수 있는 timeout입니다.
 */
data class KtorClientTimeouts(
    val requestTimeout: Duration = Duration.ofSeconds(30),
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val socketTimeout: Duration = Duration.ofSeconds(30),
): Serializable {

    init {
        requestTimeout.toMillis().requirePositiveNumber("requestTimeoutMillis")
        connectTimeout.toMillis().requirePositiveNumber("connectTimeoutMillis")
        socketTimeout.toMillis().requirePositiveNumber("socketTimeoutMillis")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * 지정한 [engineFactory]와 선택적 [block] 설정으로 [HttpClient]를 생성합니다.
 *
 * ## 동작 계약
 * - 반환된 [HttpClient]의 소유권은 호출자에게 있으며 [HttpClient.close] 호출 책임도 호출자에게 있습니다.
 * - plugin, 기본값, 정책을 설치하지 않으며 설정은 전적으로 호출자가 정합니다.
 * - 이 module은 `ktor-client-core`와 선택한 engine artifact를 `compileOnly`로 선언합니다.
 *   consumer는 필요한 Ktor artifact를 자신의 compile classpath에 추가해야 합니다.
 *
 * ```kotlin
 * val client = ktorHttpClientOf(CIO) {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * client.use { /* requests */ }
 * ```
 *
 * @param engineFactory 사용할 Ktor [HttpClientEngineFactory]입니다. 예: [CIO], OkHttp, Java.
 * @param block engine 선택 뒤 적용할 선택적 [HttpClientConfig] DSL block입니다.
 * @return 새 [HttpClient] 인스턴스입니다.
 */
fun <T : HttpClientEngineConfig> ktorHttpClientOf(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {},
): HttpClient = HttpClient(engineFactory, block)

/**
 * JSON content negotiation과 timeout 기본값을 포함하는 Ktor [HttpClient]를 생성합니다.
 *
 * ## 동작 계약
 * - engine 선택은 [engineFactory]로 명시하며, 이 helper는 CIO, Java, Apache, mock 중 하나를 선택하지 않습니다.
 * - Ktor `ContentNegotiation` JSON plugin과 `HttpTimeout` plugin만 설치합니다.
 * - retry, resilience, logging, metrics, authentication, tracing 정책은 설치하지 않습니다.
 * - 반환된 [HttpClient]의 소유권과 [HttpClient.close] 호출 책임은 호출자에게 있습니다.
 *
 * ```kotlin
 * val client = ktorJsonHttpClientOf(CIO) {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * ```
 *
 * @param engineFactory 사용할 Ktor [HttpClientEngineFactory]입니다.
 * @param json request/response serialization에 사용할 JSON 설정입니다.
 * @param timeouts client-level timeout 기본값입니다.
 * @param block 공유 plugin 설치 뒤 적용할 선택적 [HttpClientConfig] DSL block입니다.
 * @return 새 [HttpClient] 인스턴스입니다.
 */
fun <T : HttpClientEngineConfig> ktorJsonHttpClientOf(
    engineFactory: HttpClientEngineFactory<T>,
    json: Json = defaultKtorClientJson,
    timeouts: KtorClientTimeouts = KtorClientTimeouts(),
    block: HttpClientConfig<T>.() -> Unit = {},
): HttpClient =
    HttpClient(engineFactory) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeouts.requestTimeout.toMillis()
            connectTimeoutMillis = timeouts.connectTimeout.toMillis()
            socketTimeoutMillis = timeouts.socketTimeout.toMillis()
        }
        block()
    }

/**
 * Ktor CIO engine을 사용하는 [HttpClient]를 생성합니다.
 *
 * ## 동작 계약
 * - CIO는 suspend-native이며 HTTP/1.x만 지원합니다. HTTP/2는 지원하지 않습니다.
 * - HTTP/2 use case에는 HC5 Async, JDK, OkHttp engine을 우선 사용합니다.
 * - 반환된 [HttpClient]의 소유권과 [HttpClient.close] 호출 책임은 호출자에게 있습니다.
 *
 * ```kotlin
 * val client = ktorCioHttpClientOf {
 *     engine {
 *         requestTimeout = 10_000
 *     }
 * }
 * client.use { /* requests */ }
 * ```
 *
 * @param block CIO 전용 설정에 사용할 선택적 [HttpClientConfig] DSL block입니다.
 * @return CIO engine을 사용하는 새 [HttpClient]입니다.
 */
fun ktorCioHttpClientOf(
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
): HttpClient = HttpClient(CIO, block)

/**
 * JSON content negotiation과 timeout 기본값을 포함하는 CIO 기반 Ktor [HttpClient]를 생성합니다.
 *
 * ## 동작 계약
 * - CIO는 HTTP/1.x 전용입니다. HTTP/2 use case에는 HC5 Async, JDK, OkHttp engine을 우선 사용합니다.
 * - JSON과 timeout plugin만 설치합니다. retry/resilience 정책은 이 helper 밖에서 구성합니다.
 * - 반환된 [HttpClient]의 소유권과 [HttpClient.close] 호출 책임은 호출자에게 있습니다.
 *
 * ```kotlin
 * val client = ktorCioJsonHttpClientOf(
 *     timeouts = KtorClientTimeouts(requestTimeout = Duration.ofSeconds(5))
 * )
 * ```
 *
 * @param json request/response serialization에 사용할 JSON 설정입니다.
 * @param timeouts client-level timeout 기본값입니다.
 * @param block CIO 전용 설정에 사용할 선택적 [HttpClientConfig] DSL block입니다.
 * @return CIO engine을 사용하는 새 [HttpClient]입니다.
 */
fun ktorCioJsonHttpClientOf(
    json: Json = defaultKtorClientJson,
    timeouts: KtorClientTimeouts = KtorClientTimeouts(),
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
): HttpClient =
    ktorJsonHttpClientOf(
        engineFactory = CIO,
        json = json,
        timeouts = timeouts,
        block = block,
    )
