package io.bluetape4k.aws.s3

import io.bluetape4k.aws.s3.model.MoveObjectResult
import io.bluetape4k.aws.s3.model.getObjectRequest
import io.bluetape4k.aws.s3.model.putObjectRequest
import io.bluetape4k.aws.s3.model.toRequestBody
import io.bluetape4k.io.exists
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration
import software.amazon.awssdk.services.s3.model.CreateBucketResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path

private val log = KotlinLogging.logger {}

/**
 * [bucketName]의 Bucket 이 존재하는지 알아봅니다.
 *
 * @param bucketName 존재를 파악할 Bucket name
 * @return 존재 여부를 담은 [Result]
 */
fun S3Client.existsBucket(bucketName: String): Result<Boolean> {
    bucketName.requireNotBlank("bucketName")

    return runCatching {
        headBucket { it.bucket(bucketName) }
        true
    }.recover { error ->
        when (error) {
            is NoSuchBucketException -> false
            else                     -> throw error
        }
    }
}

/**
 * [bucketName]의 Bucket을 생성합니다.
 *
 * @param bucketName  생성할 Bucket name
 * @param builder 생성할 Bucket을 위한 Configuration을 설정하는 코드
 * @return Bucket 생성 결과. [CreateBucketResponse]
 */
fun S3Client.createBucket(
    bucketName: String,
    @BuilderInference builder: CreateBucketConfiguration.Builder.() -> Unit = {},
): CreateBucketResponse {
    bucketName.requireNotBlank("bucketName")

    return createBucket {
        it.bucket(bucketName).createBucketConfiguration(builder)
    }
}

//
// Get Object
//

/**
 * S3 Object 를 다운로드 받아 [ResponseTransformer]를 통해 변환된 결과를 반환합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder 요청 설정을 위한 빌더
 * @param responseTransformer 응답 변환기
 * @return 변환된 결과
 */
inline fun <T> S3Client.getObjectAs(
    bucket: String,
    key: String,
    builder: GetObjectRequest.Builder.() -> Unit = {},
    responseTransformer: ResponseTransformer<GetObjectResponse, T>,
): T {
    val request = getObjectRequest(bucket, key, builder)
    return getObject(request, responseTransformer)
}

/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 ByteArray 형태의 정보
 */
inline fun S3Client.getAsByteArray(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): ByteArray {
    val request = getObjectRequest(bucket, key, builder)
    return getObject(request, ResponseTransformer.toBytes()).asByteArray()
}

/**
 * S3 Object 를 download 한 후, 문자열로 반환합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param charset 문자 인코딩 (기본값: UTF-8)
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 문자열 형태의 정보
 */
inline fun S3Client.getAsString(
    bucket: String,
    key: String,
    charset: Charset = Charsets.UTF_8,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): String =
    getAsByteArray(bucket, key, builder).toString(charset)

/**
 * S3 Object 를 download 한 후, [file]로 저장합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param file 저장할 파일
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 정보
 */
