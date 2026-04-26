package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.nats.client.Message
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage

/**
 * NATS Java 클라이언트의 [NatsMessage]를 코틀린 DSL로 생성합니다.
 */
inline fun natsMessage(
    builder: NatsMessage.Builder.() -> Unit,
): NatsMessage =
    NatsMessage.builder().apply(builder).build()

/**
 * 기존 [Message]를 [NatsMessage] 래퍼로 변환합니다.
 */
fun natsMessageOf(message: Message) = NatsMessage(message)

/**
 * 바이너리 payload 기반의 [NatsMessage]를 생성합니다.
 */
fun natsMessageOf(
    subject: String,
    data: ByteArray?,
    replyTo: String? = null,
    headers: Headers? = null,
): NatsMessage {
    subject.requireNotBlank("subject")

    return natsMessage {
        subject(subject)
        data(data)
        replyTo?.run { replyTo(this) }
        headers?.run { headers(this) }
    }
}

/**
 * 문자열 payload 기반의 [NatsMessage]를 생성합니다.
 */
fun natsMessageOf(
    subject: String,
    data: String?,
    replyTo: String? = null,
    headers: Headers? = null,
): NatsMessage {
    subject.requireNotBlank("subject")

    return natsMessage {
        subject(subject)
        data(data)
        replyTo?.run { replyTo(this) }
        headers?.run { headers(this) }
    }
}
