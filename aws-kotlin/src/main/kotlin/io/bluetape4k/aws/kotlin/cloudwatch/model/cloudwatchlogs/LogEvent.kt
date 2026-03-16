package io.bluetape4k.aws.kotlin.cloudwatch.model.cloudwatchlogs

import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent

/**
 * DSL 블록으로 [InputLogEvent]를 빌드합니다.
 *
 * ```kotlin
 * val event = inputLogEvent {
 *     timestamp = System.currentTimeMillis()
 *     message = "Hello, CloudWatch Logs!"
 * }
 * ```
 */
inline fun inputLogEvent(
    crossinline builder: InputLogEvent.Builder.() -> Unit,
): InputLogEvent =
    InputLogEvent { builder() }

/**
 * 타임스탬프와 메시지로 [InputLogEvent]를 생성합니다.
 *
 * ```kotlin
 * val event = inputLogEventOf(
 *     timestamp = System.currentTimeMillis(),
 *     message = "Hello, CloudWatch Logs!"
 * )
 * ```
 *
 * @param timestamp 이벤트 타임스탬프 (Unix epoch milliseconds)
 * @param message 로그 메시지
 * @param builder [InputLogEvent.Builder]에 대한 추가 설정 람다
 * @return [InputLogEvent] 인스턴스
 */
inline fun inputLogEventOf(
    timestamp: Long,
    message: String,
    crossinline builder: InputLogEvent.Builder.() -> Unit = {},
): InputLogEvent = inputLogEvent {
    this.timestamp = timestamp
    this.message = message
    builder()
}
