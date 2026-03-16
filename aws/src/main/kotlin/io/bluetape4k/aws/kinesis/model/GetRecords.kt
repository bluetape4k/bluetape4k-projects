package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest

/**
 * DSL 블록으로 [GetRecordsRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = getRecordsRequest {
 *     shardIterator("AAA...")
 *     limit(100)
 * }
 * ```
 */
inline fun getRecordsRequest(
    builder: GetRecordsRequest.Builder.() -> Unit,
): GetRecordsRequest =
    GetRecordsRequest.builder().apply(builder).build()

/**
 * 샤드 이터레이터와 한도로 [GetRecordsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [shardIterator]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = getRecordsRequestOf(shardIterator = "AAA...", limit = 50)
 * ```
 */
inline fun getRecordsRequestOf(
    shardIterator: String,
    limit: Int = 100,
    builder: GetRecordsRequest.Builder.() -> Unit = {},
): GetRecordsRequest {
    shardIterator.requireNotBlank("shardIterator")
    return getRecordsRequest {
        shardIterator(shardIterator)
        limit(limit)
        builder()
    }
}
