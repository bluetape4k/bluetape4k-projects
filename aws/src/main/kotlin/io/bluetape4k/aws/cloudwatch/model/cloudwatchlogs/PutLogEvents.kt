package io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest

/**
 * DSL 블록으로 [PutLogEventsRequest]를 빌드합니다.
 *
 * ```kotlin
 * val request = putLogEventsRequest {
 *     logGroupName("/aws/lambda/my-function")
 *     logStreamName("2024/01/01/[$LATEST]abc123")
 *     logEvents(listOf(inputLogEvent))
 * }
 * ```
 */
inline fun putLogEventsRequest(
    @BuilderInference builder: PutLogEventsRequest.Builder.() -> Unit,
): PutLogEventsRequest =
    PutLogEventsRequest.builder().apply(builder).build()

/**
 * 로그 그룹/스트림 이름과 이벤트 목록으로 [PutLogEventsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [logStreamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val request = putLogEventsRequestOf(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123",
 *     logEvents = listOf(inputLogEvent)
 * )
 * ```
 */
inline fun putLogEventsRequestOf(
    logGroupName: String,
    logStreamName: String,
    logEvents: List<InputLogEvent>,
    @BuilderInference builder: PutLogEventsRequest.Builder.() -> Unit = {},
): PutLogEventsRequest {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return putLogEventsRequest {
        logGroupName(logGroupName)
        logStreamName(logStreamName)
        logEvents(logEvents)
        builder()
    }
}

/**
 * DSL 블록으로 [InputLogEvent]를 빌드합니다.
 *
 * ```kotlin
 * val event = inputLogEvent {
 *     timestamp(System.currentTimeMillis())
 *     message("Hello, CloudWatch Logs!")
 * }
 * ```
 */
inline fun inputLogEvent(
    @BuilderInference builder: InputLogEvent.Builder.() -> Unit,
): InputLogEvent =
    InputLogEvent.builder().apply(builder).build()

/**
 * 타임스탬프와 메시지로 [InputLogEvent]를 생성합니다.
 *
 * ```kotlin
 * val event = inputLogEventOf(
 *     timestamp = System.currentTimeMillis(),
 *     message = "Hello, CloudWatch Logs!"
 * )
 * ```
 */
inline fun inputLogEventOf(
    timestamp: Long,
    message: String,
    @BuilderInference builder: InputLogEvent.Builder.() -> Unit = {},
): InputLogEvent = inputLogEvent {
    timestamp(timestamp)
    message(message)
    builder()
}
