package io.bluetape4k.aws.auth

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

const val AWS_LOCAL_ACCESS_KEY = "accesskey"
const val AWS_LOCAL_SECURITY_KEY = "secretkey"

@JvmField
val LocalAwsCredentialsProvider: StaticCredentialsProvider =
    staticCredentialsProviderOf(AWS_LOCAL_ACCESS_KEY, AWS_LOCAL_SECURITY_KEY)

fun awsBasicCredentialsOf(accessKeyId: String, securityAccessKey: String): AwsBasicCredentials =
    AwsBasicCredentials.create(accessKeyId, securityAccessKey)

fun staticCredentialsProviderOf(credentials: AwsBasicCredentials): StaticCredentialsProvider =
    StaticCredentialsProvider.create(credentials)

/**
 * [AwsBasicCredentials]을 제공하는 [StaticCredentialsProvider]를 생성합니다.
 *
 * ```
 * private val credentialsProvider: StaticCredentialsProvider by lazy {
 *      staticCredentialsProviderOf(s3Server.accessKey, s3Server.secretKey)
 * }
 * ```
 * @param accessKeyId        aws access key
 * @param securityAccessKey  aws security key
 * @return [StaticCredentialsProvider] instance
 */
fun staticCredentialsProviderOf(accessKeyId: String, securityAccessKey: String): StaticCredentialsProvider =
    staticCredentialsProviderOf(awsBasicCredentialsOf(accessKeyId, securityAccessKey))
