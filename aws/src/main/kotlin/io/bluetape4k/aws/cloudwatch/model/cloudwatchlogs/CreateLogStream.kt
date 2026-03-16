package io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest

/**
 * DSL 블록으로 [CreateLogStreamRequest]를 빌드합니다.
 *
 * ```kotlin
 * val request = createLogStreamRequest {
 *     logGroupName("/aws/lambda/my-function")
 *     logStreamName("2024/01/01/[$LATEST]abc123")
 * }
 * ```
 */
inline fun createLogStreamRequest(
    @BuilderInference builder: CreateLogStreamRequest.Builder.() -> Unit,
): CreateLogStreamRequest =
    CreateLogStreamRequest.builder().apply(builder).build()

/**
 * 로그 그룹 이름과 스트림 이름으로 [CreateLogStreamRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [logStreamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val request = createLogStreamRequestOf(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123"
 * )
 * ```
 */
inline fun createLogStreamRequestOf(
    logGroupName: String,
    logStreamName: String,
    @BuilderInference builder: CreateLogStreamRequest.Builder.() -> Unit = {},
): CreateLogStreamRequest {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return createLogStreamRequest {
        logGroupName(logGroupName)
        logStreamName(logStreamName)
        builder()
    }
}
