package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.getBucketPolicy
import aws.sdk.kotlin.services.s3.model.GetBucketPolicyRequest
import aws.sdk.kotlin.services.s3.model.GetObjectAclRequest
import aws.sdk.kotlin.services.s3.model.GetObjectAclResponse
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.sdk.kotlin.services.s3.model.GetObjectRetentionRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRetentionResponse
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.content.writeToFile
import aws.smithy.kotlin.runtime.content.writeToOutputStream
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import io.bluetape4k.aws.kotlin.s3.model.getObjectAclRequestOf
import io.bluetape4k.aws.kotlin.s3.model.getObjectRequestOf
import io.bluetape4k.aws.kotlin.s3.model.getObjectRetentionRequestOf
import io.bluetape4k.aws.kotlin.s3.model.headObjectRequestOf
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.coroutines.flow.extensions.flowFromSuspend
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapMerge
import java.io.OutputStream
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [bucket]의 [key]에 해당하는 객체가 존재하는지 확인합니다.
 *
 * ```
 * val exists = s3Client.existsObject("bucket-name", "key")
 * ```
 */
suspend fun S3Client.existsObject(
    bucket: String,
    key: String,
    @BuilderInference builder: HeadObjectRequest.Builder.() -> Unit = {},
): Boolean {
    val request = headObjectRequestOf(bucket, key, builder = builder)
    return runCatching { headObject(request); true }.isSuccess
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져옵니다.
 *
 * ```
 * val response = s3Client.get("bucket-name", "key")
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return [GetObjectResponse] 인스턴스
 */
suspend fun S3Client.get(
    bucketName: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectResponse {
    val request = getObjectRequestOf(bucketName, key, builder = builder)
    return getObject(request) { it }
}


/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 [builder]을 통해 원하는 수형으로 변환합니다.
 *
 * ```
 * val objectText = s3Client.getAs("bucket-name", "key") { it.readText() }
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param configurer [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @param builder [GetObjectResponse] 를 통해 원하는 수형으로 변환하는 람다
 * @return [builder] 을 통해 변환된 수형
 */
suspend fun <T> S3Client.getAs(
    bucketName: String,
    key: String,
    @BuilderInference configurer: GetObjectRequest.Builder.() -> Unit = {},
    @BuilderInference builder: suspend (GetObjectResponse) -> T,
): T {
    val request = getObjectRequestOf(bucketName, key, builder = configurer)
    return getObject(request, builder)
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 바이트 배열로 변환합니다.
 *
 * ```
 * val bytes = s3Client.getAsByteArray("bucket-name", "key")
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return 바이트 배열
 */
suspend fun S3Client.getAsByteArray(
    bucketName: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): ByteArray? {
    return getAs(bucketName, key, builder) {
        it.body?.toByteArray()
    }
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 문자열로 변환합니다.
 *
 * ```
 * val text = s3Client.getAsString("bucket-name", "key")
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return 문자열
 */
suspend fun S3Client.getAsString(
    bucketName: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): String? {
    return getAs(bucketName, key, builder) {
        it.body?.decodeToString()
    }
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 파일로 저장합니다.
 *
 * ```
 * val file = File("test.txt")
 * s3Client.getAsFile("bucket-name", "key", file)
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param file 저장할 파일
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return 저장된 바이트 수
 */
suspend fun S3Client.getAsFile(
    bucketName: String,
    key: String,
    file: java.io.File,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): Long {
    return getAs(bucketName, key, builder) {
        it.body?.writeToFile(file) ?: -1L
    }
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 파일로 저장합니다.
 *
 * ```
 * val filePath = Paths.get("test.txt")
 * s3Client.getAsFile("bucket-name", "key", filePath)
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param filePath 저장할 파일 경로
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return 저장된 바이트 수
 */
suspend fun S3Client.getAsFile(
    bucketName: String,
    key: String,
    filePath: Path,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): Long {
    return getAs(bucketName, key, builder) {
        it.body?.writeToFile(filePath) ?: -1L
    }
}

/**
 * [bucketName]의 [key]에 해당하는 객체를 가져와 [outputStream]에 쓰기합니다.
 * [builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 *
 * ```
 * val outputStream = ByteArrayOutputStream()
 * s3Client.getAsOutputStream("bucket-name", "key", outputStream)
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param outputStream 출력 스트림
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 * @return [outputStream]에 쓰인 바이트 수
 * @see [writeToOutputStream]
 */
suspend fun S3Client.getAsOutputStream(
    bucketName: String,
    key: String,
    outputStream: OutputStream,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
) {
    getAs(bucketName, key, builder) {
        it.body?.writeToOutputStream(outputStream)
    }
}

/**
 * S3의 여러 객체를 동시에 다운로드 합니다.
 *
 * ```
 * val responses = s3Client.getAll(request1, request2, request3).toList()
 * ```
 *
 * @param concurrency 동시에 처리할 요청 수
 * @param getObjectRequests 다운로드할 객체 요청들
 * @return [GetObjectResponse] 의 [Flow]
 */
fun S3Client.getAll(
    concurrency: Int = DEFAULT_CONCURRENCY,
    vararg getObjectRequests: GetObjectRequest,
): Flow<GetObjectResponse> = callbackFlow {
    getObjectRequests
        .asFlow()
        .async { request ->
            val response = getObject(request) { it }
            send(response)
        }
}

/**
 * S3 객체에 대한 Presigned URL을 생성합니다.
 *
 * ```
 * val url = s3Client.presignGetObject("bucket-name", "key")
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param duration Presigned URL의 유효 시간
 * @param builder [GetObjectRequest.Builder] 를 통해 [GetObjectRequest] 를 설정합니다.
 */
suspend fun S3Client.presignGetObject(
    bucketName: String,
    key: String,
    duration: Duration = 5.seconds,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): HttpRequest {
    val request = getObjectRequestOf(bucketName, key, builder = builder)
    return presignGetObject(request, duration)
}

/**
 * [bucketName]의 [key]에 대한 객체 ACL을 조회합니다.
 *
 * ```
 * val response = s3Client.getObjectAcl("bucket-name", "key")
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @return [GetObjectAclResponse] 인스턴스
 */
suspend fun S3Client.getObjectAcl(
    bucketName: String,
    key: String,
    versionId: String? = null,
    @BuilderInference builder: GetObjectAclRequest.Builder.() -> Unit = {},
): GetObjectAclResponse {
    bucketName.requireNotBlank("bucketName")
    key.requireNotBlank("key")

    return getObjectAcl(getObjectAclRequestOf(bucketName, key, versionId, builder))
}

/**
 * [bucketName]의 여러 Key에 대한 객체 ACL을 병렬로 조회합니다.
 *
 * ```
 * val responses = s3Client.getObjectsAcl("bucket-name", "key1", "key2", "key3").toList()
 * ```
 *
 * @param bucketName 버킷 이름
 * @param keys 객체 키 목록
 * @return [GetObjectAclResponse] 의 Flow
 */
fun S3Client.getObjectsAcl(bucketName: String, vararg keys: String): Flow<GetObjectAclResponse> {
    bucketName.requireNotBlank("bucketName")
    keys.requireNotEmpty("keys")

    return keys.asFlow()
        .flatMapMerge(DEFAULT_CONCURRENCY) { key ->
            flowFromSuspend {
                getObjectAcl(bucketName, key)
            }
//            channelFlow {
//                val response = getObjectAcl(bucketName, key)
//                send(response)
//            }
        }
}

/**
 * [bucketName]의 [key]에 대한 객체 Retention을 조회합니다.
 *
 * ```
 * val response = s3Client.getObjectRetention("bucket-name", "key")
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param builder [GetObjectRetentionRequest.Builder] 를 통해 [GetObjectRetentionRequest] 를 설정합니다.
 * @return [GetObjectRetentionResponse] 인스턴스
 */
suspend fun S3Client.getObjectRetention(
    bucketName: String,
    key: String,
    versionId: String? = null,
    @BuilderInference builder: GetObjectRetentionRequest.Builder.() -> Unit = {},
): GetObjectRetentionResponse {
    val request = getObjectRetentionRequestOf(bucketName, key, versionId, builder)
    return getObjectRetention(request)
}

/**
 * [bucketName] 버킷의 정책을 조회합니다. Policy가 없는 경우 `null`을 반환합니다.
 *
 * ```
 * val policy = s3Client.tryGetBucketPolicy("bucket-name")
 * ```
 * @param bucketName 버킷 이름
 * @param expectedBucketOwner 버킷 소유자
 * @return 버킷 정책 문자열, Policy가 없는 경우 `null` 반환
 */
suspend fun S3Client.tryGetBucketPolicy(
    bucketName: String,
    expectedBucketOwner: String? = null,
    @BuilderInference builder: GetBucketPolicyRequest.Builder.() -> Unit = {},
): String? {
    return runCatching {
        getBucketPolicy {
            this.bucket = bucketName
            this.expectedBucketOwner = expectedBucketOwner
            builder()
        }
            .policy
    }.getOrNull()
}
