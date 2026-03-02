package io.bluetape4k.aws.s3.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.s3.model.ListBucketsRequest

/**
 * [ListBucketsRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = listBucketsRequest { maxBuckets(50) }
 * // result.maxBuckets() == 50
 * ```
 */
inline fun listBucketsRequest(
    @BuilderInference builder: ListBucketsRequest.Builder.() -> Unit = {},
): ListBucketsRequest =
    ListBucketsRequest.builder().apply(builder).build()

/**
 * 요청 override 설정을 포함한 [ListBucketsRequest]를 생성합니다.
 *
 * 예제:
 * ```kotlin
 * val result = listBucketsRequestOf {
 *     apiCallAttemptTimeout(java.time.Duration.ofSeconds(3))
 * }
 * // result.overrideConfiguration().isPresent == true
 * ```
 */
fun listBucketsRequestOf(
    @BuilderInference configrationBuilder: AwsRequestOverrideConfiguration.Builder.() -> Unit = {},
): ListBucketsRequest =
    listBucketsRequest {
        overrideConfiguration(configrationBuilder)
    }
