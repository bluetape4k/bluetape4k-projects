package io.bluetape4k.aws.kinesis.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry

/**
 * DSL 블록으로 [PutRecordsRequest]를 빌드합니다.
 *
 * ```kotlin
 * val req = putRecordsRequest {
 *     streamName("my-stream")
 *     records(entries)
 * }
 * ```
 */
inline fun putRecordsRequest(
    builder: PutRecordsRequest.Builder.() -> Unit,
): PutRecordsRequest =
    PutRecordsRequest.builder().apply(builder).build()

/**
 * 스트림 이름과 레코드 목록으로 [PutRecordsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = putRecordsRequestOf("my-stream", entries)
 * ```
 */
inline fun putRecordsRequestOf(
    streamName: String,
    entries: List<PutRecordsRequestEntry>,
    builder: PutRecordsRequest.Builder.() -> Unit = {},
): PutRecordsRequest {
    streamName.requireNotBlank("streamName")
    return putRecordsRequest {
        streamName(streamName)
        records(entries)
        builder()
    }
}

/**
 * 파티션 키와 데이터로 [PutRecordsRequestEntry]를 생성합니다.
 *
 * ## 동작/계약
 * - [partitionKey]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val entry = putRecordsRequestEntryOf(
 *     partitionKey = "pk",
 *     data = SdkBytes.fromUtf8String("hello")
 * )
 * ```
 */
fun putRecordsRequestEntryOf(
    partitionKey: String,
    data: SdkBytes,
): PutRecordsRequestEntry {
    partitionKey.requireNotBlank("partitionKey")
    return PutRecordsRequestEntry.builder()
        .partitionKey(partitionKey)
        .data(data)
        .build()
}
