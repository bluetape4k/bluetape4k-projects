package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupResponse
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamResponse
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse

/**
 * [logGroupName]으로 CloudWatch Logs 그룹을 코루틴으로 생성합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchLogsAsyncClient.createLogGroup("/aws/lambda/my-function")
 * ```
 */
suspend fun CloudWatchLogsAsyncClient.createLogGroup(
    logGroupName: String,
): CreateLogGroupResponse {
    logGroupName.requireNotBlank("logGroupName")
    return createLogGroup { it.logGroupName(logGroupName) }.await()
}

/**
 * [logGroupName]과 [logStreamName]으로 CloudWatch Logs 스트림을 코루틴으로 생성합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [logStreamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchLogsAsyncClient.createLogStream(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123"
 * )
 * ```
 */
suspend fun CloudWatchLogsAsyncClient.createLogStream(
    logGroupName: String,
    logStreamName: String,
): CreateLogStreamResponse {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return createLogStream {
        it.logGroupName(logGroupName)
        it.logStreamName(logStreamName)
    }.await()
}

/**
 * [logGroupName]의 [logStreamName] 스트림에 [logEvents] 목록을 코루틴으로 게시합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [logStreamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchLogsAsyncClient.putLogEvents(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123",
 *     logEvents = listOf(inputLogEvent)
 * )
 * ```
 */
suspend fun CloudWatchLogsAsyncClient.putLogEvents(
    logGroupName: String,
    logStreamName: String,
    logEvents: List<InputLogEvent>,
): PutLogEventsResponse {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return putLogEvents {
        it.logGroupName(logGroupName)
        it.logStreamName(logStreamName)
        it.logEvents(logEvents)
    }.await()
}

/**
 * CloudWatch Logs 그룹 목록을 코루틴으로 조회합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsAsyncClient.describeLogGroups(logGroupNamePrefix = "/aws/lambda")
 * response.logGroups().forEach { group -> println(group.logGroupName()) }
 * ```
 */
suspend fun CloudWatchLogsAsyncClient.describeLogGroups(
    logGroupNamePrefix: String? = null,
): DescribeLogGroupsResponse =
    describeLogGroups {
        logGroupNamePrefix?.let { prefix -> it.logGroupNamePrefix(prefix) }
    }.await()

/**
 * [logGroupName]의 로그 스트림 목록을 코루틴으로 조회합니다.
 *
 * ## 동작/계약
 * - [logGroupName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchLogsAsyncClient.describeLogStreams("/aws/lambda/my-function")
 * response.logStreams().forEach { stream -> println(stream.logStreamName()) }
 * ```
 */
suspend fun CloudWatchLogsAsyncClient.describeLogStreams(
    logGroupName: String,
    logStreamNamePrefix: String? = null,
): DescribeLogStreamsResponse {
    logGroupName.requireNotBlank("logGroupName")
    return describeLogStreams {
        it.logGroupName(logGroupName)
        logStreamNamePrefix?.let { prefix -> it.logStreamNamePrefix(prefix) }
    }.await()
}
