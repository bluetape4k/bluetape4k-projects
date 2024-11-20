package io.bluetape4k.aws.s3

import io.bluetape4k.aws.s3.model.getObjectRequest
import io.bluetape4k.aws.s3.model.putObjectRequest
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CopyObjectResult
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration
import software.amazon.awssdk.services.s3.model.CreateBucketResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * [bucketName]의 Bucket 이 존재하는지 알아봅니다.
 *
 * @param bucketName 존재를 파악할 Bucket name
 * @return 존재 여부
 */
fun S3AsyncClient.existsBucket(bucketName: String): CompletableFuture<Boolean> {
    bucketName.requireNotBlank("bucketName")

    return headBucket { it.bucket(bucketName) }
        .handle { _, error -> error == null }
}

/**
 * [bucketName]의 Bucket을 생성합니다.
 *
 * @param bucketName  생성할 Bucket name
 * @param createBucketConfiguration 생성할 Bucket을 위한 Configuration
 * @return Bucket 생성 결과. [CreateBucketResponse]
 */
fun S3AsyncClient.createBucket(
    bucketName: String,
    createBucketConfiguration: (CreateBucketConfiguration.Builder) -> Unit = {},
): CompletableFuture<CreateBucketResponse> {
    bucketName.requireNotBlank("bucketName")

    return createBucket {
        it.bucket(bucketName).createBucketConfiguration(createBucketConfiguration)
    }
}

//
// Get Object
//

/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param getObjectRequest 요청 정보 Builder
 * @return 다운받은 S3 Object의 ByteArray 형태의 정보
 */
fun S3AsyncClient.getAsByteArray(
    bucket: String,
    key: String,
    getObjectRequest: (GetObjectRequest.Builder) -> Unit = {},
): CompletableFuture<ByteArray> {
    val request = getObjectRequest(bucket, key, getObjectRequest)

    return getObject(request, AsyncResponseTransformer.toBytes())
        .thenApply { it.asByteArray() }
}

/**
 * S3 Object 를 download 한 후, 문자열로 반환합니다.
 *
 * @param getObjectRequest 요청 정보 Builder
 * @return 다운받은 S3 Object의 문자열 형태의 정보
 */
fun S3AsyncClient.getAsString(
    bucket: String,
    key: String,
    getObjectRequest: (GetObjectRequest.Builder) -> Unit = {},
): CompletableFuture<String> {
    val request = getObjectRequest(bucket, key, getObjectRequest)

    return getObject(request, AsyncResponseTransformer.toBytes())
        .thenApply { it.asString(Charsets.UTF_8) }
}


fun S3AsyncClient.getAsFile(
    bucket: String,
    key: String,
    destinationPath: Path,
    getObjectRequest: (GetObjectRequest.Builder) -> Unit = {},
): CompletableFuture<GetObjectResponse> {
    val request = getObjectRequest(bucket, key, getObjectRequest)
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
 * @param requestBuilder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
fun S3AsyncClient.put(
    bucket: String,
    key: String,
    body: AsyncRequestBody,
    requestBuilder: (PutObjectRequest.Builder) -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    val request = putObjectRequest(bucket, key, requestBuilder)
    return putObject(request, body)
}

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bytes             Upload 할 Byte Array
 * @param requestBuilder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
fun S3AsyncClient.putAsByteArray(
    bucket: String,
    key: String,
    bytes: ByteArray,
    requestBuilder: (PutObjectRequest.Builder) -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    return put(bucket, key, AsyncRequestBody.fromBytes(bytes), requestBuilder)
}

/**
 * S3 서버로 [contents]를 Upload 합니다.
 *
 * @param contents          Upload 할 문자열
 * @param requestBuilder  PutObjectRequest builder
 * @return S3에 저장된 결과
 */
fun S3AsyncClient.putAsString(
    bucket: String,
    key: String,
    contents: String,
    requestBuilder: (PutObjectRequest.Builder) -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    return put(bucket, key, AsyncRequestBody.fromString(contents), requestBuilder)
}

fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    file: File,
    requestBuilder: (PutObjectRequest.Builder) -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    return put(bucket, key, AsyncRequestBody.fromFile(file), requestBuilder)
}


fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    path: Path,
    requestBuilder: (PutObjectRequest.Builder) -> Unit = {},
): CompletableFuture<PutObjectResponse> {
    return put(bucket, key, AsyncRequestBody.fromFile(path), requestBuilder)
}

//
// Move Object
//

/**
 * [S3Object]를 Move 합니다.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey        원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey        대상 object key
 * @return
 */
fun S3AsyncClient.moveObject(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): CompletableFuture<CopyObjectResult> {
    return copyObject {
        it.sourceBucket(srcBucketName)
            .sourceKey(srcKey)
            .destinationBucket(destBucketName)
            .destinationKey(destKey)
    }
        .thenApply {
            if (it.copyObjectResult().eTag().isNotBlank()) {
                deleteObject { builder ->
                    builder.bucket(srcBucketName).key(srcKey)
                }
            }
            it
        }
        .thenApply {
            it.copyObjectResult()
        }
}

/**
 * [S3Object]를 Move 합니다.
 *
 * @param copyObjectRequest   복사 Request
 * @param deleteObjectRequest 원복 복제품 삭제 request
 * @return 복사 결과
 */
fun S3AsyncClient.moveObject(
    copyObjectRequest: (CopyObjectRequest.Builder) -> Unit,
    deleteObjectRequest: (DeleteObjectRequest.Builder) -> Unit,
): CompletableFuture<CopyObjectResult> {
    return copyObject(copyObjectRequest)
        .thenCompose { copyResponse ->
            if (copyResponse.copyObjectResult().eTag().isNotBlank()) {
                deleteObject(deleteObjectRequest).thenApply { copyResponse }
            } else {
                CompletableFuture.completedFuture(copyResponse)
            }
        }.thenApply {
            it.copyObjectResult()
        }
}
