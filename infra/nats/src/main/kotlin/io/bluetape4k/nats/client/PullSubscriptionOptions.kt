package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.PullSubscribeOptions

/**
 * DSL 블록으로 [PullSubscribeOptions]를 생성합니다.
 *
 * @param builder [PullSubscribeOptions.Builder]에 적용할 설정 블록
 * @return [PullSubscribeOptions] 인스턴스
 */
inline fun pullSubscriptionOptions(
    builder: PullSubscribeOptions.Builder.() -> Unit,
): PullSubscribeOptions =
    PullSubscribeOptions
        .builder()
        .apply(builder)
        .build()

/**
 * 스트림과 바인드 이름으로 [PullSubscribeOptions]를 생성합니다.
 *
 * @param stream 스트림 이름
 * @param bind 바인드(consumer) 이름
 * @return [PullSubscribeOptions] 인스턴스
 */
fun pullSubscriptionOptionsOf(
    stream: String,
    bind: String,
): PullSubscribeOptions {
    stream.requireNotBlank("stream")
    bind.requireNotBlank("bind")

    return PullSubscribeOptions.bind(stream, bind)
}
