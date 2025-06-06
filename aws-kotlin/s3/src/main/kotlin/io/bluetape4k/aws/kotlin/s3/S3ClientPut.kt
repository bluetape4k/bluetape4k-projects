package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.fromFile
import io.bluetape4k.aws.kotlin.s3.model.putObjectRequestOf
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.io.exists
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.io.File
import java.nio.file.Path

/**
 * [bucketName]의 [key]에 객체를 저장합니다.
 *
 * ```
 * val response = s3Client.put("bucket-name", "key") {
 *    this.body = ByteStream.fromString("Hello, World!")
 *    this.contentType = "text/plain"
 *    this.contentLength = this.content.size.toLong()
 *    this.metadata = mapOf("key" to "value")
 *    this.acl = "public-read"
 *    this.cacheControl = "max-age=3600"
 * }
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param configurer [PutObjectRequest.Builder] 를 통해 [PutObjectRequest] 를 설정합니다.
 * @return [PutObjectResponse] 인스턴스
 */
suspend inline fun S3Client.put(
    bucketName: String,
    key: String,
    body: ByteStream? = null,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline configurer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    val request = putObjectRequestOf(bucketName, key, body, metadata, acl, contentType, configurer)
    return putObject(request)
}

/**
 * [bucketName]의 [key]에 [bytes]를 저장합니다.
 *
 * ```
 * val response = s3Client.putAsByteArray("bucket-name", "key", byteArrayOf(1, 2, 3, 4))
 * ```
 *
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param bytes 저장할 바이트 배열
 * @param metadata 메타데이터
 * @param configurer [PutObjectRequest.Builder] 를 통해 [PutObjectRequest] 를 설정합니다.
 * @return [PutObjectResponse] 인스턴스
 */
suspend inline fun S3Client.putFromByteArray(
    bucketName: String,
    key: String,
    bytes: ByteArray,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline configurer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    return put(bucketName, key, ByteStream.fromBytes(bytes), metadata, acl, contentType, configurer)
}

/**
 * [bucketName]의 [key]에 [text]를 저장합니다.
 *
 * ```
 * val response = s3Client.putAsString("bucket-name", "key", "Hello World!")
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param text 저장할 문자열
 * @param metadata 메타데이터
 * @param configurer [PutObjectRequest.Builder] 를 통해 [PutObjectRequest] 를 설정합니다.
 * @return [PutObjectResponse] 인스턴스
 */
suspend inline fun S3Client.putFromString(
    bucketName: String,
    key: String,
    text: String,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline configurer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    return put(bucketName, key, ByteStream.fromString(text), metadata, acl, contentType, configurer)
}

/**
 * [bucketName]의 [key]에 [file]의 정보를 저장합니다.
 *
 * ```
 * val response = s3Client.putFromFile("bucket-name", "key", File("test.txt"))
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param file 저장할 파일
 * @param metadata 메타데이터
 * @param configurer [PutObjectRequest.Builder] 를 통해 [PutObjectRequest] 를 설정합니다.
 * @return [PutObjectResponse] 인스턴스
 * @throws IllegalArgumentException 파일이 존재하지 않을 경우
 * @see putFromPath
 */
suspend inline fun S3Client.putFromFile(
    bucketName: String,
    key: String,
    file: File,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline configurer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    require(file.exists()) { "File not found: $file" }
    return put(bucketName, key, ByteStream.fromFile(file), metadata, acl, contentType, configurer)
}

/**
 * [bucketName]의 [key]에 [path]의 파일 정보를 저장합니다.
 *
 * ```
 * val response = s3Client.putFromPath("bucket-name", "key", Paths.get("test.txt"))
 * ```
 * @param bucketName 버킷 이름
 * @param key 객체 키
 * @param path 저장할 파일 경로
 * @param metadata 메타데이터
 * @param configurer [PutObjectRequest.Builder] 를 통해 [PutObjectRequest] 를 설정합니다.
 * @return [PutObjectResponse] 인스턴스
 * @throws IllegalArgumentException 파일이 존재하지 않을 경우
 * @see putFromFile
 */
suspend inline fun S3Client.putFromPath(
    bucketName: String,
    key: String,
    path: Path,
    metadata: Map<String, String>? = null,
    acl: ObjectCannedAcl? = null,
    contentType: String? = null,
    crossinline configurer: PutObjectRequest.Builder.() -> Unit = {},
): PutObjectResponse {
    require(path.exists()) { "File not found: $path" }
    return put(bucketName, key, ByteStream.fromFile(path.toFile()), metadata, acl, contentType, configurer)
}

/**
 * 여러 객체를 동시에 저장합니다.
 *
 * ```
 * val response = s3Client.putAll(concurrency = 10, putRequest1, putRequest2, putRequest3).toList()
 * ```
 *
 * @param concurrency 동시에 실행할 요청 수
 * @param putRequests [PutObjectRequest] 목록
 * @return [PutObjectResponse] 목록
 * @see put
 */
fun S3Client.putAll(
    concurrency: Int = DEFAULT_CONCURRENCY,
    vararg putRequests: PutObjectRequest,
): Flow<PutObjectResponse> {
    return putRequests.asFlow()
        .async {
            putObject(it)
        }
}
