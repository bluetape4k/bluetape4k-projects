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
import java.nio.charset.Charset
import java.nio.file.Path

/**
 * [bucketName]의 Bucket 이 존재하는지 알아봅니다.
 *
 * @param bucketName 존재를 파악할 Bucket name
 * @return 존재 여부
 */
suspend inline fun S3AsyncClient.existsBucket(bucketName: String): Boolean =
    existsBucketAsync(bucketName).await()

/**
 * [bucketName]의 Bucket을 생성합니다.
 *
 * @param bucketName 생성할 Bucket name
 * @param builder 요청 설정을 위한 빌더
 * @return Bucket 생성 결과
 */
suspend fun S3AsyncClient.createBucket(
    bucketName: String,
    @BuilderInference builder: CreateBucketConfiguration.Builder.() -> Unit = {},
): CreateBucketResponse =
    createBucketAsync(bucketName, builder).await()

/**
 * S3 Object 를 download 한 후, ByteArray 로 반환합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 ByteArray 형태의 정보
 */
suspend inline fun S3AsyncClient.getAsByteArray(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): ByteArray =
    getAsByteArrayAsync(bucket, key, builder).await()

/**
 * S3 Object 를 download 한 후, 문자열로 반환합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 문자열 형태의 정보
 */
suspend inline fun S3AsyncClient.getAsString(
    bucket: String,
    key: String,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): String =
    getAsStringAsync(bucket, key, builder).await()

/**
 * S3 Object 를 download 한 후, [destinationPath]에 저장합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param destinationPath 저장할 경로
 * @param builder 요청 설정을 위한 빌더
 * @return 다욱받은 S3 Object의 정보
 */
suspend inline fun S3AsyncClient.getAsFile(
    bucket: String,
    key: String,
    destinationPath: Path,
    @BuilderInference builder: GetObjectRequest.Builder.() -> Unit = {},
): GetObjectResponse =
    getAsFileAsync(bucket, key, destinationPath, builder).await()

/**
 * S3 서버로 [body]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param body Upload 할 [AsyncRequestBody]
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.put(
    bucket: String,
    key: String,
    body: AsyncRequestBody,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    putAsync(bucket, key, body, builder).await()

/**
 * S3 서버로 [bytes]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param bytes Upload 할 Byte Array
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsByteArray(
    bucket: String,
    key: String,
    bytes: ByteArray,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    putAsByteArrayAsync(bucket, key, bytes, builder).await()

/**
 * S3 서버로 [contents]를 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param contents Upload 할 문자열
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsString(
    bucket: String,
    key: String,
    contents: String,
    charset: Charset = Charsets.UTF_8,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    putAsStringAsync(bucket, key, contents, charset, builder).await()

/**
 * S3 서버로 [file]을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param file Upload 할 파일
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    file: File,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    putAsFileAsync(bucket, key, file, builder).await()

/**
 * S3 서버로 [path]의 파일을 Upload 합니다.
 *
 * @param bucket Upload 할 Bucket name
 * @param key Upload 할 Object key
 * @param path Upload 할 파일 경로
 * @param builder 요청 설정을 위한 빌더
 * @return S3에 저장된 결과
 */
suspend inline fun S3AsyncClient.putAsFile(
    bucket: String,
    key: String,
    path: Path,
    @BuilderInference builder: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse =
    putAsFileAsync(bucket, key, path, builder).await()

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
suspend fun S3AsyncClient.moveObject(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult =
    moveObjectAsync(srcBucketName, srcKey, destBucketName, destKey).await()

/**
 * [software.amazon.awssdk.services.s3.model.S3Object]를 Move 합니다.
 *
 * 참고: 이 연산은 원자적이지 않습니다. 복사는 성공했지만 삭제가 실패할 수 있습니다.
 *
 * @param copyObjectRequest 복사 Request
 * @param deleteObjectRequest 원본 삭제 request
 * @return 이동 작업 결과
 */
suspend fun S3AsyncClient.moveObject(
    @BuilderInference copyObjectRequest: CopyObjectRequest.Builder.() -> Unit,
    @BuilderInference deleteObjectRequest: DeleteObjectRequest.Builder.() -> Unit,
): MoveObjectResult =
    moveObjectAsync(copyObjectRequest, deleteObjectRequest).await()

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
suspend fun S3AsyncClient.moveObjectAtomic(
    srcBucketName: String,
    srcKey: String,
    destBucketName: String,
    destKey: String,
): MoveObjectResult =
    moveObjectAtomicAsync(srcBucketName, srcKey, destBucketName, destKey).await()
