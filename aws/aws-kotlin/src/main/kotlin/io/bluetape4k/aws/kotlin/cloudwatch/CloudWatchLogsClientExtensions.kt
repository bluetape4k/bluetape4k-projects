package io.bluetape4k.aws.kotlin.cloudwatch

import aws.sdk.kotlin.services.cloudwatchlogs.CloudWatchLogsClient
import aws.sdk.kotlin.services.cloudwatchlogs.createLogGroup
import aws.sdk.kotlin.services.cloudwatchlogs.createLogStream
import aws.sdk.kotlin.services.cloudwatchlogs.describeLogGroups
import aws.sdk.kotlin.services.cloudwatchlogs.describeLogStreams
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogGroupRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogGroupResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.CreateLogStreamResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogGroupsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogGroupsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.DescribeLogStreamsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.model.InputLogEvent
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsRequest
import aws.sdk.kotlin.services.cloudwatchlogs.model.PutLogEventsResponse
import aws.sdk.kotlin.services.cloudwatchlogs.putLogEvents
import io.bluetape4k.support.requireNotBlank

/**
 * [logGroupName]으로 CloudWatch Logs 그룹을 생성합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsClient.createLogGroup("/aws/lambda/my-function")
 * ```
 *
 * @param logGroupName 생성할 로그 그룹 이름
 * @param builder [CreateLogGroupRequest.Builder]에 대한 추가 설정 람다
 * @return [CreateLogGroupResponse] 인스턴스
 */
suspend inline fun CloudWatchLogsClient.createLogGroup(
    logGroupName: String,
    crossinline builder: CreateLogGroupRequest.Builder.() -> Unit = {},
): CreateLogGroupResponse {
    logGroupName.requireNotBlank("logGroupName")
    return createLogGroup {
        this.logGroupName = logGroupName
        builder()
    }
}

/**
 * [logGroupName]과 [logStreamName]으로 CloudWatch Logs 스트림을 생성합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsClient.createLogStream(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123"
 * )
 * ```
 *
 * @param logGroupName 로그 그룹 이름
 * @param logStreamName 생성할 로그 스트림 이름
 * @param builder [CreateLogStreamRequest.Builder]에 대한 추가 설정 람다
 * @return [CreateLogStreamResponse] 인스턴스
 */
suspend inline fun CloudWatchLogsClient.createLogStream(
    logGroupName: String,
    logStreamName: String,
    crossinline builder: CreateLogStreamRequest.Builder.() -> Unit = {},
): CreateLogStreamResponse {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return createLogStream {
        this.logGroupName = logGroupName
        this.logStreamName = logStreamName
        builder()
    }
}

/**
 * [logGroupName]의 [logStreamName] 스트림에 [logEvents] 목록을 게시합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsClient.putLogEvents(
 *     logGroupName = "/aws/lambda/my-function",
 *     logStreamName = "2024/01/01/[$LATEST]abc123",
 *     logEvents = listOf(inputLogEvent)
 * )
 * ```
 *
 * @param logGroupName 로그 그룹 이름
 * @param logStreamName 로그 스트림 이름
 * @param logEvents 게시할 로그 이벤트 목록
 * @param builder [PutLogEventsRequest.Builder]에 대한 추가 설정 람다
 * @return [PutLogEventsResponse] 인스턴스
 */
suspend inline fun CloudWatchLogsClient.putLogEvents(
    logGroupName: String,
    logStreamName: String,
    logEvents: List<InputLogEvent>,
    crossinline builder: PutLogEventsRequest.Builder.() -> Unit = {},
): PutLogEventsResponse {
    logGroupName.requireNotBlank("logGroupName")
    logStreamName.requireNotBlank("logStreamName")
    return putLogEvents {
        this.logGroupName = logGroupName
        this.logStreamName = logStreamName
        this.logEvents = logEvents
        builder()
    }
}

/**
 * CloudWatch Logs 그룹 목록을 조회합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsClient.describeLogGroups(logGroupNamePrefix = "/aws/lambda")
 * response.logGroups?.forEach { group -> println(group.logGroupName) }
 * ```
 *
 * @param logGroupNamePrefix 조회할 로그 그룹 이름 접두어. null이면 전체 조회합니다.
 * @param builder [DescribeLogGroupsRequest.Builder]에 대한 추가 설정 람다
 * @return [DescribeLogGroupsResponse] 인스턴스
 */
suspend inline fun CloudWatchLogsClient.describeLogGroups(
    logGroupNamePrefix: String? = null,
    crossinline builder: DescribeLogGroupsRequest.Builder.() -> Unit = {},
): DescribeLogGroupsResponse =
    describeLogGroups {
        logGroupNamePrefix?.let { this.logGroupNamePrefix = it }
        builder()
    }

/**
 * [logGroupName]의 로그 스트림 목록을 조회합니다.
 *
 * ```kotlin
 * val response = cloudWatchLogsClient.describeLogStreams("/aws/lambda/my-function")
 * response.logStreams?.forEach { stream -> println(stream.logStreamName) }
 * ```
 *
 * @param logGroupName 로그 그룹 이름
 * @param logStreamNamePrefix 조회할 로그 스트림 이름 접두어. null이면 필터하지 않습니다.
 * @param builder [DescribeLogStreamsRequest.Builder]에 대한 추가 설정 람다
 * @return [DescribeLogStreamsResponse] 인스턴스
 */
suspend inline fun CloudWatchLogsClient.describeLogStreams(
    logGroupName: String,
    logStreamNamePrefix: String? = null,
    crossinline builder: DescribeLogStreamsRequest.Builder.() -> Unit = {},
): DescribeLogStreamsResponse {
    logGroupName.requireNotBlank("logGroupName")
    return describeLogStreams {
        this.logGroupName = logGroupName
        logStreamNamePrefix?.let { this.logStreamNamePrefix = it }
        builder()
    }
}
