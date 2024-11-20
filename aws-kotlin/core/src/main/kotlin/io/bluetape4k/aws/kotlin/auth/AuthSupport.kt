package io.bluetape4k.aws.kotlin.auth

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

const val AWS_LOCAL_ACCESS_KEY = "accesskey"
const val AWS_LOCAL_SECRET_KEY = "secretkey"

/**
 * 로컬 테스트 환경에서 사용할 [StaticCredentialsProvider] instance
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
 * @param accessKeyId      aws access key
 * @param secretAccessKey  aws security key
 * @return [StaticCredentialsProvider] instance
 */
fun staticCredentialsProviderOf(accessKeyId: String, secretAccessKey: String): StaticCredentialsProvider =
    StaticCredentialsProvider.invoke {
        this.accessKeyId = accessKeyId
        this.secretAccessKey = secretAccessKey
    }

/**
 * [Credentials]을 제공하는 [StaticCredentialsProvider]를 생성합니다.
 *
 * @param credentials  [Credentials] instance
 */
fun staticCredentialsProviderOf(credentials: Credentials): StaticCredentialsProvider =
    StaticCredentialsProvider.invoke {
        this.accessKeyId = credentials.accessKeyId
        this.secretAccessKey = credentials.secretAccessKey
    }

/**
 * [Credentials]를 생성합니다.
 *
 * @param accessKeyId      aws access key
 * @param secretAccessKey  aws security key
 */
fun credentialsOf(accessKeyId: String, secretAccessKey: String): Credentials =
    Credentials(accessKeyId, secretAccessKey)
