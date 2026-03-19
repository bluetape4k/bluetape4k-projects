package io.bluetape4k.aws.s3.transfer

import io.bluetape4k.aws.s3.model.putObjectRequestOf
import io.bluetape4k.io.exists
import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import java.io.File
import java.nio.file.Path

/**
 * [UploadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = uploadRequest {
 *     putObjectRequest(putObjectRequestOf("demo-bucket", "notes/a.txt"))
 *     requestBody("hello".toAsyncRequestBody())
 * }
 * // result.putObjectRequest().bucket() == "demo-bucket"
 * ```
 */
inline fun uploadRequest(
    builder: UploadRequest.Builder.() -> Unit,
): UploadRequest =
    UploadRequest.builder().apply(builder).build()

/**
 * [putObjectRequest], [requestBody]로 [UploadRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val put = putObjectRequestOf("demo-bucket", "notes/a.txt")
 * val result = uploadRequestOf(put, "hello".toAsyncRequestBody())
 * // result.requestBody() != null
 * ```
 */
inline fun uploadRequestOf(
    putObjectRequest: PutObjectRequest,
    requestBody: AsyncRequestBody,
    builder: UploadRequest.Builder.() -> Unit = {},
): UploadRequest {
    return uploadRequest {
        putObjectRequest(putObjectRequest)
        requestBody(requestBody)
        builder()
    }
}

/**
 * [UploadFileRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = uploadFileRequest { source(java.nio.file.Path.of("build.gradle.kts")) }
 * // result.source() != null
 * ```
 */
inline fun uploadFileRequest(
    builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest =
    UploadFileRequest.builder().apply(builder).build()

/**
 * 경로 기반 [UploadFileRequest]를 생성합니다.
 *
 * [source]가 존재하지 않으면 [IllegalArgumentException]을 던집니다.
 *
 * 예제:
 * ```kotlin
 * val source = java.nio.file.Path.of("settings.gradle.kts")
 * val result = uploadFileRequestOf("demo-bucket", "repo/settings.gradle.kts", source) { }
 * // result.putObjectRequest().key() == "repo/settings.gradle.kts"
 * ```
 */
inline fun uploadFileRequestOf(
    bucket: String,
    key: String,
    source: Path,
    builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadFileRequest {
        putObjectRequest(putObjectRequestOf(bucket, key))
        source(source)
        builder()
    }
}

/**
 * 파일 기반 [UploadFileRequest]를 생성합니다.
 *
 * [source]가 존재하지 않으면 [IllegalArgumentException]을 던집니다.
 *
 * 예제:
 * ```kotlin
 * val source = java.io.File("settings.gradle.kts")
 * val result = uploadFileRequestOf("demo-bucket", "repo/settings.gradle.kts", source) { }
 * // result.putObjectRequest().bucket() == "demo-bucket"
 * ```
 */
inline fun uploadFileRequestOf(
    bucket: String,
    key: String,
    source: File,
    builder: UploadFileRequest.Builder.() -> Unit,
): UploadFileRequest {
    bucket.requireNotBlank("bucket")
    key.requireNotBlank("key")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadFileRequest {
        putObjectRequest(putObjectRequestOf(bucket, key))
        source(source)
        builder()
    }
}

/**
 * [UploadDirectoryRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = uploadDirectoryRequest {
 *     bucket("demo-bucket")
 *     source(java.nio.file.Path.of("src"))
 * }
 * // result.bucket() == "demo-bucket"
 * ```
 */
inline fun uploadDirectoryRequest(
    builder: UploadDirectoryRequest.Builder.() -> Unit,
): UploadDirectoryRequest =
    UploadDirectoryRequest.builder().apply(builder).build()

/**
 * 디렉터리 업로드용 [UploadDirectoryRequest]를 생성합니다.
 *
 * [source]가 존재하지 않으면 [IllegalArgumentException]을 던집니다.
 *
 * 예제:
 * ```kotlin
 * val source = java.nio.file.Path.of("src")
 * val result = uploadDirectoryRequestOf("demo-bucket", source) { }
 * // result.source() == source
 * ```
 */
inline fun uploadDirectoryRequestOf(
    bucket: String,
    source: Path,
    builder: UploadDirectoryRequest.Builder.() -> Unit,
): UploadDirectoryRequest {
    bucket.requireNotBlank("bucket")
    require(source.exists()) { "source[$source] does not exist." }

    return uploadDirectoryRequest {
        bucket(bucket)
        source(source)
        builder()
    }
}