inline fun S3Client.getAsFile(
    bucket: String,
    key: String,
    file: File,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectResponse {
    val request = getObjectRequest(bucket, key, builder)
    return getObject(request, ResponseTransformer.toFile(file))
}

/**
 * S3 Object 를 download 한 후, [path]에 저장합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param path 저장할 경로
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 정보
 */
inline fun S3Client.getAsFile(
    bucket: String,
    key: String,
    path: Path,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectResponse {
    val request = getObjectRequest(bucket, key, builder)
    return getObject(request, ResponseTransformer.toFile(path))
}

//
// Put Object
//

/**
 * S3 서버로 [body]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param body Upload 할 [RequestBody]
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
inline fun S3Client.put(
    bucket: String,
    key: String,
    body: RequestBody,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    val request = putObjectRequest(bucket, key, builder)
    return putObject(request, body)
}

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param bytes Upload 할 Byte Array
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
inline fun S3Client.putAsByteArray(
    bucket: String,
    key: String,
    bytes: ByteArray,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    put(bucket, key, bytes.toRequestBody(), builder)

/**
 * S3 서버로 [contents]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param contents Upload 할 문자열
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
inline fun S3Client.putAsString(
    bucket: String,
    key: String,
    contents: String,
    charset: Charset = Charsets.UTF_8,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    put(bucket, key, contents.toRequestBody(charset), builder)

/**
 * S3 서버로 [file]을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param file Upload 할 파일
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 * @throws IllegalArgumentException 파일이 존재하지 않을 경우
 */
inline fun S3Client.putAsFile(
    bucket: String,
    key: String,
    file: File,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    require(file.exists()) { "File does not exist. file=$file" }

    return put(bucket, key, file.toRequestBody(), builder)
}

/**
 * S3 서버로 [path]의 파일을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param path Upload 할 파일 경로
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 * @throws IllegalArgumentException 파일이 존재하지 않을 경우
 */
inline fun S3Client.putAsFile(
    bucket: String,
    key: String,
    path: Path,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    require(path.exists()) { "file does not exist. path=$path" }

    return put(bucket, key, path.toRequestBody(), builder)
}

//
// Move Object
//

/**
 * [S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 * 원자성이 필요한 경우 [moveObjectAtomicAsync]을 사용하세요.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey        원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey        대상 object key
 * @return 이동 작업 결과
 */
fun S3Client.moveObject(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult {
    srcBucketName.requireNotBlank("srcBucketName")
    srcKey.requireNotBlank("srcKey")
    destBucketName.requireNotBlank("destBucketName")
    destKey.requireNotBlank("destKey")

    val copyResponse =
        copyObject { builder ->
            builder
                .sourceBucket(srcBucketName)
                .sourceKey(srcKey)
                .destinationBucket(destBucketName)
                .destinationKey(destKey)
        }

    val deleteResponse =
        if (copyResponse.copyObjectResult().eTag()?.isNotBlank() == true) {
            runCatching {
                deleteObject { it.bucket(srcBucketName).key(srcKey) }
            }.onFailure { error ->
                log.warn(
                    "Failed to delete source object after copy. Source: {}/{}, Dest: {}/{}",
                    srcBucketName,
                    srcKey,
                    destBucketName,
                    destKey,
                    error,
                )
            }.getOrNull()
        } else {
            null
        }

    return MoveObjectResult(copyResponse.copyObjectResult(), deleteResponse)
}

/**
 * [S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 *
 * @param copyRequest   복사 Request
 * @param deleteRequest 원본 복제품 삭제 request
 * @return 이동 작업 결과
 */
fun S3Client.moveObject(
    copyRequest: CopyObjectRequest.Builder.() -> Unit,
    deleteRequest: DeleteObjectRequest.Builder.() -> Unit,
): MoveObjectResult {
    val copyResponse = copyObject(copyRequest)

    val deleteResponse =
        if (copyResponse.copyObjectResult().eTag()?.isNotBlank() == true) {
            runCatching {
                deleteObject(deleteRequest)
            }.onFailure { error ->
                log.warn(error) { "Failed to delete source object after copy." }
            }.getOrNull()
        } else {
            null
        }

    return MoveObjectResult(copyResponse.copyObjectResult(), deleteResponse)
}

/**
 * [S3Object]를 원자적으로 Move 합니다.
 *
 * 복사가 성공했지만 삭제가 실패한 경우, 복사된 객체를 삭제하고 예외를 발생시킵니다.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey        원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey        대상 object key
 * @return 이동 작업 결과
 * @throws IllegalStateException 삭제 실패 시 복구도 실패한 경우
 */
fun S3Client.moveObjectAtomic(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult {
    val result = moveObject(srcBucketName, srcKey, destBucketName, destKey)

    if (result.isPartialSuccess) {
        // 복사는 성공했지만 삭제가 실패한 경우, 롤백 시도
        log.warn { "Move partially succeeded. Attempting rollback by deleting copied object. Dest: $destBucketName/$destKey" }

        runCatching {
            deleteObject { it.bucket(destBucketName).key(destKey) }
        }.onFailure { error ->
            log.error(error) { "Rollback failed! Copied object may remain at destination. Dest: $destBucketName/$destKey" }
            throw IllegalStateException(
                "Move failed and rollback also failed. Copied object remains at $destBucketName/$destKey",
                error
            )
        }

        error("Move failed: copy succeeded but delete failed. Rollback completed.")
    }

    return result
}
