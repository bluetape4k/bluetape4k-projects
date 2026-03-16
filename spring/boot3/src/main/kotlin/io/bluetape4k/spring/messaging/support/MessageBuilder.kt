package io.bluetape4k.spring.messaging.support

import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder

/**
 * 페이로드와 빌더 블록으로 [Message]를 생성합니다.
 *
 * ## 동작/계약
 * - [MessageBuilder.withPayload]로 빌더를 만들고 [builder]를 적용한 뒤 `build()`합니다.
 * - 헤더 설정은 [builder] 블록에서 직접 수행합니다.
 *
 * ```kotlin
 * val msg = message("payload") {
 *     setHeader("key", "value")
 * }
 * // msg.payload == "payload"
 * ```
 */
inline fun <T: Any> message(
    payload: T,
    @BuilderInference builder: MessageBuilder<T>.() -> Unit = {},
): Message<T> {
    return MessageBuilder.withPayload(payload).apply(builder).build()
}

/**
 * [message]의 별칭으로 [Message]를 생성합니다.
 *
 * ## 동작/계약
 * - 구현은 [message]를 그대로 호출합니다.
 * - 전달한 [payload], [builder]가 동일하게 적용됩니다.
 *
 * ```kotlin
 * val msg = messageOf("payload")
 * // msg.payload == "payload"
 * ```
 */
inline fun <T: Any> messageOf(
    payload: T,
    @BuilderInference builder: MessageBuilder<T>.() -> Unit = {},
): Message<T> = message(payload, builder)

/**
 * 초기 헤더 맵과 빌더 블록으로 [Message]를 생성합니다.
 *
 * ## 동작/계약
 * - [headers]의 항목을 먼저 `setHeader`로 설정한 뒤 [builder]를 실행합니다.
 * - 동일 키를 [builder]에서 다시 설정하면 마지막 설정값이 적용됩니다.
 *
 * ```kotlin
 * val msg = messageOf("payload", mapOf("a" to 1)) {
 *     setHeader("b", 2)
 * }
 * // msg.headers["a"] == 1
 * ```
 */
inline fun <T: Any> messageOf(
    payload: T,
    headers: Map<String, Any?> = emptyMap(),
    @BuilderInference builder: MessageBuilder<T>.() -> Unit = {},
): Message<T> =
    message(payload) {
        headers.forEach { (name, value) ->
            setHeader(name, value)
        }
        builder()
    }
