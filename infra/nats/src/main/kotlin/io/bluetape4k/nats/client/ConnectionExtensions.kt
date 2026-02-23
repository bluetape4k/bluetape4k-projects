package io.bluetape4k.nats.client

import io.bluetape4k.support.requireGe
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
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
    subject.requireNotBlank("subject")
    body.requireNotEmpty("body")

    publish(subject, headers, body.toUtf8Bytes())
}

fun Connection.publish(
    subject: String,
    replyTo: String,
    body: String,
    headers: Headers? = null,
) {
    subject.requireNotBlank("subject")
    replyTo.requireNotBlank("replyTo")
    body.requireNotEmpty("body")

    publish(subject, replyTo, headers, body.toUtf8Bytes())
}

fun Connection.request(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): Message? {
    subject.requireNotBlank("subject")
    return request(subject, headers, body?.toUtf8Bytes(), timeout?.toJavaDuration())
}

fun Connection.request(
    message: Message,
    timeout: Duration? = null,
): Message? {
    return request(message, timeout?.toJavaDuration())
}

fun Connection.requestAsync(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): CompletableFuture<Message> {
    subject.requireNotBlank("subject")

    return if (timeout == null) request(subject, headers, body?.toUtf8Bytes())
    else requestWithTimeout(subject, headers, body?.toUtf8Bytes(), timeout.toJavaDuration())
}

fun Connection.requestAsync(
    message: Message,
    timeout: Duration? = null,
): CompletableFuture<Message> {
    return if (timeout == null) request(message) else requestWithTimeout(message, timeout.toJavaDuration())
}

suspend fun Connection.requestSuspending(
    message: Message,
    timeout: Duration? = null,
): Message {
    // timeout 이 없으면 일반 request 경로를 사용하여 Java API에 null timeout 을 전달하지 않는다.
    return if (timeout == null) {
        request(message).await()
    } else {
        requestWithTimeout(message, timeout.toJavaDuration()).await()
    }
}

suspend fun Connection.requestSuspending(
    subject: String,
    body: ByteArray,
    headers: Headers? = null,
): Message {
    subject.requireNotBlank("subject")
    return request(subject, headers, body).await()
}


suspend fun Connection.requestWithTimeoutSuspending(
    subject: String,
    body: ByteArray,
    headers: Headers? = null,
    timeout: Duration? = null,
): Message {
    subject.requireNotBlank("subject")
    // timeout 이 null 이면 즉시 request 경로로 위임한다.
    return if (timeout == null) {
        request(subject, headers, body).await()
    } else {
        requestWithTimeout(subject, headers, body, timeout.toJavaDuration()).await()
    }
}

suspend fun Connection.drainSuspending(timeout: kotlin.time.Duration): Boolean {
    timeout.requireGe(kotlin.time.Duration.ZERO, "timeout")
    return drain(timeout.toJavaDuration()).await()
}

fun Connection.flush(timeout: Duration) {
    flush(timeout.toJavaDuration())
}

fun Connection.createStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createStream(streamName, storageType, *subjects)
}

fun Connection.createOrReplaceStream(
    streamName: String,
    subject: String,
): StreamInfo = createOrReplaceStream(streamName, subjects = arrayOf(subject))

fun Connection.createOrReplaceStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createOrReplaceStream(streamName, storageType, *subjects)
}

fun Connection.createStreamOrUpdateSubjects(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createStreamOrUpdateSubjects(streamName, storageType, *subjects)
}
