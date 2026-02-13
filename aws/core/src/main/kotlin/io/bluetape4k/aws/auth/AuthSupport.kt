package io.bluetape4k.aws.auth

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * 로컬 테스트 환경에서 사용하는 기본 Access Key 입니다.
 */
const val AWS_LOCAL_ACCESS_KEY = "accesskey"

/**
 * 로컬 테스트 환경에서 사용하는 기본 Secret Key 입니다.
 */
const val AWS_LOCAL_SECURITY_KEY = "secretkey"

@JvmField
val LocalAwsCredentialsProvider: StaticCredentialsProvider =
    staticCredentialsProviderOf(AWS_LOCAL_ACCESS_KEY, AWS_LOCAL_SECURITY_KEY)

/**
 * [AwsBasicCredentials] 인스턴스를 생성합니다.
 */
fun awsBasicCredentialsOf(accessKeyId: String, securityAccessKey: String): AwsBasicCredentials =
    AwsBasicCredentials.create(accessKeyId, securityAccessKey)

/**
 * [AwsBasicCredentials]를 감싼 [StaticCredentialsProvider]를 생성합니다.
 */
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
 * @param accessKeyId        AWS Access Key
 * @param securityAccessKey  AWS Secret Key
 * @return [StaticCredentialsProvider] 인스턴스
 */
fun staticCredentialsProviderOf(accessKeyId: String, securityAccessKey: String): StaticCredentialsProvider =
    staticCredentialsProviderOf(awsBasicCredentialsOf(accessKeyId, securityAccessKey))
