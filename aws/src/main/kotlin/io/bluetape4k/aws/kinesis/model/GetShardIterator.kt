package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType

/**
 * DSL 블록으로 [GetShardIteratorRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = getShardIteratorRequest {
 *     streamName("my-stream")
 *     shardId("shardId-000000000000")
 *     shardIteratorType(ShardIteratorType.TRIM_HORIZON)
 * }
 * ```
 */
inline fun getShardIteratorRequest(
    builder: GetShardIteratorRequest.Builder.() -> Unit,
): GetShardIteratorRequest =
    GetShardIteratorRequest.builder().apply(builder).build()

/**
 * 스트림 이름, 샤드 ID, 이터레이터 타입으로 [GetShardIteratorRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [shardId]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = getShardIteratorRequestOf(
 *     streamName = "my-stream",
 *     shardId = "shardId-000000000000",
 *     type = ShardIteratorType.TRIM_HORIZON
 * )
 * ```
 */
inline fun getShardIteratorRequestOf(
    streamName: String,
    shardId: String,
    type: ShardIteratorType = ShardIteratorType.TRIM_HORIZON,
    builder: GetShardIteratorRequest.Builder.() -> Unit = {},
): GetShardIteratorRequest {
    streamName.requireNotBlank("streamName")
    shardId.requireNotBlank("shardId")
    return getShardIteratorRequest {
        streamName(streamName)
        shardId(shardId)
        shardIteratorType(type)
        builder()
    }
}
