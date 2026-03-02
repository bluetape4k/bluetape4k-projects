package io.bluetape4k.aws.auth

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * 로컬 테스트 환경에서 사용하는 기본 Access Key 문자열입니다.
 *
 * ## 동작/계약
 * - 로컬 자격 증명 생성 시 `accessKeyId` 기본값으로 사용한다.
 * - 값은 `"accesskey"` 상수로 고정되어 있다.
 *
 * ```kotlin
 * val accessKey = AWS_LOCAL_ACCESS_KEY
 * // accessKey == "accesskey"
 * ```
 */
const val AWS_LOCAL_ACCESS_KEY = "accesskey"

/**
 * 로컬 테스트 환경에서 사용하는 기본 Secret Key 문자열입니다.
 *
 * ## 동작/계약
 * - 로컬 자격 증명 생성 시 `secretAccessKey` 기본값으로 사용한다.
 * - 값은 `"secretkey"` 상수로 고정되어 있다.
 *
 * ```kotlin
 * val secretKey = AWS_LOCAL_SECURITY_KEY
 * // secretKey == "secretkey"
 * ```
 */
const val AWS_LOCAL_SECURITY_KEY = "secretkey"

/**
 * 로컬 기본 키 쌍으로 구성한 [StaticCredentialsProvider]를 제공합니다.
 *
 * ## 동작/계약
 * - `AWS_LOCAL_ACCESS_KEY`, `AWS_LOCAL_SECURITY_KEY`를 사용해 즉시 생성한다.
 * - 동일 인스턴스를 재사용하는 `val` 프로퍼티다.
 *
 * ```kotlin
 * val provider = LocalAwsCredentialsProvider
 * val credentials = provider.resolveCredentials()
 * // credentials.accessKeyId() == AWS_LOCAL_ACCESS_KEY
 * ```
 */
@JvmField
val LocalAwsCredentialsProvider: StaticCredentialsProvider =
    staticCredentialsProviderOf(AWS_LOCAL_ACCESS_KEY, AWS_LOCAL_SECURITY_KEY)

/**
 * Access Key/Secret Key 문자열로 [AwsBasicCredentials]를 생성합니다.
 *
 * ## 동작/계약
 * - [AwsBasicCredentials.create]를 그대로 호출해 새 인스턴스를 반환한다.
 * - 입력 문자열은 변환 없이 자격 증명 필드에 매핑된다.
 *
 * ```kotlin
 * val credentials = awsBasicCredentialsOf("ak", "sk")
 * // credentials.accessKeyId() == "ak"
 * ```
 */
fun awsBasicCredentialsOf(accessKeyId: String, securityAccessKey: String): AwsBasicCredentials =
    AwsBasicCredentials.create(accessKeyId, securityAccessKey)

/**
 * [AwsBasicCredentials]를 감싼 [StaticCredentialsProvider]를 생성합니다.
 *
 * ## 동작/계약
 * - [StaticCredentialsProvider.create]를 호출해 provider를 생성한다.
 * - 전달된 [credentials]를 resolve 결과로 그대로 노출한다.
 *
 * ```kotlin
 * val credentials = awsBasicCredentialsOf("ak", "sk")
 * val provider = staticCredentialsProviderOf(credentials)
 * // provider.resolveCredentials().secretAccessKey() == "sk"
 * ```
 */
fun staticCredentialsProviderOf(credentials: AwsBasicCredentials): StaticCredentialsProvider =
    StaticCredentialsProvider.create(credentials)

/**
 * 키 문자열로 [AwsBasicCredentials]을 만들고 [StaticCredentialsProvider]를 생성합니다.
 *
 * ## 동작/계약
 * - 내부에서 [awsBasicCredentialsOf] 후 [staticCredentialsProviderOf]를 순차 호출한다.
 * - 인자 두 개를 전달하면 즉시 고정 자격 증명 provider를 얻는다.
 *
 * ```kotlin
 * private val credentialsProvider: StaticCredentialsProvider by lazy {
 *      staticCredentialsProviderOf(s3Server.accessKey, s3Server.secretKey)
 * }
 * // credentialsProvider.resolveCredentials().accessKeyId() == s3Server.accessKey
 * ```
 */
fun staticCredentialsProviderOf(accessKeyId: String, securityAccessKey: String): StaticCredentialsProvider =
    staticCredentialsProviderOf(awsBasicCredentialsOf(accessKeyId, securityAccessKey))
