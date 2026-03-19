package io.bluetape4k.aws.kinesis

import kotlinx.coroutines.future.await
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

/**
 * Kinesis 스트림을 코루틴으로 생성합니다.
 *
 * 내부적으로 [createStreamAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.createStream("my-stream", shardCount = 1)
 * ```
 */
suspend fun KinesisAsyncClient.createStream(
    streamName: String,
    shardCount: Int = 1,
): CreateStreamResponse =
    createStreamAsync(streamName, shardCount).await()

/**
 * Kinesis 스트림에 단일 레코드를 코루틴으로 전송합니다.
 *
 * 내부적으로 [putRecordAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.putRecord(
 *     streamName = "my-stream",
 *     partitionKey = "pk",
 *     data = SdkBytes.fromUtf8String("hello")
 * )
 * ```
 */
suspend fun KinesisAsyncClient.putRecord(
    streamName: String,
    partitionKey: String,
    data: SdkBytes,
): PutRecordResponse =
    putRecordAsync(streamName, partitionKey, data).await()

/**
 * Kinesis 스트림에 복수의 레코드를 코루틴으로 배치 전송합니다.
 *
 * 내부적으로 [putRecordsAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.putRecords("my-stream", entries)
 * ```
 */
suspend fun KinesisAsyncClient.putRecords(
    streamName: String,
    entries: List<PutRecordsRequestEntry>,
): PutRecordsResponse =
    putRecordsAsync(streamName, entries).await()

/**
 * Kinesis 스트림의 샤드 이터레이터를 코루틴으로 조회합니다.
 *
 * 내부적으로 [getShardIteratorAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.getShardIterator(
 *     streamName = "my-stream",
 *     shardId = "shardId-000000000000",
 *     type = ShardIteratorType.TRIM_HORIZON
 * )
 * ```
 */
suspend fun KinesisAsyncClient.getShardIterator(
    streamName: String,
    shardId: String,
    type: ShardIteratorType = ShardIteratorType.TRIM_HORIZON,
): GetShardIteratorResponse =
    getShardIteratorAsync(streamName, shardId, type).await()

/**
 * Kinesis 샤드 이터레이터로부터 레코드를 코루틴으로 조회합니다.
 *
 * 내부적으로 [getRecordsAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.getRecords(shardIterator, limit = 100)
 * ```
 */
suspend fun KinesisAsyncClient.getRecords(
    shardIterator: String,
    limit: Int = 100,
): GetRecordsResponse =
    getRecordsAsync(shardIterator, limit).await()

/**
 * Kinesis 스트림 상세 정보를 코루틴으로 조회합니다.
 *
 * 내부적으로 [describeStreamAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.describeStream("my-stream")
 * ```
 */
suspend fun KinesisAsyncClient.describeStream(
    streamName: String,
): DescribeStreamResponse =
    describeStreamAsync(streamName).await()

/**
 * Kinesis 스트림을 코루틴으로 삭제합니다.
 *
 * 내부적으로 [deleteStreamAsync]를 호출한 뒤 `await()`로 완료를 기다립니다.
 *
 * ```kotlin
 * val response = kinesisAsyncClient.deleteStream("my-stream")
 * ```
 */
suspend fun KinesisAsyncClient.deleteStream(
    streamName: String,
): DeleteStreamResponse =
    deleteStreamAsync(streamName).await()
