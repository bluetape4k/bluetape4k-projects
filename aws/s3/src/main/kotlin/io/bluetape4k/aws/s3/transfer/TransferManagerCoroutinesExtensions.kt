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
 */
suspend inline fun <T: Any> S3TransferManager.downloadSuspending(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<T> =
    download(responseTransformer, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param responseTransformer 응답을 변환할 비동기 transformer
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 */
suspend fun <T: Any> S3TransferManager.downloadSuspending(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    @BuilderInference builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<T> =
    download(bucket, key, responseTransformer, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 ByteArray 로 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 */
suspend inline fun S3TransferManager.downloadAsByteArraySuspending(
    bucket: String,
    key: String,
    @BuilderInference crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): CompletedDownload<ResponseBytes<GetObjectResponse>> =
    downloadAsByteArray(bucket, key, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 파일로 다운로드 받습니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param destination 저장할 파일 경로
 * @param builder [DownloadFileRequest.Builder] 를 구성하는 람다 함수
 * @return 다운로드 완료 결과
 */
suspend inline fun S3TransferManager.downloadFileSuspending(
    bucket: String,
    key: String,
    destination: Path,
    @BuilderInference builder: DownloadFileRequest.Builder.() -> Unit = {},
): CompletedFileDownload =
    downloadFile(bucket, key, destination, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 객체를 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param asyncRequestBody 업로드할 객체
 * @param builder [UploadRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 */
suspend inline fun S3TransferManager.uploadSuspending(
    bucket: String,
    key: String,
    asyncRequestBody: AsyncRequestBody,
    @BuilderInference builder: UploadRequest.Builder.() -> Unit = {},
): CompletedUpload =
    upload(bucket, key, asyncRequestBody, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 ByteArray 를 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param content 업로드할 ByteArray
 * @param builder [UploadRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 */
suspend inline fun S3TransferManager.uploadByteArraySuspending(
    bucket: String,
    key: String,
    content: ByteArray,
    @BuilderInference builder: UploadRequest.Builder.() -> Unit = {},
): CompletedUpload =
    uploadByteArray(bucket, key, content, builder).completionFuture().await()

/**
 * [S3TransferManager]를 이용하여 파일을 S3에 업로드 합니다.
 *
 * @param bucket Bucket name
 * @param key Object key
 * @param source 업로드할 파일 경로
 * @param builder [UploadFileRequest.Builder] 를 구성하는 람다 함수
 * @return 업로드 완료 결과
 */
suspend inline fun S3TransferManager.uploadFileSuspending(
    bucket: String,
    key: String,
    source: Path,
    @BuilderInference builder: UploadFileRequest.Builder.() -> Unit = {},
): CompletedFileUpload =
    uploadFile(bucket, key, source, builder).completionFuture().await()
