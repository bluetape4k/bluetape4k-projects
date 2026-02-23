package io.bluetape4k.nats.client

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.JetStream
import io.nats.client.PublishOptions
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

fun JetStream.publish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck {
    subject.requireNotBlank("subject")
    return publish(subject, headers, body?.toUtf8Bytes(), options)
}

fun JetStream.publishAsync(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): CompletableFuture<PublishAck> {
    subject.requireNotBlank("subject")
    return publishAsync(subject, headers, body?.toUtf8Bytes(), options)
}

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
suspend fun JetStream.coPublish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck =
    publishSuspending(subject, body, headers, options)
