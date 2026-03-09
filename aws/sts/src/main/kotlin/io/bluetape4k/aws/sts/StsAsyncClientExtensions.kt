package io.bluetape4k.aws.sts

import io.bluetape4k.aws.sts.model.assumeRoleRequestOf
import io.bluetape4k.aws.sts.model.getCallerIdentityRequest
import io.bluetape4k.aws.sts.model.getSessionTokenRequest
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse
import java.util.concurrent.CompletableFuture

/**
 * 현재 AWS 자격 증명의 호출자 신원 정보를 비동기로 반환합니다.
 *
 * ## 동작/계약
 * - 계정 ID, 사용자 ID, ARN 정보를 포함하는 응답을 비동기로 반환한다.
 *
 * ```kotlin
 * val response = stsAsyncClient.getCallerIdentityAsync().join()
 * // response.account().isNotBlank() == true
 * ```
 */
fun StsAsyncClient.getCallerIdentityAsync(): CompletableFuture<GetCallerIdentityResponse> {
    val request = getCallerIdentityRequest {}
    return getCallerIdentity(request)
}

/**
 * IAM 역할을 임시로 맡아(Assume) 임시 자격 증명을 비동기로 반환합니다.
 *
 * ## 동작/계약
 * - [roleArn]은 맡을 IAM 역할의 ARN이다.
 * - [sessionName]은 세션 이름으로, 감사 로그에 기록된다.
 * - [durationSeconds]는 임시 자격 증명의 유효 시간(초)이다.
 * - [durationSeconds]는 900~43200 범위여야 하며, 범위를 벗어나면 [IllegalArgumentException]을 던진다.
 *
 * ```kotlin
 * val response = stsAsyncClient.assumeRoleAsync(
 *     roleArn = "arn:aws:iam::123456789012:role/MyRole",
 *     sessionName = "my-session"
 * ).join()
 * // response.credentials().accessKeyId().isNotBlank() == true
 * ```
 */
fun StsAsyncClient.assumeRoleAsync(
    roleArn: String,
    sessionName: String,
    durationSeconds: Int = 3600,
): CompletableFuture<AssumeRoleResponse> {
    requireValidAssumeRoleDuration(durationSeconds)

    val request = assumeRoleRequestOf(roleArn, sessionName) {
        durationSeconds(durationSeconds)
    }
    return assumeRole(request)
}

/**
 * MFA 인증 기반의 임시 세션 자격 증명을 비동기로 반환합니다.
 *
 * ## 동작/계약
 * - [durationSeconds]는 임시 자격 증명의 유효 시간(초)이다.
 * - [durationSeconds]는 900~129600 범위여야 하며, 범위를 벗어나면 [IllegalArgumentException]을 던진다.
 *
 * ```kotlin
 * val response = stsAsyncClient.getSessionTokenAsync().join()
 * // response.credentials().accessKeyId().isNotBlank() == true
 * ```
 */
fun StsAsyncClient.getSessionTokenAsync(
    durationSeconds: Int = 3600,
): CompletableFuture<GetSessionTokenResponse> {
    requireValidSessionTokenDuration(durationSeconds)

    val request = getSessionTokenRequest {
        durationSeconds(durationSeconds)
    }
    return getSessionToken(request)
}
