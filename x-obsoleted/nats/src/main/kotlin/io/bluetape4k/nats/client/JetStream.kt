package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.JetStream
import io.nats.client.PublishOptions
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

/**
 * 문자열 payload를 UTF-8 바이트로 변환해 JetStream 메시지를 발행합니다.
 *
 * ## 동작/계약
 * - [subject]가 blank면 `requireNotBlank("subject")`로 `IllegalArgumentException`이 발생합니다.
 * - [body]가 null이면 payload 없이 발행합니다.
 * - 내부 `publish(subject, headers, bodyBytes, options)`를 호출합니다.
 *
 * ```kotlin
 * val ack = jetStream.publish("orders.created", "{\"id\":1}")
 * // ack.stream.isNotBlank() == true
 * ```
 */
fun JetStream.publish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck {
    subject.requireNotBlank("subject")
    return publish(subject, headers, body?.toUtf8Bytes(), options)
}

/**
 * 문자열 payload를 UTF-8 바이트로 변환해 비동기 발행합니다.
 *
 * ## 동작/계약
 * - [subject] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
 * - 반환값은 NATS ACK 결과를 담는 [CompletableFuture]입니다.
 * - 발행 실패는 future 예외로 전파됩니다.
 *
 * ```kotlin
 * val ack = jetStream.publishAsync("orders.created", "{\"id\":1}").get()
 * // ack.stream.isNotBlank() == true
 * ```
 */
fun JetStream.publishAsync(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): CompletableFuture<PublishAck> {
    subject.requireNotBlank("subject")
    return publishAsync(subject, headers, body?.toUtf8Bytes(), options)
}

/**
 * `publishAsync`를 suspend로 감싼 발행 유틸입니다.
 *
 * ## 동작/계약
 * - [subject] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
 * - 내부적으로 [publishAsync]를 호출하고 `await()`로 완료를 대기합니다.
 * - 코루틴 취소 시 `CancellationException`이 전파될 수 있습니다.
 *
 * ```kotlin
 * val ack = jetStream.publishSuspending("orders.created", "{\"id\":1}")
 * // ack.stream.isNotBlank() == true
 * ```
 */
suspend fun JetStream.publishSuspending(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck {
    subject.requireNotBlank("subject")
    return publishAsync(subject, body, headers, options).await()
}

@Deprecated(
    message = "use publishSuspending",
    replaceWith = ReplaceWith("publishSuspending(subject, body, headers, options)")
)
/** [publishSuspending]으로 대체된 하위 호환 API입니다. */
suspend fun JetStream.coPublish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck =
    publishSuspending(subject, body, headers, options)
