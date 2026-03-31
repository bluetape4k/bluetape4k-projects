package io.bluetape4k.aws.kotlin.sts

import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.getCallerIdentity
import aws.sdk.kotlin.services.sts.getSessionToken
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityResponse
import aws.sdk.kotlin.services.sts.model.GetSessionTokenResponse
import io.bluetape4k.aws.kotlin.sts.model.assumeRoleRequestOf

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
