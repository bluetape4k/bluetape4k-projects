package io.bluetape4k.aws.kotlin.kinesis.model

import aws.sdk.kotlin.services.kinesis.model.PutRecordRequest
import io.bluetape4k.support.requireNotBlank

/**
 * 스트림 이름, 파티션 키, 데이터로 [PutRecordRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [streamName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [partitionKey]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = putRecordRequestOf(
 *     streamName = "my-stream",
 *     partitionKey = "pk",
 *     data = "hello".toByteArray()
 * )
 * ```
 */
inline fun putRecordRequestOf(
    streamName: String,
    partitionKey: String,
    data: ByteArray,
    @BuilderInference crossinline builder: PutRecordRequest.Builder.() -> Unit = {},
): PutRecordRequest {
    streamName.requireNotBlank("streamName")
    partitionKey.requireNotBlank("partitionKey")
    return PutRecordRequest {
        this.streamName = streamName
        this.partitionKey = partitionKey
        this.data = data
        builder()
    }
}
