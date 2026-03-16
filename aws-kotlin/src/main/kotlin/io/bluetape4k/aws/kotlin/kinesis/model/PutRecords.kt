package io.bluetape4k.aws.kotlin.kinesis.model

import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry
import io.bluetape4k.support.requireNotBlank

/**
 * 파티션 키와 데이터로 [PutRecordsRequestEntry]를 생성합니다.
 *
 * ## 동작/계약
 * - [partitionKey]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val entry = putRecordsRequestEntryOf(
 *     partitionKey = "pk",
 *     data = "hello".toByteArray()
 * )
 * ```
 */
inline fun putRecordsRequestEntryOf(
    partitionKey: String,
    data: ByteArray,
    crossinline builder: PutRecordsRequestEntry.Builder.() -> Unit = {},
): PutRecordsRequestEntry {
    partitionKey.requireNotBlank("partitionKey")
    return PutRecordsRequestEntry {
        this.partitionKey = partitionKey
        this.data = data
        builder()
    }
}
