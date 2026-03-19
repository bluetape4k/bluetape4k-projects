package io.bluetape4k.aws.kinesis

import io.bluetape4k.aws.kinesis.model.createStreamRequest
import io.bluetape4k.aws.kinesis.model.deleteStreamRequest
import io.bluetape4k.aws.kinesis.model.describeStreamRequest
import io.bluetape4k.aws.kinesis.model.getRecordsRequest
import io.bluetape4k.aws.kinesis.model.getShardIteratorRequest
import io.bluetape4k.aws.kinesis.model.putRecordRequest
import io.bluetape4k.aws.kinesis.model.putRecordsRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse
import software.amazon.awssdk.services.kinesis.model.DeleteStreamResponse
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType
import java.util.concurrent.CompletableFuture

/**
 * Kinesis 스트림을 비동기로 생성합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.createStreamAsync("my-stream", shardCount = 1).join()
 * ```
 */
fun KinesisAsyncClient.createStreamAsync(
    streamName: String,
    shardCount: Int = 1,
): CompletableFuture<CreateStreamResponse> {
    streamName.requireNotBlank("streamName")
    return createStream(createStreamRequest {
        streamName(streamName)
        shardCount(shardCount)
    })
}

/**
 * Kinesis 스트림에 단일 레코드를 비동기로 전송합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.putRecordAsync(
 *     streamName = "my-stream",
 *     partitionKey = "pk",
 *     data = SdkBytes.fromUtf8String("data")
 * ).join()
 * ```
 */
fun KinesisAsyncClient.putRecordAsync(
    streamName: String,
    partitionKey: String,
    data: SdkBytes,
): CompletableFuture<PutRecordResponse> {
    streamName.requireNotBlank("streamName")
    partitionKey.requireNotBlank("partitionKey")
    return putRecord(putRecordRequest {
        streamName(streamName)
        partitionKey(partitionKey)
        data(data)
    })
}

/**
 * Kinesis 스트림에 복수의 레코드를 비동기로 배치 전송합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.putRecordsAsync("my-stream", entries).join()
 * ```
 */
fun KinesisAsyncClient.putRecordsAsync(
    streamName: String,
    entries: List<PutRecordsRequestEntry>,
): CompletableFuture<PutRecordsResponse> {
    streamName.requireNotBlank("streamName")
    return putRecords(putRecordsRequest {
        streamName(streamName)
        records(entries)
    })
}

/**
 * Kinesis 스트림의 샤드 이터레이터를 비동기로 조회합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.getShardIteratorAsync(
 *     streamName = "my-stream",
 *     shardId = "shardId-000000000000",
 *     type = ShardIteratorType.TRIM_HORIZON
 * ).join()
 * ```
 */
fun KinesisAsyncClient.getShardIteratorAsync(
    streamName: String,
    shardId: String,
    type: ShardIteratorType = ShardIteratorType.TRIM_HORIZON,
): CompletableFuture<GetShardIteratorResponse> {
    streamName.requireNotBlank("streamName")
    shardId.requireNotBlank("shardId")
    return getShardIterator(getShardIteratorRequest {
        streamName(streamName)
        shardId(shardId)
        shardIteratorType(type)
    })
}

/**
 * Kinesis 샤드 이터레이터로부터 레코드를 비동기로 조회합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.getRecordsAsync(shardIterator, limit = 100).join()
 * ```
 */
fun KinesisAsyncClient.getRecordsAsync(
    shardIterator: String,
    limit: Int = 100,
): CompletableFuture<GetRecordsResponse> {
    shardIterator.requireNotBlank("shardIterator")
    return getRecords(getRecordsRequest {
        shardIterator(shardIterator)
        limit(limit)
    })
}

/**
 * Kinesis 스트림 상세 정보를 비동기로 조회합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.describeStreamAsync("my-stream").join()
 * ```
 */
fun KinesisAsyncClient.describeStreamAsync(
    streamName: String,
): CompletableFuture<DescribeStreamResponse> {
    streamName.requireNotBlank("streamName")
    return describeStream(describeStreamRequest {
        streamName(streamName)
    })
}

/**
 * Kinesis 스트림을 비동기로 삭제합니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.deleteStreamAsync("my-stream").join()
 * ```
 */
fun KinesisAsyncClient.deleteStreamAsync(
    streamName: String,
): CompletableFuture<DeleteStreamResponse> {
    streamName.requireNotBlank("streamName")
    return deleteStream(deleteStreamRequest {
        streamName(streamName)
    })
}
