package io.bluetape4k.spring.messaging.support

import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder

/**
 * Spring Messaging의 [Message]를 생성합니다.
 *
 * ```
 * val message = message("payload") {
 *   setHeader("key", "value")
 *   setHeaderIfAbsent("key", "value")
 * }
 * ```
 *
 * @param payload 메시지 페이로드
 * @param builder 메시지 초기화 블록
 */
inline fun <T: Any> message(
    payload: T,
    @BuilderInference builder: MessageBuilder<T>.() -> Unit = {},
): Message<T> {
    return MessageBuilder.withPayload(payload).apply(builder).build()
}

/**
 * Spring Messaging의 [Message]를 생성합니다.
 *
 * ```
 * val message = messageOf("payload") {
 *      setHeader("key", "value")
 *      setHeaderIfAbsent("key", "value")
 * }
 * ```
 *
 * @param payload 메시지 페이로드
 * @param builder 메시지 초기화 블록
 * @return [Message] 메시지
 */
inline fun <T: Any> messageOf(
    payload: T,
    @BuilderInference builder: MessageBuilder<T>.() -> Unit = {},
): Message<T> = message(payload, builder)

/**
 * Spring Messaging의 [Message]를 생성합니다.
 *
 * ```
 * val message = messageOf("payload", mapOf("key" to "value")) {
 *      setHeaderIfAbsent("key", "value")
 *
 *      setReplyChannel(replyChannel)
 * }
 * ```
 *
 * @param payload 메시지 페이로드
 * @param builder 메시지 초기화 블록
 * @return [Message] 메시지
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
