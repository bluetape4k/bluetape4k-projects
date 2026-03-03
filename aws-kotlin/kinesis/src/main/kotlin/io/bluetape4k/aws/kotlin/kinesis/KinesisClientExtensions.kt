package io.bluetape4k.aws.kotlin.kinesis

import aws.sdk.kotlin.services.kinesis.KinesisClient
import aws.sdk.kotlin.services.kinesis.createStream
import aws.sdk.kotlin.services.kinesis.deleteStream
import aws.sdk.kotlin.services.kinesis.describeStream
import aws.sdk.kotlin.services.kinesis.getRecords
import aws.sdk.kotlin.services.kinesis.getShardIterator
import aws.sdk.kotlin.services.kinesis.model.CreateStreamRequest
import aws.sdk.kotlin.services.kinesis.model.CreateStreamResponse
import aws.sdk.kotlin.services.kinesis.model.DeleteStreamRequest
import aws.sdk.kotlin.services.kinesis.model.DeleteStreamResponse
import aws.sdk.kotlin.services.kinesis.model.DescribeStreamRequest
import aws.sdk.kotlin.services.kinesis.model.DescribeStreamResponse
import aws.sdk.kotlin.services.kinesis.model.GetRecordsRequest
import aws.sdk.kotlin.services.kinesis.model.GetRecordsResponse
import aws.sdk.kotlin.services.kinesis.model.GetShardIteratorRequest
import aws.sdk.kotlin.services.kinesis.model.GetShardIteratorResponse
import aws.sdk.kotlin.services.kinesis.model.PutRecordRequest
import aws.sdk.kotlin.services.kinesis.model.PutRecordResponse
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequest
import aws.sdk.kotlin.services.kinesis.model.PutRecordsRequestEntry
import aws.sdk.kotlin.services.kinesis.model.PutRecordsResponse
import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType
import aws.sdk.kotlin.services.kinesis.putRecord
import aws.sdk.kotlin.services.kinesis.putRecords
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue

/**
 * AWS Kotlin SDK Kinesis 클라이언트를 생성합니다.
 *
 * ```kotlin
 * val client = kinesisClientOf(
 *     endpointUrl = Url.parse("http://localhost:4566"),
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider
 * )
 * ```
 *
 * @param endpointUrl Kinesis 서비스 엔드포인트 URL. null이면 기본 AWS 엔드포인트를 사용합니다.
 * @param region AWS 리전. null이면 환경 설정에서 자동으로 감지합니다.
 * @param credentialsProvider AWS 인증 정보 제공자. null이면 기본 자격 증명 체인을 사용합니다.
 * @param httpClient HTTP 클라이언트 엔진. 기본값은 [HttpClientEngineProvider.defaultHttpEngine]입니다.
 * @param builder [KinesisClient.Config.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [KinesisClient] 인스턴스.
 */
inline fun kinesisClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: KinesisClient.Config.Builder.() -> Unit = {},
): KinesisClient =
    KinesisClient {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        this.httpClient = httpClient

        builder()
    }.apply {
        ShutdownQueue.register(this)
    }

/**
 * Kinesis 스트림을 생성합니다.
 *
 * ```kotlin
 * val response = kinesisClient.createStream("my-stream", shardCount = 1)
 * ```
 *
 * @param streamName 생성할 스트림 이름
 * @param shardCount 샤드 수 (기본값: 1)
 * @param builder [CreateStreamRequest]를 빌드하는 람다 함수
 * @return [CreateStreamResponse] 인스턴스
 */
suspend inline fun KinesisClient.createStream(
    streamName: String,
    shardCount: Int = 1,
    @BuilderInference crossinline builder: CreateStreamRequest.Builder.() -> Unit = {},
): CreateStreamResponse {
    streamName.requireNotBlank("streamName")
    return createStream {
        this.streamName = streamName
        this.shardCount = shardCount
        builder()
    }
}

/**
 * Kinesis 스트림에 단일 레코드를 전송합니다.
 *
 * ```kotlin
 * val response = kinesisClient.putRecord(
 *     streamName = "my-stream",
 *     partitionKey = "pk",
 *     data = "hello".toByteArray()
 * )
 * ```
 *
 * @param streamName 대상 스트림 이름
 * @param partitionKey 파티션 키
 * @param data 전송할 데이터 바이트 배열
 * @param builder [PutRecordRequest]를 빌드하는 람다 함수
 * @return [PutRecordResponse] 인스턴스
 */
