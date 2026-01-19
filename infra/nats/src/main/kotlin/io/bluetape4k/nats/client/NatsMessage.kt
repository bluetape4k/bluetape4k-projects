package io.bluetape4k.nats.client

import io.nats.client.Message
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage

inline fun natsMessage(
    @BuilderInference initializer: NatsMessage.Builder.() -> Unit,
): NatsMessage =
    NatsMessage
        .builder()
        .apply(initializer)
        .build()

fun natsMessageOf(message: Message) = NatsMessage(message)

fun natsMessageOf(
    subject: String,
    data: ByteArray?,
    replyTo: String? = null,
    headers: Headers? = null,
): NatsMessage = natsMessage {
    subject(subject)
    data(data)
    replyTo?.run { replyTo(this) }
    headers?.run { headers(this) }
}

fun natsMessageOf(
    subject: String,
    data: String?,
    replyTo: String? = null,
    headers: Headers? = null,
): NatsMessage = natsMessage {
    subject(subject)
    data(data)
    replyTo?.run { replyTo(this) }
    headers?.run { headers(this) }
}
