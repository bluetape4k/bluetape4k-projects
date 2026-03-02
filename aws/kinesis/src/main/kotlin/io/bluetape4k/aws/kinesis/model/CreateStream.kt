package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest

/**
 * DSL 블록으로 [CreateStreamRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = createStreamRequest {
 *     streamName("my-stream")
 *     shardCount(1)
 * }
 * ```
 */
inline fun createStreamRequest(
    @BuilderInference builder: CreateStreamRequest.Builder.() -> Unit,
): CreateStreamRequest =
    CreateStreamRequest.builder().apply(builder).build()

/**
 * 스트림 이름과 샤드 수로 [CreateStreamRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = createStreamRequestOf("my-stream", shardCount = 2)
 * // req.streamName() == "my-stream"
 * // req.shardCount() == 2
 * ```
 */
inline fun createStreamRequestOf(
    streamName: String,
    shardCount: Int = 1,
    @BuilderInference builder: CreateStreamRequest.Builder.() -> Unit = {},
): CreateStreamRequest {
    streamName.requireNotBlank("streamName")
    return createStreamRequest {
        streamName(streamName)
        shardCount(shardCount)
        builder()
    }
}