suspend inline fun KinesisClient.putRecord(
    streamName: String,
    partitionKey: String,
    data: ByteArray,
    @BuilderInference crossinline builder: PutRecordRequest.Builder.() -> Unit = {},
): PutRecordResponse {
    streamName.requireNotBlank("streamName")
    partitionKey.requireNotBlank("partitionKey")
    return putRecord {
        this.streamName = streamName
        this.partitionKey = partitionKey
        this.data = data
        builder()
    }
}

/**
 * Kinesis 스트림에 복수의 레코드를 배치로 전송합니다.
 *
 * ```kotlin
 * val entries = listOf(
 *     PutRecordsRequestEntry { partitionKey = "pk1"; data = "msg1".toByteArray() }
 * )
 * val response = kinesisClient.putRecords("my-stream", entries)
 * ```
 *
 * @param streamName 대상 스트림 이름
 * @param entries 전송할 레코드 목록
 * @param builder [PutRecordsRequest]를 빌드하는 람다 함수
 * @return [PutRecordsResponse] 인스턴스
 */
suspend inline fun KinesisClient.putRecords(
    streamName: String,
    entries: List<PutRecordsRequestEntry>,
    @BuilderInference crossinline builder: PutRecordsRequest.Builder.() -> Unit = {},
): PutRecordsResponse {
    streamName.requireNotBlank("streamName")
    return putRecords {
        this.streamName = streamName
        this.records = entries
        builder()
    }
}

/**
 * Kinesis 스트림의 샤드 이터레이터를 조회합니다.
 *
 * ```kotlin
 * val response = kinesisClient.getShardIterator(
 *     streamName = "my-stream",
 *     shardId = "shardId-000000000000",
 *     type = ShardIteratorType.TrimHorizon
 * )
 * ```
 *
 * @param streamName 스트림 이름
 * @param shardId 샤드 ID
 * @param type 샤드 이터레이터 타입 (기본값: [ShardIteratorType.TrimHorizon])
 * @param builder [GetShardIteratorRequest]를 빌드하는 람다 함수
 * @return [GetShardIteratorResponse] 인스턴스
 */
suspend inline fun KinesisClient.getShardIterator(
    streamName: String,
    shardId: String,
    type: ShardIteratorType = ShardIteratorType.TrimHorizon,
    @BuilderInference crossinline builder: GetShardIteratorRequest.Builder.() -> Unit = {},
): GetShardIteratorResponse {
    streamName.requireNotBlank("streamName")
    shardId.requireNotBlank("shardId")
    return getShardIterator {
        this.streamName = streamName
        this.shardId = shardId
        this.shardIteratorType = type
        builder()
    }
}

/**
 * Kinesis 샤드 이터레이터로부터 레코드를 조회합니다.
 *
 * ```kotlin
 * val response = kinesisClient.getRecords(shardIterator, limit = 100)
 * ```
 *
 * @param shardIterator 샤드 이터레이터 문자열
 * @param limit 조회할 최대 레코드 수 (기본값: 100)
 * @param builder [GetRecordsRequest]를 빌드하는 람다 함수
 * @return [GetRecordsResponse] 인스턴스
 */
suspend inline fun KinesisClient.getRecords(
    shardIterator: String,
    limit: Int = 100,
    @BuilderInference crossinline builder: GetRecordsRequest.Builder.() -> Unit = {},
): GetRecordsResponse {
    shardIterator.requireNotBlank("shardIterator")
    return getRecords {
        this.shardIterator = shardIterator
        this.limit = limit
        builder()
    }
}

/**
 * Kinesis 스트림의 상세 정보를 조회합니다.
 *
 * ```kotlin
 * val response = kinesisClient.describeStream("my-stream")
 * ```
 *
 * @param streamName 스트림 이름
 * @param builder [DescribeStreamRequest]를 빌드하는 람다 함수
 * @return [DescribeStreamResponse] 인스턴스
 */
suspend inline fun KinesisClient.describeStream(
    streamName: String,
    @BuilderInference crossinline builder: DescribeStreamRequest.Builder.() -> Unit = {},
): DescribeStreamResponse {
    streamName.requireNotBlank("streamName")
    return describeStream {
        this.streamName = streamName
        builder()
    }
}

/**
 * Kinesis 스트림을 삭제합니다.
 *
 * ```kotlin
 * val response = kinesisClient.deleteStream("my-stream")
 * ```
 *
 * @param streamName 삭제할 스트림 이름
 * @param builder [DeleteStreamRequest]를 빌드하는 람다 함수
 * @return [DeleteStreamResponse] 인스턴스
 */
suspend inline fun KinesisClient.deleteStream(
    streamName: String,
    @BuilderInference crossinline builder: DeleteStreamRequest.Builder.() -> Unit = {},
): DeleteStreamResponse {
    streamName.requireNotBlank("streamName")
    return deleteStream {
        this.streamName = streamName
        builder()
    }
}
