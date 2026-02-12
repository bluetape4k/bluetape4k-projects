package io.bluetape4k.aws.s3

import io.bluetape4k.aws.s3.model.MoveObjectResult
import io.bluetape4k.aws.s3.model.getObjectRequest
import io.bluetape4k.aws.s3.model.putObjectRequest
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
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
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

private val log = KotlinLogging.logger { }

/**
 * [bucketName]의 Bucket 이 존재하는지 알아봅니다.
 *
 * @param bucketName 존재를 파악할 Bucket name
 * @return 존재 여부
 */
fun S3AsyncClient.existsBucket(bucketName: String): CompletableFuture<Boolean> {
    bucketName.requireNotBlank("bucketName")

    return headBucket { it.bucket(bucketName) }
        .handle { _, error ->
            when (error) {
                is NoSuchBucketException -> false
                null                     -> true
                else                     -> throw error
            }
        }
}

/**
 * [bucketName]의 Bucket을 생성합니다.
 *
 * @param bucketName  생성할 Bucket name
 * @param builder 생성할 Bucket을 위한 Configuration
 * @return Bucket 생성 결과. [CreateBucketResponse]
 */
fun S3AsyncClient.createBucket(
    bucketName: String,
    @BuilderInference builder: CreateBucketConfiguration.Builder.() -> Unit = {},
): CompletableFuture<CreateBucketResponse> {
    bucketName.requireNotBlank("bucketName")

    return createBucket {
        it.bucket(bucketName).createBucketConfiguration(builder)
    }
}

//
// Get Object
//

/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param builder 요청 정보 Builder
 * @return 다운받은 S3 Object의 ByteArray 형태의 정보
 */
inline fun S3AsyncClient.getAsByteArray(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<ByteArray> {
    val request = getObjectRequest(bucket, key, builder)

    return getObject(request, AsyncResponseTransformer.toBytes())
        .thenApply { it.asByteArray() }
}

/**
 * S3 Object 를 download 한 후, 문자열로 반환합니다.
 *
 * @param builder 요청 정보 Builder
 * @return 다운받은 S3 Object의 문자열 형태의 정보
 */
inline fun S3AsyncClient.getAsString(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<String> {
    val request = getObjectRequest(bucket, key, builder)

    return getObject(request, AsyncResponseTransformer.toBytes())
        .thenApply { it.asString(Charsets.UTF_8) }
}

inline fun S3AsyncClient.getAsFile(
    bucket: String,
    key: String,
    destinationPath: Path,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<GetObjectResponse> {
    val request = getObjectRequest(bucket, key, builder)
    return getObject(request, destinationPath)
}

//
// Put Object
//

/**
 * S3 서버로 [body]를 Upload 합니다.
 *
 * @param bucket            Upload 할 Bucket name
 * @param key               Upload 할 Object key
 * @param body              Upload 할 [AsyncRequestBody]
 * @param builder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
inline fun S3AsyncClient.put(
    bucket: String,
    key: String,
    body: AsyncRequestBody,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = putObjectRequest(bucket, key, builder)
    return putObject(request, body)
}

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bytes             Upload 할 Byte Array
 * @param builder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
inline fun S3AsyncClient.putAsByteArray(
    bucket: String,
    key: String,
    bytes: ByteArray,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<PutObjectResponse> =
    put(bucket, key, AsyncRequestBody.fromBytes(bytes), builder)

/**
 * S3 서버로 [contents]를 Upload 합니다.
 *
 * @param contents          Upload 할 문자열
 * @param builder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
inline fun S3AsyncClient.putAsString(
    bucket: String,
    key: String,
    contents: String,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<PutObjectResponse> =
    put(bucket, key, AsyncRequestBody.fromString(contents), builder)

inline fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    file: File,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<PutObjectResponse> =
    put(bucket, key, AsyncRequestBody.fromFile(file), builder)

inline fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    path: Path,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): CompletableFuture<PutObjectResponse> =
    put(bucket, key, AsyncRequestBody.fromFile(path), builder)

//
// Move Object
//

/**
 * [S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 * 원자성이 필요한 경우 [moveObjectAtomic]을 사용하세요.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey        원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey        대상 object key
 * @return 이동 작업 결과
 */
fun S3AsyncClient.moveObject(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): CompletableFuture<MoveObjectResult> {
    srcBucketName.requireNotBlank("srcBucketName")
    srcKey.requireNotBlank("srcKey")
    destBucketName.requireNotBlank("destBucketName")
    destKey.requireNotBlank("destKey")

    return copyObject { builder ->
        builder
            .sourceBucket(srcBucketName)
            .sourceKey(srcKey)
            .destinationBucket(destBucketName)
            .destinationKey(destKey)
    }.thenCompose { copyResponse ->
        if (copyResponse.copyObjectResult().eTag()?.isNotEmpty() == true) {
            deleteObject { builder ->
                builder.bucket(srcBucketName).key(srcKey)
            }.handle { deleteResponse, error ->
                if (error != null) {
                    log.warn(error) {
                        "Failed to delete source object after copy. " +
                                "Source: $srcBucketName/$srcKey, Dest: $destBucketName/$destKey"
                    }
                }
                MoveObjectResult(copyResponse.copyObjectResult(), deleteResponse)
            }
        } else {
            CompletableFuture.completedFuture(MoveObjectResult(copyResponse.copyObjectResult()))
        }
    }
}

/**
 * [S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 *
 * @param copyObjectRequest   복사 Request
 * @param deleteObjectRequest 원본 복제품 삭제 request
 * @return 이동 작업 결과
 */
fun S3AsyncClient.moveObject(
    copyObjectRequest: CopyObjectRequest.Builder.() -> Unit,
    deleteObjectRequest: DeleteObjectRequest.Builder.() -> Unit,
): CompletableFuture<MoveObjectResult> =
    copyObject(copyObjectRequest).thenCompose { copyResponse ->
        if (copyResponse.copyObjectResult().eTag()?.isNotBlank() == true) {
            deleteObject(deleteObjectRequest).handle { deleteResponse, error ->
                if (error != null) {
                    log.warn(error) { "Failed to delete source object after copy" }
                }
                MoveObjectResult(copyResponse.copyObjectResult(), deleteResponse)
            }
        } else {
            CompletableFuture.completedFuture(MoveObjectResult(copyResponse.copyObjectResult()))
        }
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
fun S3AsyncClient.moveObjectAtomic(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): CompletableFuture<MoveObjectResult> =
    moveObject(srcBucketName, srcKey, destBucketName, destKey).thenCompose { result ->
        if (result.isPartialSuccess) {
            // 복사는 성공했지만 삭제가 실패한 경우, 롤백 시도
            log.warn {
                "Move partially succeeded. Attempting rollback by deleting copied object. Dest: $destBucketName/$destKey"
            }
            deleteObject { it.bucket(destBucketName).key(destKey) }
                .handle { _, rollbackError ->
                    if (rollbackError != null) {
                        log.error(rollbackError) {
                            "Rollback failed! Copied object may remain at destination. Dest: $destBucketName/$destKey"
                        }
                        throw IllegalStateException(
                            "Move failed and rollback also failed. Copied object remains at $destBucketName/$destKey",
                            rollbackError,
                        )
                    }
                    throw IllegalStateException("Move failed: copy succeeded but delete failed. Rollback completed.")
                }
        } else {
            CompletableFuture.completedFuture(result)
        }
    }
