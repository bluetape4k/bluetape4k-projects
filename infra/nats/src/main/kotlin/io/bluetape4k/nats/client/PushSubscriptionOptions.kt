package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.PushSubscribeOptions

/**
 * DSL 블록으로 [PushSubscribeOptions]를 생성합니다.
 *
 * @param builder [PushSubscribeOptions.Builder]에 적용할 설정 블록
 * @return [PushSubscribeOptions] 인스턴스
 */
inline fun pushSubscriptionOptions(
    @BuilderInference builder: PushSubscribeOptions.Builder.() -> Unit,
): PushSubscribeOptions = PushSubscribeOptions.builder().apply(builder).build()

/**
 * 스트림 이름으로 [PushSubscribeOptions]를 생성합니다.
 *
 * @param stream 스트림 이름
 * @return [PushSubscribeOptions] 인스턴스
 */
fun pushSubscriptionOf(stream: String): PushSubscribeOptions {
    stream.requireNotBlank("stream")

    return PushSubscribeOptions.stream(stream)
}

/**
 * 스트림과 durable 이름으로 바인드된 [PushSubscribeOptions]를 생성합니다.
 *
 * @param stream 스트림 이름
 * @param durable durable consumer 이름
 * @return [PushSubscribeOptions] 인스턴스
 */
fun pushSubscriptionOf(
    stream: String,
    durable: String,
): PushSubscribeOptions {
    stream.requireNotBlank("stream")
    durable.requireNotBlank("durable")

    return PushSubscribeOptions.bind(stream, durable)
}
