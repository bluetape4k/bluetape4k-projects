package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.putObjectRequestOf
import io.bluetape4k.aws.s3.model.toAsyncRequestBody
import io.bluetape4k.io.exists
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.Download
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import software.amazon.awssdk.transfer.s3.model.FileDownload
import software.amazon.awssdk.transfer.s3.model.FileUpload
import software.amazon.awssdk.transfer.s3.model.Upload
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.nio.file.Path

private val log = KotlinLogging.logger { }

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param T 반환 타입
 * @param responseTransformer 응답을 변환할 transformer
 * @param builder [DownloadRequest] 를 구성하는 람다 함수
 * @return [Download] 인스턴스
 *
 * 예제:
 * ```kotlin
 * val download = transferManager.downloadAsync(AsyncResponseTransformer.toBytes())
 * // download.completionFuture().isDone == false
 * ```
 */
inline fun <T: Any> S3TransferManager.downloadAsync(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): Download<T> = download(downloadRequest(responseTransformer, builder))

/**
 * [S3TransferManager]를 이용하여 S3 Object 를 다운로드 받습니다.
 *
 * @param T 반환 타입
 * @param bucket bucket name
 * @param key key
 * @param responseTransformer 응답을 변환할 비동기 transformer
 * @param builder [GetObjectRequest.Builder] 를 구성하는 람다 함수
 * @return 다운로드한 S3 Object
 *
 * 예제:
 * ```kotlin
 * val download = transferManager.downloadAsync("demo-bucket", "docs/readme.txt", AsyncResponseTransformer.toBytes())
 * // download.completionFuture().isCompletedExceptionally == false
 * ```
 */
inline fun <T: Any> S3TransferManager.downloadAsync(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): Download<T> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = downloadRequestOf(bucket, key, responseTransformer, builder)
    return download(request)
}

/**
 * S3 Object 를 ByteArray 로 다운로드 받습니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param builder [DownloadRequest.UntypedBuilder] 를 구성하는 람다 함수
 * @return 다운로드한 S3 Object
 *
 * 예제:
 * ```kotlin
 * val download = transferManager.downloadAsByteArrayAsync("demo-bucket", "docs/readme.txt")
 * // download.completionFuture().isDone == false
 * ```
 */
inline fun S3TransferManager.downloadAsByteArrayAsync(
    bucket: String,
    key: String,
    crossinline builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): Download<ResponseBytes<GetObjectResponse>> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request =
        downloadRequestOf(bucket, key, AsyncResponseTransformer.toBytes(), builder)

    return download(request)
}

/**
 * S3 Object 를 파일로 다운로드 받습니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param destination 저장할 파일 경로
 * @param builder [DownloadFileRequest.Builder] 를 구성하는 람다 함수.
 * 기본 요청(`bucket`, `key`, `destination`) 적용 후 추가 설정을 덮어쓸 수 있습니다.
 * @return 다운로드한 S3 Object
 *
 * 예제:
 * ```kotlin
 * val target = java.nio.file.Path.of("build/tmp/readme.txt")
 * val download = transferManager.downloadFileAsync("demo-bucket", "docs/readme.txt", target)
 * // download.completionFuture().isDone == false
 * ```
 */
inline fun S3TransferManager.downloadFileAsync(
    bucket: String,
    key: String,
    destination: Path,
    builder: DownloadFileRequest.Builder.() -> Unit = {},
): FileDownload {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = downloadFileRequestOf(bucket, key, destination, builder)
    return downloadFile(request)
}

/**
 * [S3TransferManager]를 이용하여 객체를 S3에 업로드 합니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param asyncRequestBody 업로드할 객체
 * @param builder  추가로 구성할 [UploadRequest.Builder]를 구성하는 람다 함수
 * @return [Upload] 인스턴스
 *
 * 예제:
 * ```kotlin
 * val upload = transferManager.uploadAsync("demo-bucket", "notes/hello.txt", "hello".toAsyncRequestBody())
 * // upload.completionFuture().isDone == false
 * ```
 */
inline fun S3TransferManager.uploadAsync(
    bucket: String,
    key: String,
    asyncRequestBody: AsyncRequestBody,
    builder: UploadRequest.Builder.() -> Unit = {},
): Upload {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request =
        uploadRequest {
            putObjectRequest(putObjectRequestOf(bucket, key))
            requestBody(asyncRequestBody)
            builder()
        }
    return upload(request)
}

/**
 * ByteArray 를 S3에 업로드 합니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param content 업로드할 ByteArray
 * @param builder [UploadRequest.Builder] 를 구성하는 람다 함수
 * @return [Upload] 인스턴스
 *
 * 예제:
 * ```kotlin
 * val upload = transferManager.uploadByteArrayAsync("demo-bucket", "notes/data.bin", byteArrayOf(1, 2, 3))
 * // upload.completionFuture().isDone == false
 * ```
 */
inline fun S3TransferManager.uploadByteArrayAsync(
    bucket: String,
    key: String,
    content: ByteArray,
    builder: UploadRequest.Builder.() -> Unit = {},
): Upload {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request =
        uploadRequest {
            putObjectRequest(putObjectRequestOf(bucket, key))
            requestBody(content.toAsyncRequestBody())
            builder()
        }

    return upload(request)
}

/**
 * 파일을 S3에 업로드 합니다.
 *
 * @param bucket bucket name
 * @param key key
 * @param source 업로드할 파일 경로
 * @param builder [UploadFileRequest.Builder] 를 구성하는 람다 함수
 * @return [FileUpload] 인스턴스
 * @throws IllegalArgumentException 파일이 존재하지 않을 경우
 *
 * 예제:
 * ```kotlin
 * val source = java.nio.file.Path.of("settings.gradle.kts")
 * val upload = transferManager.uploadFileAsync("demo-bucket", "repo/settings.gradle.kts", source)
 * // upload.completionFuture().isDone == false
 * ```
 */
inline fun S3TransferManager.uploadFileAsync(
    bucket: String,
    key: String,
    source: Path,
    builder: UploadFileRequest.Builder.() -> Unit = {},
): FileUpload {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    require(source.exists()) { "File not found. source=$source" }

    val request =
        uploadFileRequest {
            putObjectRequest(putObjectRequestOf(bucket, key))
            source(source)
            builder()
        }

    return uploadFile(request)
}
