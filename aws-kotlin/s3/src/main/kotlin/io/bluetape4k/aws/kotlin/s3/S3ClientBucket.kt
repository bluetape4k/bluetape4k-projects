package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.listObjectsV2
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.CreateBucketResponse
import aws.sdk.kotlin.services.s3.model.DeleteBucketResponse
import io.bluetape4k.aws.kotlin.s3.model.deleteBucketRequestOf
import io.bluetape4k.aws.kotlin.s3.model.headBucketRequestOf
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank

private val log by lazy { KotlinLogging.logger { } }

/**
 * [bucket]의 버킷이 존재하는지 확인합니다.
 *
 * ```
 * val exists = s3Client.existsBucket("bucket-name")
 * ```
 *
 * @param bucket 버킷 이름
 * @return 버킷이 존재하면 `true`, 존재하지 않으면 `false`
 */
suspend fun S3Client.existsBucket(bucket: String): Boolean {
    return runCatching { headBucket(headBucketRequestOf(bucket)) }.isSuccess
}

/**
 * [bucketName]의 버킷을 생성합니다.
 * [configurer] 를 통해 버킷 생성 설정을 변경할 수 있습니다.
 *
 * ```
 * s3Client.createBucket("bucket-name") {
 *    acl = BucketCannedACL.PRIVATE
 *    region = "us-west-2"
 *    createBucketConfiguration {
 *        locationConstraint = BucketLocationConstraint.US_WEST_2
 *    }
 * }
 * ```
 * @param bucketName 버킷 이름
 * @param configurer [CreateBucketRequest.Builder] 를 통해 [CreateBucketRequest] 를 설정합니다.
 * @return [CreateBucketResponse] 인스턴스
 */
suspend fun S3Client.createBucket(
    bucketName: String,
    configurer: CreateBucketRequest.Builder.() -> Unit = {},
): CreateBucketResponse {
    bucketName.requireNotBlank("bucketName")
    return createBucket {
        bucket = bucketName
        configurer()
    }
}

/**
 * [bucketName]의 버킷이 존재하지 않으면 생성합니다.
 *
 * ```
 * s3Client.ensureBucket("bucket-name")
 * ```
 *
 * @param bucketName 버킷 이름
 */
suspend fun S3Client.ensureBucketExists(
    bucketName: String,
    configurer: CreateBucketRequest.Builder.() -> Unit = {},
) {
    bucketName.requireNotBlank("bucketName")

    if (!existsBucket(bucketName)) {
        createBucket(bucketName, configurer)
    }
}


/**
 * [bucket] 버킷의 모든 Object를 삭제하고, Bucket도 삭제합니다.
 *
 * @param bucket 삭제할 버킷 이름
 * @return [DeleteBucketResponse] 인스턴스
 */
suspend fun S3Client.forceDeleteBucket(
    bucket: String,
): DeleteBucketResponse {
    bucket.requireNotBlank("bucketName")

    // 버킷 내 모든 Object 삭제 (listObjectsV2는 최대 1000개만 반환하므로, 모든 Object를 삭제하기 위해 반복)
    log.debug { "버킷의 모든 Object를 삭제합니다. bucket=$bucket" }
    do {
        val keys = listObjectsV2 { this.bucket = bucket }.contents?.mapNotNull { it.key } ?: emptyList()

        if (keys.isNotEmpty()) {
            deleteAll(bucket, keys)
        }
    } while (keys.isNotEmpty())

    // 버킷 삭제
    log.debug { "버킷을 삭제합니다. bucket=$bucket" }
    return deleteBucket(deleteBucketRequestOf(bucket))
}
