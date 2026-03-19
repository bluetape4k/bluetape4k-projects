package io.bluetape4k.aws.kotlin.kinesis.model

import aws.sdk.kotlin.services.kinesis.model.GetShardIteratorRequest
import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType
import io.bluetape4k.support.requireNotBlank

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
 *     type = ShardIteratorType.TrimHorizon
 * )
 * ```
 */
inline fun getShardIteratorRequestOf(
    streamName: String,
    shardId: String,
    type: ShardIteratorType = ShardIteratorType.TrimHorizon,
    crossinline builder: GetShardIteratorRequest.Builder.() -> Unit = {},
): GetShardIteratorRequest {
    streamName.requireNotBlank("streamName")
    shardId.requireNotBlank("shardId")
    return GetShardIteratorRequest {
        this.streamName = streamName
        this.shardId = shardId
        this.shardIteratorType = type
        builder()
    }
}
