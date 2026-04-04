package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import io.bluetape4k.support.requireNotBlank
import java.net.URLEncoder

/**
 * 버킷/키 정보를 받아 URL-encoded copy source를 생성한 뒤 [CopyObjectRequest] 를 생성합니다.
 *
 * ```kotlin
 * val request = copyObjectRequestOf(
 *     srcBucket = "src-bucket",
 *     srcKey = "path/to/src-object.txt",
 *     destBucket = "dest-bucket",
 *     destKey = "path/to/dest-object.txt"
 * )
 * s3Client.copyObject(request)
 * ```
 *
 * @param srcBucket 원본 버킷 이름
 * @param srcKey 원본 객체 키
 * @param destBucket 대상 버킷 이름
 * @param destKey 대상 객체 키
 * @param acl 접근 제어 목록
 * @return [CopyObjectRequest] 인스턴스
 */
inline fun copyObjectRequestOf(
    srcBucket: String,
    srcKey: String,
    destBucket: String,
    destKey: String,
    acl: ObjectCannedAcl? = null,
    crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectRequest {
    srcBucket.requireNotBlank("srcBucket")
    srcKey.requireNotBlank("srcKey")
    destBucket.requireNotBlank("destBucket")
    destKey.requireNotBlank("destKey")

    return CopyObjectRequest {
        this.copySource = URLEncoder.encode("$srcBucket/$srcKey", Charsets.UTF_8)
        this.bucket = destBucket
        this.key = destKey
        this.acl = acl

        builder()
    }
}

/**
 * 이미 구성된 copy source 문자열을 사용해 [CopyObjectRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = copyObjectRequestOf(
 *     copySource = "src-bucket/path/to/src-object.txt",
 *     destBucket = "dest-bucket",
 *     destKey = "path/to/dest-object.txt"
 * )
 * s3Client.copyObject(request)
 * ```
 *
 * @param copySource URL-encoded copy source 문자열 (예: "src-bucket/src-key")
 * @param destBucket 대상 버킷 이름
 * @param destKey 대상 객체 키
 * @param acl 접근 제어 목록
 * @return [CopyObjectRequest] 인스턴스
 */
inline fun copyObjectRequestOf(
    copySource: String,
    destBucket: String,
    destKey: String,
    acl: ObjectCannedAcl? = null,
    crossinline builder: CopyObjectRequest.Builder.() -> Unit = {},
): CopyObjectRequest {
    copySource.requireNotBlank("copySource")
    destBucket.requireNotBlank("destBucket")
    destKey.requireNotBlank("destKey")

    return CopyObjectRequest {
        this.copySource = copySource
        this.bucket = destBucket
        this.key = destKey
        this.acl = acl

        builder()
    }
}
