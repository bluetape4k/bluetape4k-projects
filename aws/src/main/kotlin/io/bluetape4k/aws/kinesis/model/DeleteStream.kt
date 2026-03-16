package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest

/**
 * DSL 블록으로 [DeleteStreamRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = deleteStreamRequest {
 *     streamName("my-stream")
 * }
 * ```
 */
inline fun deleteStreamRequest(
    @BuilderInference builder: DeleteStreamRequest.Builder.() -> Unit,
): DeleteStreamRequest =
    DeleteStreamRequest.builder().apply(builder).build()

/**
 * 스트림 이름으로 [DeleteStreamRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = deleteStreamRequestOf("my-stream")
 * ```
 */
inline fun deleteStreamRequestOf(
    streamName: String,
    @BuilderInference builder: DeleteStreamRequest.Builder.() -> Unit = {},
): DeleteStreamRequest {
    streamName.requireNotBlank("streamName")
    return deleteStreamRequest {
        streamName(streamName)
        builder()
    }
}
