package io.bluetape4k.nats.client

import io.nats.client.Options
import java.util.*

/**
 * DSL 블록으로 NATS [Options]를 생성합니다.
 *
 * ## 동작/계약
 * - `Options.builder()`를 생성해 [builder]를 적용 후 `build()`합니다.
 * - 호출마다 새 [Options] 인스턴스를 반환합니다.
 * - builder 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val options = natsOptions { server(Options.DEFAULT_URL) }
 * // options.servers.first() == Options.DEFAULT_URL
 * ```
 */
inline fun natsOptions(
    @BuilderInference builder: Options.Builder.() -> Unit,
): Options {
    return Options.builder().apply(builder).build()
}

/**
 * [Properties] 기반 [Options.Builder]로 NATS [Options]를 생성합니다.
 *
 * ## 동작/계약
 * - `Options.Builder(properties)`를 초기값으로 사용합니다.
 * - [builder]에서 추가 설정을 덮어쓸 수 있습니다.
 * - 호출마다 새 [Options] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val options = natsOptions(Properties()) { maxReconnects(10) }
 * // options.maxReconnects == 10
 * ```
 */
inline fun natsOptions(
    properties: Properties,
    @BuilderInference builder: Options.Builder.() -> Unit = {},
): Options {
    return Options.Builder(properties).apply(builder).build()
}

/**
 * URL/재연결/버퍼 크기 기본값으로 [Options]를 생성합니다.
 *
 * ## 동작/계약
 * - [url], [maxReconnects], [bufferSize]를 각각 `server/maxReconnects/bufferSize`에 적용합니다.
 * - 인자를 지정하지 않으면 NATS 클라이언트 기본 상수를 사용합니다.
 * - 유효하지 않은 값 검증은 NATS builder 구현에 위임됩니다.
 *
 * ```kotlin
 * val options = natsOptionsOf()
 * // options.servers.first() == Options.DEFAULT_URL
 * ```
 */
fun natsOptionsOf(
    url: String = Options.DEFAULT_URL,
    maxReconnects: Int = Options.DEFAULT_MAX_RECONNECT,
    bufferSize: Int = Options.DEFAULT_BUFFER_SIZE,
): Options = natsOptions {
    server(url)
    maxReconnects(maxReconnects)
    bufferSize(bufferSize)
}
