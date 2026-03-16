package io.bluetape4k.aws.s3.transfer

import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.CompletedDownload
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload
import software.amazon.awssdk.transfer.s3.model.CompletedUpload
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.nio.file.Path

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param T 반환 타입
 * @param responseTransformer 응답을 변환할 transformer
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val result = transferManager.download(AsyncResponseTransformer.toBytes())
 * // result.response().sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend inline fun <T: Any> S3TransferManager.download(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<T> = downloadAsync(responseTransformer, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param responseTransformer 응답을 변환할 비동기 transformer
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val result = transferManager.download("demo-bucket", "docs/readme.txt", AsyncResponseTransformer.toBytes())
 * // result.response().sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend fun <T: Any> S3TransferManager.download(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<T> = downloadAsync(bucket, key, responseTransformer, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 ByteArray 로 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val result = transferManager.downloadAsByteArray("demo-bucket", "docs/readme.txt")
 * // result.result().asByteArray().isNotEmpty() == true
 * ```
 */
suspend inline fun S3TransferManager.downloadAsByteArray(
    bucket: String,
    key: String,
    crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<ResponseBytes<GetObjectResponse>> =
    downloadAsByteArrayAsync(bucket, key, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 파일로 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param destination 저장할 파일 경로
 * @param builder [DownloadFileRequest.Builder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val target = java.nio.file.Path.of("build/tmp/readme.txt")
 * val result = transferManager.downloadFile("demo-bucket", "docs/readme.txt", target)
 * // result.response().sdkHttpResponse().isSuccessful == true
 * ```
 */
suspend inline fun S3TransferManager.downloadFile(
    bucket: String,
    key: String,
    destination: Path,
    builder: DownloadFileRequest.Builder.() -> Unit = {},
): CompletedFileDownload = downloadFileAsync(bucket, key, destination, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 객체를 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param asyncRequestBody 업로드할 객체
 * @param builder [UploadRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val result = transferManager.upload("demo-bucket", "notes/hello.txt", "hello".toAsyncRequestBody())
 * // result.response().eTag().isNullOrBlank() == false
 * ```
 */
suspend inline fun S3TransferManager.upload(
    bucket: String,
    key: String,
    asyncRequestBody: AsyncRequestBody,
    builder: UploadRequest.Builder.() -> Unit = {},
): CompletedUpload = uploadAsync(bucket, key, asyncRequestBody, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 ByteArray 를 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param content 업로드할 ByteArray
 * @param builder [UploadRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val result = transferManager.uploadByteArray("demo-bucket", "notes/data.bin", byteArrayOf(1, 2, 3))
 * // result.response().eTag().isNullOrBlank() == false
 * ```
 */
suspend inline fun S3TransferManager.uploadByteArray(
    bucket: String,
    key: String,
    content: ByteArray,
    builder: UploadRequest.Builder.() -> Unit = {},
): CompletedUpload = uploadByteArrayAsync(bucket, key, content, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 파일을 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param source 업로드할 파일 경로
 * @param builder [UploadFileRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 *
 * 예제:
 * ```kotlin
 * val source = java.nio.file.Path.of("settings.gradle.kts")
 * val result = transferManager.uploadFile("demo-bucket", "repo/settings.gradle.kts", source)
 * // result.response().eTag().isNullOrBlank() == false
 * ```
 */
suspend inline fun S3TransferManager.uploadFile(
    bucket: String,
    key: String,
    source: Path,
    builder: UploadFileRequest.Builder.() -> Unit = {},
): CompletedFileUpload = uploadFileAsync(bucket, key, source, builder).completionFuture().await()
