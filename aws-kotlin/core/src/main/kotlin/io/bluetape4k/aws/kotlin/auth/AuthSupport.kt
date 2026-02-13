package io.bluetape4k.aws.kotlin.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import io.bluetape4k.support.requireNotBlank

/**
 * 로컬 테스트 환경에서 사용하는 기본 Access Key 입니다.
 */
const val AWS_LOCAL_ACCESS_KEY = "accesskey"

/**
 * 로컬 테스트 환경에서 사용하는 기본 Secret Key 입니다.
 */
const val AWS_LOCAL_SECRET_KEY = "secretkey"

/**
 * 로컬 테스트 환경에서 사용할 [StaticCredentialsProvider] 인스턴스입니다.
 */
@JvmField
val LocalCredentialsProvider: StaticCredentialsProvider =
    staticCredentialsProviderOf(AWS_LOCAL_ACCESS_KEY, AWS_LOCAL_SECRET_KEY)

/**
 * [Credentials]을 제공하는 [StaticCredentialsProvider]를 생성합니다.
 *
 * ```
 * private val credentialsProvider: StaticCredentialsProvider by lazy {
 *      staticCredentialsProviderOf(s3Server.accessKey, s3Server.secretKey)
 * }
 * ```
 * @param accessKeyId      AWS access key
 * @param secretAccessKey  AWS secret key
 * @return [StaticCredentialsProvider] 인스턴스
 */
fun staticCredentialsProviderOf(accessKeyId: String, secretAccessKey: String): StaticCredentialsProvider {
    return staticCredentialsProviderOf(credentialsOf(accessKeyId, secretAccessKey))
}

/**
 * [Credentials]을 제공하는 [StaticCredentialsProvider]를 생성합니다.
 *
 * @param credentials  [Credentials] 인스턴스
 */
fun staticCredentialsProviderOf(credentials: Credentials): StaticCredentialsProvider =
    StaticCredentialsProvider {
        this.accessKeyId = credentials.accessKeyId
        this.secretAccessKey = credentials.secretAccessKey
    }

/**
 * [Credentials]를 생성합니다.
 *
 * @param accessKeyId      AWS access key
 * @param secretAccessKey  AWS secret key
 */
fun credentialsOf(accessKeyId: String, secretAccessKey: String): Credentials {
    accessKeyId.requireNotBlank("accessKeyId")
    secretAccessKey.requireNotBlank("secretAccessKey")

    return Credentials(accessKeyId, secretAccessKey)
}
