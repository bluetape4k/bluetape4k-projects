package io.bluetape4k.aws.s3

import io.bluetape4k.aws.s3.model.MoveObjectResult
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration
import software.amazon.awssdk.services.s3.model.CreateBucketResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.File
import java.nio.file.Path

/**
 * [bucketName]의 Bucket 이 존재하는지 알아봅니다.
 *
 * @param bucketName 존재를 파악할 Bucket name
 * @return 존재 여부
 */
suspend inline fun S3AsyncClient.existsBucketSuspending(bucketName: String): Boolean = existsBucket(bucketName).await()

/**
 * [bucketName]의 Bucket을 생성합니다.
 *
 * @param bucketName  생성할 Bucket name
 * @param builder 생성할 Bucket을 위한 Configuration
 * @return Bucket 생성 결과. [CreateBucketResponse]
 */
suspend fun S3AsyncClient.createBucketSuspending(
    bucketName: String,
    @BuilderInference builder: CreateBucketConfiguration.Builder.() -> Unit = {},
): CreateBucketResponse = createBucket(bucketName, builder).await()

/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param bucket bucket name
 * @param key object key
 * @param builder 요청 정보 Builder
 * @return 다욱받은 S3 Object의 ByteArray 형태의 정보
 */
suspend inline fun S3AsyncClient.getAsByteArraySuspending(
    bucket: String,
    key: String,
    builder: GetObjectRequest.Builder.() -> Unit = {},
): ByteArray = getAsByteArray(bucket, key, builder).await()

/**
 * S3 Object 를 download 한 후, 문자열로 반환합니다.
 *
 * @param bucket bucket name
 * @param key object key
 * @param builder 요청 정보 Builder
 * @return 다욱받은 S3 Object의 문자열 형태의 정보
 */
suspend inline fun S3AsyncClient.getAsStringSuspending(
    bucket: String,
    key: String,
    builder: GetObjectRequest.Builder.() -> Unit = {},
): String = getAsString(bucket, key, builder).await()

/**
 * S3 Object 를 download 한 후, [destinationPath]에 저장합니다.
 *
 * @param bucket bucket name
 * @param key object key
 * @param destinationPath 저장할 경로
 * @param builder 요청 정보 Builder
 * @return 다욱받은 S3 Object의 정보
 */
suspend inline fun S3AsyncClient.getAsFileSuspending(
    bucket: String,
    key: String,
    destinationPath: Path,
    builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectResponse = getAsFile(bucket, key, destinationPath, builder).await()

/**
 * S3 서버로 [body]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param body Upload 할 [AsyncRequestBody]
 * @param builder PutObjectRequest builder
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putSuspending(
    bucket: String,
    key: String,
    body: AsyncRequestBody,
    builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse = put(bucket, key, body, builder).await()

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param bytes Upload 할 Byte Array
 * @param builder PutObjectRequest builder
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsByteArraySuspending(
    bucket: String,
    key: String,
    bytes: ByteArray,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse = putAsByteArray(bucket, key, bytes, builder).await()

/**
 * S3 서버로 [contents]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param contents Upload 할 문자열
 * @param builder PutObjectRequest builder
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsStringSuspending(
    bucket: String,
    key: String,
    contents: String,
    builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse = putAsString(bucket, key, contents, builder).await()

/**
 * S3 서버로 [file]을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param file Upload 할 파일
 * @param builder PutObjectRequest builder
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsFileSuspending(
    bucket: String,
    key: String,
    file: File,
    builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse = putAsFile(bucket, key, file, builder).await()

/**
 * S3 서버로 [path]의 파일을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param path Upload 할 파일 경로
 * @param builder PutObjectRequest builder
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsFileSuspending(
    bucket: String,
    key: String,
    path: Path,
    builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse = putAsFile(bucket, key, path, builder).await()

/**
 * [software.amazon.awssdk.services.s3.model.S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey 원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey 대상 object key
 * @return 이동 작업 결과
 */
suspend fun S3AsyncClient.moveObjectSuspending(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult = moveObject(srcBucketName, srcKey, destBucketName, destKey).await()

/**
 * [software.amazon.awssdk.services.s3.model.S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 *
 * @param copyObjectRequest 복사 Request
 * @param deleteObjectRequest 원본 삭제 request
 * @return 이동 작업 결과
 */
suspend fun S3AsyncClient.moveObjectSuspending(
    copyObjectRequest: CopyObjectRequest.Builder.() -> Unit,
    deleteObjectRequest: DeleteObjectRequest.Builder.() -> Unit,
): MoveObjectResult = moveObject(copyObjectRequest, deleteObjectRequest).await()

/**
 * [software.amazon.awssdk.services.s3.model.S3Object]를 원자적으로 Move 합니다.
 *
 * 복사가 성공했지만 삭제가 실패한 경우, 복사된 객체를 삭제하고 예외를 발생시킵니다.
 *
 * @param srcBucketName 원본 bucket name
 * @param srcKey 원본 object key
 * @param destBucketName 대상 bucket name
 * @param destKey 대상 object key
 * @return 이동 작업 결과
 * @throws IllegalStateException 삭제 실패 시 복구도 실패한 경우
 */
suspend fun S3AsyncClient.moveObjectAtomicSuspending(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult = moveObjectAtomic(srcBucketName, srcKey, destBucketName, destKey).await()
