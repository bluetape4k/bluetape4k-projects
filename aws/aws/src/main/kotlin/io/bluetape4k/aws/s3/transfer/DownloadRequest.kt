package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.getObjectRequestOf
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.DownloadRequest
import java.nio.file.Path

/**
 * [responseTransformer]를 사용하는 [DownloadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = downloadRequest(AsyncResponseTransformer.toBytes()) { }
 * // result.responseTransformer() != null
 * ```
 */
inline fun <T> downloadRequest(
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> =
    DownloadRequest.builder()
        .apply(builder)
        .responseTransformer(responseTransformer)
        .build()

/**
 * [bucket], [key]와 [responseTransformer] 기반 [DownloadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = downloadRequestOf("demo-bucket", "docs/readme.txt", AsyncResponseTransformer.toBytes())
 * // result.getObjectRequest().key() == "docs/readme.txt"
 * ```
 */
inline fun <T> downloadRequestOf(
    bucket: String,
    key: String,
    responseTransformer: AsyncResponseTransformer<GetObjectResponse, T>,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<T> {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    val request = getObjectRequestOf(bucket, key)

    return downloadRequest(responseTransformer) {
        getObjectRequest(request)
        builder()
    }
}

/**
 * 파일 저장용 [DownloadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val destination = java.nio.file.Path.of("build/tmp/readme.txt")
 * val result = downloadRequestOf("demo-bucket", "docs/readme.txt", destination)
 * // result.getObjectRequest().bucket() == "demo-bucket"
 * ```
 */
inline fun downloadRequestOf(
    bucket: String,
    key: String,
    downloadPath: Path,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<GetObjectResponse> =
    downloadRequestOf(
        bucket,
        key,
        AsyncResponseTransformer.toFile(downloadPath)
    ) {
        builder()
    }

/**
 * ByteArray 응답용 [DownloadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = downloadByteArrayRequestOf("demo-bucket", "docs/readme.txt")
 * // result.getObjectRequest().bucket() == "demo-bucket"
 * ```
 */
inline fun downloadByteArrayRequestOf(
    bucket: String,
    key: String,
    builder: DownloadRequest.UntypedBuilder.() -> Unit = {},
): DownloadRequest<ResponseBytes<GetObjectResponse>> =
    downloadRequestOf(
        bucket,
        key,
        AsyncResponseTransformer.toBytes()
    ) {
        builder()
    }

/**
 * [DownloadFileRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = downloadFileRequest { destination(java.nio.file.Path.of("build/tmp/a.txt")) }
 * // result.destination() != null
 * ```
 */
inline fun downloadFileRequest(
    builder: DownloadFileRequest.Builder.() -> Unit = {},
): DownloadFileRequest =
    DownloadFileRequest.builder().apply(builder).build()

/**
 * 파일 다운로드용 [DownloadFileRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val destination = java.nio.file.Path.of("build/tmp/readme.txt")
 * val result = downloadFileRequestOf("demo-bucket", "docs/readme.txt", destination)
 * // result.destination() == destination
 * ```
 */
inline fun downloadFileRequestOf(
    bucket: String,
    key: String,
    destination: Path,
    builder: DownloadFileRequest.Builder.() -> Unit = {},
): DownloadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")

    return downloadFileRequest {
        getObjectRequest(getObjectRequestOf(bucket, key))
        destination(destination)
        builder()
    }
}

/**
 * [DownloadDirectoryRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = downloadDirectoryRequest { bucket("demo-bucket") }
 * // result.bucket() == "demo-bucket"
 * ```
 */
inline fun downloadDirectoryRequest(
    builder: DownloadDirectoryRequest.Builder.() -> Unit = {},
): DownloadDirectoryRequest =
    DownloadDirectoryRequest.builder().apply(builder).build()

/**
 * 디렉터리 다운로드용 [DownloadDirectoryRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val destination = java.nio.file.Path.of("build/tmp/downloads")
 * val result = downloadDirectoryRequestOf("demo-bucket", destination)
 * // result.destination() == destination
 * ```
 */
inline fun downloadDirectoryRequestOf(
    bucket: String,
    destination: Path,
    builder: DownloadDirectoryRequest.Builder.() -> Unit = {},
): DownloadDirectoryRequest {
    bucket.requireNotBlank("bucket")

    return downloadDirectoryRequest {
        bucket(bucket)
        destination(destination)

        builder()
    }
}
