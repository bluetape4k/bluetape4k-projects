package io.bluetape4k.nats.client

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.JetStream
import io.nats.client.PublishOptions
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import java.util.concurrent.CompletableFuture

fun JetStream.publish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck =
    publish(subject, headers, body?.toUtf8Bytes(), options)

fun JetStream.publishAsync(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): CompletableFuture<PublishAck> =
    publishAsync(subject, headers, body?.toUtf8Bytes(), options)

suspend fun JetStream.coPublish(
    subject: String,
    body: String? = null,
    headers: Headers? = null,
    options: PublishOptions? = null,
): PublishAck =
    publishAsync(subject, headers, body?.toUtf8Bytes(), options).suspendAwait()
