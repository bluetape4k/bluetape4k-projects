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

/**
 * 문자열 payload를 UTF-8로 변환해 NATS 메시지를 발행합니다.
 *
 * @param subject 발행 대상 subject
 * @param body 메시지 본문 (UTF-8 변환)
 * @param headers 메시지 헤더 (선택)
 */
fun Connection.publish(
    subject: String,
    body: String,
    headers: Headers? = null,
) {
    subject.requireNotBlank("subject")
    body.requireNotEmpty("body")

    publish(subject, headers, body.toUtf8Bytes())
}

/**
 * 문자열 payload를 UTF-8로 변환해 reply-to 주소 포함 NATS 메시지를 발행합니다.
 *
 * @param subject 발행 대상 subject
 * @param replyTo 응답 수신 subject
 * @param body 메시지 본문 (UTF-8 변환)
 * @param headers 메시지 헤더 (선택)
 */
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

/**
 * 문자열 payload로 동기 request를 전송합니다.
 *
 * @param subject 요청 대상 subject
 * @param body 요청 본문 (null 가능)
 * @param headers 메시지 헤더 (선택)
 * @param timeout 요청 타임아웃 (null이면 기본값)
 * @return 응답 [Message] 또는 null
 */
fun Connection.request(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): Message? {
    subject.requireNotBlank("subject")
    return request(subject, headers, body?.toUtf8Bytes(), timeout?.toJavaDuration())
}

/**
 * [Message] 객체로 동기 request를 전송합니다.
 *
 * @param message 요청 메시지
 * @param timeout 요청 타임아웃 (null이면 기본값)
 * @return 응답 [Message] 또는 null
 */
fun Connection.request(
    message: Message,
    timeout: Duration? = null,
): Message? = request(message, timeout?.toJavaDuration())

/**
 * 문자열 payload로 비동기 request를 전송합니다.
 *
 * @param subject 요청 대상 subject
 * @param body 요청 본문 (null 가능)
 * @param headers 메시지 헤더 (선택)
 * @param timeout 요청 타임아웃 (null이면 기본값)
 * @return 응답 [Message]를 담은 [CompletableFuture]
 */
fun Connection.requestAsync(
    subject: String,
    body: String?,
    headers: Headers? = null,
    timeout: Duration? = null,
): CompletableFuture<Message> {
    subject.requireNotBlank("subject")

    return if (timeout == null) {
        request(subject, headers, body?.toUtf8Bytes())
    } else {
        requestWithTimeout(subject, headers, body?.toUtf8Bytes(), timeout.toJavaDuration())
    }
}

/**
 * [Message] 객체로 비동기 request를 전송합니다.
 *
 * @param message 요청 메시지
 * @param timeout 요청 타임아웃 (null이면 기본값)
 * @return 응답 [Message]를 담은 [CompletableFuture]
 */
fun Connection.requestAsync(
    message: Message,
    timeout: Duration? = null,
): CompletableFuture<Message> =
    if (timeout == null) request(message) else requestWithTimeout(message, timeout.toJavaDuration())

/**
 * [Message] 객체로 request를 suspend 함수로 전송합니다.
 *
 * @param message 요청 메시지
 * @param timeout 요청 타임아웃 (null이면 기본값)
 * @return 응답 [Message]
 */
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

/**
 * 바이트 배열 payload로 request를 suspend 함수로 전송합니다.
 *
 * @param subject 요청 대상 subject
 * @param body 요청 본문 바이트 배열
 * @param headers 메시지 헤더 (선택)
 * @return 응답 [Message]
 */
suspend fun Connection.requestSuspending(
    subject: String,
    body: ByteArray,
    headers: Headers? = null,
): Message {
    subject.requireNotBlank("subject")
    return request(subject, headers, body).await()
}

/**
 * 타임아웃 지정 request를 suspend 함수로 전송합니다.
 *
 * @param subject 요청 대상 subject
 * @param body 요청 본문 바이트 배열
 * @param headers 메시지 헤더 (선택)
 * @param timeout 요청 타임아웃 (null이면 타임아웃 없이 전송)
 * @return 응답 [Message]
 */
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

/**
 * Kotlin [Duration] 기반으로 Connection drain을 suspend 함수로 실행합니다.
 *
 * @param timeout drain 타임아웃 (0 이상)
 * @return drain 성공 여부
 */
suspend fun Connection.drainSuspending(timeout: kotlin.time.Duration): Boolean {
    timeout.requireGe(kotlin.time.Duration.ZERO, "timeout")
    return drain(timeout.toJavaDuration()).await()
}

/**
 * Kotlin [Duration] 기반으로 Connection flush를 실행합니다.
 *
 * @param timeout flush 타임아웃
 */
fun Connection.flush(timeout: Duration) {
    flush(timeout.toJavaDuration())
}

/**
 * Connection을 통해 새 JetStream 스트림을 생성합니다.
 *
 * @param streamName 스트림 이름
 * @param storageType 저장소 유형 (기본값: Memory)
 * @param subjects 스트림에 포함할 subject 목록
 * @return 생성된 [StreamInfo]
 */
fun Connection.createStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createStream(streamName, storageType, *subjects)
}

/**
 * 단일 subject로 스트림을 교체 생성합니다.
 *
 * @param streamName 스트림 이름
 * @param subject subject 이름
 * @return 생성된 [StreamInfo]
 */
fun Connection.createOrReplaceStream(
    streamName: String,
    subject: String,
): StreamInfo = createOrReplaceStream(streamName, subjects = arrayOf(subject))

/**
 * 기존 스트림을 제거한 뒤 새 설정으로 다시 생성합니다.
 *
 * @param streamName 스트림 이름
 * @param storageType 저장소 유형 (기본값: Memory)
 * @param subjects 스트림에 포함할 subject 목록
 * @return 생성된 [StreamInfo]
 */
fun Connection.createOrReplaceStream(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createOrReplaceStream(streamName, storageType, *subjects)
}

/**
 * 스트림이 없으면 생성하고, 있으면 subject 집합을 업데이트합니다.
 *
 * @param streamName 스트림 이름
 * @param storageType 저장소 유형 (기본값: Memory)
 * @param subjects 스트림에 포함할 subject 목록
 * @return 생성 또는 업데이트된 [StreamInfo]
 */
fun Connection.createStreamOrUpdateSubjects(
    streamName: String,
    storageType: StorageType = StorageType.Memory,
    vararg subjects: String,
): StreamInfo {
    streamName.requireNotBlank("streamName")
    return jetStreamManagement().createStreamOrUpdateSubjects(streamName, storageType, *subjects)
}
