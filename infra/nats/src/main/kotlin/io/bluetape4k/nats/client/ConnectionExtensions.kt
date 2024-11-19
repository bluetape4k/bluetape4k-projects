package io.bluetape4k.nats.client

import io.bluetape4k.support.toUtf8Bytes
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.api.StorageType
import io.nats.client.api.StreamInfo
import io.nats.client.impl.Headers
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun Connection.publish(subject: String, body: String, headers: Headers? = null) {
    publish(subject, headers, body.toUtf8Bytes())
}

fun Connection.publish(
    subject: String,
    replyTo: String,
    body: String,
    headers: Headers? = null,
) {
    publish(subject, replyTo, headers, body.toUtf8Bytes())
}

fun Connection.request(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): Message {
    return request(subject, headers, body?.toUtf8Bytes(), timeout?.toJavaDuration())
}

fun Connection.request(
    message: Message,
    timeout: Duration? = null,
): Message {
    return request(message, timeout?.toJavaDuration())
}

fun Connection.requestAsync(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): CompletableFuture<Message> {
    return if (timeout == null) request(subject, headers, body?.toUtf8Bytes())
    else requestWithTimeout(subject, headers, body?.toUtf8Bytes(), timeout.toJavaDuration())
}

fun Connection.requestAsync(message: Message, timeout: Duration? = null): CompletableFuture<Message> {
    return if (timeout == null) request(message) else requestWithTimeout(message, timeout.toJavaDuration())
}

suspend fun Connection.requestSuspending(
    message: Message,
    timeout: Duration? = null,
): Message {
    return requestWithTimeout(message, timeout?.toJavaDuration()).await()
}

suspend fun Connection.requestSuspending(
    subject: String,
    body: ByteArray,
    headers: Headers? = null,
): Message {
    return request(subject, headers, body).await()
}


suspend fun Connection.requestWithTimeoutSuspending(
    subject: String,
    body: ByteArray,
    headers: Headers? = null,
    timeout: Duration? = null,
): Message {
    return requestWithTimeout(subject, headers, body, timeout?.toJavaDuration()).await()
}

suspend fun Connection.drainSuspending(timeout: Duration): Boolean =
    drain(timeout.toJavaDuration()).await()

fun Connection.flush(timeout: Duration) {
    flush(timeout.toJavaDuration())
}


fun Connection.createStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo =
    jetStreamManagement().createStream(streamName, storageType, *subjects)

fun Connection.createOrReplaceStream(
    streamName: String,
    subject: String,
): StreamInfo = createOrReplaceStream(streamName, subjects = arrayOf(subject))

fun Connection.createOrReplaceStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo =
    jetStreamManagement().createOrReplaceStream(streamName, storageType, *subjects)

fun Connection.createStreamOrUpdateSubjects(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo =
    jetStreamManagement().createStreamOrUpdateSubjects(streamName, storageType, *subjects)
