package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest

/**
 * DSL 블록으로 [DescribeStreamRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = describeStreamRequest {
 *     streamName("my-stream")
 * }
 * ```
 */
inline fun describeStreamRequest(
    builder: DescribeStreamRequest.Builder.() -> Unit,
): DescribeStreamRequest =
    DescribeStreamRequest.builder().apply(builder).build()

/**
 * 스트림 이름으로 [DescribeStreamRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = describeStreamRequestOf("my-stream")
 * ```
 */
inline fun describeStreamRequestOf(
    streamName: String,
    builder: DescribeStreamRequest.Builder.() -> Unit = {},
): DescribeStreamRequest {
    streamName.requireNotBlank("streamName")
    return describeStreamRequest {
        streamName(streamName)
        builder()
    }
}
