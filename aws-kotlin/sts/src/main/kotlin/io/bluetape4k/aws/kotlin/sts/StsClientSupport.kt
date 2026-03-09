package io.bluetape4k.aws.kotlin.sts

import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.getCallerIdentity
import aws.sdk.kotlin.services.sts.getSessionToken
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityResponse
import aws.sdk.kotlin.services.sts.model.GetSessionTokenResponse
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.aws.kotlin.sts.model.assumeRoleRequestOf
import io.bluetape4k.utils.ShutdownQueue

/**
 * AWS Kotlin SDK STS 클라이언트를 생성합니다.
 *
 * AWS Security Token Service(STS)는 AWS 리소스에 대한 액세스를 제어할 수 있는
 * 임시 제한 권한 자격 증명을 요청할 수 있는 웹 서비스입니다.
 *
 * 예시:
 * ```kotlin
 * val client = stsClientOf(
 *     endpointUrl = Url.parse("http://localhost:4566"),
 *     region = "us-east-1",
 *     credentialsProvider = myCredentialsProvider
 * )
 * ```
 *
 * @param endpointUrl STS 서비스 엔드포인트 URL. null이면 기본 AWS 엔드포인트를 사용합니다.
 * @param region AWS 리전. null이면 환경 설정에서 자동으로 감지합니다.
 * @param credentialsProvider AWS 인증 정보 제공자. null이면 기본 자격 증명 체인을 사용합니다.
 * @param httpClient HTTP 클라이언트 엔진. 기본값은 [HttpClientEngineProvider.defaultHttpEngine]입니다.
 * @param builder [StsClient.Config.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [StsClient] 인스턴스.
 */
inline fun stsClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: StsClient.Config.Builder.() -> Unit = {},
): StsClient = StsClient {
    endpointUrl?.let { this.endpointUrl = it }
    region?.let { this.region = it }
    credentialsProvider?.let { this.credentialsProvider = it }
    this.httpClient = httpClient

    builder()
}.apply {
    ShutdownQueue.register(this)
}

/**
 * 현재 AWS 자격 증명의 호출자 신원 정보를 반환합니다.
 *
 * ## 동작/계약
 * - 계정 ID, 사용자 ID, ARN 정보를 포함하는 응답을 반환한다.
 *
 * ```kotlin
 * val response = client.getCallerIdentity()
 * // response.account?.isNotBlank() == true
 * ```
 */
suspend fun StsClient.getCallerIdentity(): GetCallerIdentityResponse =
    getCallerIdentity {}

/**
 * IAM 역할을 임시로 맡아(Assume) 임시 자격 증명을 반환합니다.
 *
 * ## 동작/계약
 * - [roleArn]은 맡을 IAM 역할의 ARN이다.
 * - [sessionName]은 세션 이름으로, 감사 로그에 기록된다.
 * - [durationSeconds]는 임시 자격 증명의 유효 시간(초)이다.
 * - [durationSeconds]는 900~43200 범위를 만족해야 하며, 범위를 벗어나면 [IllegalArgumentException]을 던진다.
 *
 * ```kotlin
 * val response = client.assumeRole(
 *     roleArn = "arn:aws:iam::123456789012:role/MyRole",
 *     sessionName = "my-session"
 * )
 * // response.credentials?.accessKeyId?.isNotBlank() == true
 * ```
 */
suspend fun StsClient.assumeRole(
    roleArn: String,
    sessionName: String,
    durationSeconds: Int = 3600,
): AssumeRoleResponse {
    requireValidAssumeRoleDuration(durationSeconds)

    val request = assumeRoleRequestOf(roleArn, sessionName) {
        this.durationSeconds = durationSeconds
    }
    return assumeRole(request)
}

/**
 * MFA 인증 기반의 임시 세션 자격 증명을 반환합니다.
 *
 * ## 동작/계약
 * - [durationSeconds]는 임시 자격 증명의 유효 시간(초)이다.
 * - [durationSeconds]는 900~129600 범위를 만족해야 하며, 범위를 벗어나면 [IllegalArgumentException]을 던진다.
 *
 * ```kotlin
 * val response = client.getSessionToken()
 * // response.credentials?.accessKeyId?.isNotBlank() == true
 * ```
 */
suspend fun StsClient.getSessionToken(
    durationSeconds: Int = 3600,
): GetSessionTokenResponse {
    requireValidSessionTokenDuration(durationSeconds)
    return getSessionToken {
        this.durationSeconds = durationSeconds
    }
}
