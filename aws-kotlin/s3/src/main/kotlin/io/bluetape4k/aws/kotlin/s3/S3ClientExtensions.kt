package io.bluetape4k.aws.kotlin.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * [S3Client] 를 생성합니다.
 *
 * ```
 * val s3Client = s3ClientOf(
 *    endpointUrl = "http://localhost:4566",
 *    region = "us-west-2",
 *    credentialsProvider = StaticCredentialsProvider { accessKeyId = "test"; secretAccess = "test" }
 * ) {
 *  clientName = "bluetape4k-s3-client"
 * }
 * ```
 *
 * @param endpointUrl S3 엔드포인트 URL
 * @param region AWS 리전
 * @param credentialsProvider AWS 자격 증명 제공자
 * @param configurer [S3Client.Config.Builder] 를 통해 [S3Client.Config] 를 설정합니다.
 * @return [S3Client] 인스턴스
 */
inline fun s3ClientOf(
    endpointUrl: String? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    crossinline configurer: S3Client.Config.Builder.() -> Unit = {},
): S3Client = S3Client {
    endpointUrl?.let { this.endpointUrl = Url.parse(it) }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }

    configurer()
}
