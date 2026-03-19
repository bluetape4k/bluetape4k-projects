package io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest

/**
 * DSL 블록으로 [CreateLogGroupRequest]를 빌드합니다.
 *
 * ```kotlin
 * val request = createLogGroupRequest {
 *     logGroupName("/aws/lambda/my-function")
 * }
 * ```
 */
inline fun createLogGroupRequest(
    builder: CreateLogGroupRequest.Builder.() -> Unit,
): CreateLogGroupRequest =
    CreateLogGroupRequest.builder().apply(builder).build()

/**
 * 로그 그룹 이름으로 [CreateLogGroupRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val request = createLogGroupRequestOf("/aws/lambda/my-function")
 * // request.logGroupName() == "/aws/lambda/my-function"
 * ```
 */
inline fun createLogGroupRequestOf(
    logGroupName: String,
    builder: CreateLogGroupRequest.Builder.() -> Unit = {},
): CreateLogGroupRequest {
    logGroupName.requireNotBlank("logGroupName")
    return createLogGroupRequest {
        logGroupName(logGroupName)
        builder()
    }
}
