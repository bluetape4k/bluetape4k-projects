package io.bluetape4k.aws.sts

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse

/**
 * 현재 AWS 자격 증명의 호출자 신원 정보를 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getCallerIdentityAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = stsAsyncClient.getCallerIdentity()
 * // response.account().isNotBlank() == true
 * ```
 */
suspend fun StsAsyncClient.getDefaultCallerIdentity(): GetCallerIdentityResponse =
    getCallerIdentityAsync().await()

/**
 * IAM 역할을 임시로 맡아(Assume) 임시 자격 증명을 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [assumeRoleAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = stsAsyncClient.assumeRole(
 *     roleArn = "arn:aws:iam::123456789012:role/MyRole",
 *     sessionName = "my-session"
 * )
 * // response.credentials().accessKeyId().isNotBlank() == true
 * ```
 */
suspend fun StsAsyncClient.assumeRole(
    roleArn: String,
    sessionName: String,
    durationSeconds: Int = 3600,
): AssumeRoleResponse =
    assumeRoleAsync(roleArn, sessionName, durationSeconds).await()

/**
 * MFA 인증 기반의 임시 세션 자격 증명을 코루틴으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [getSessionTokenAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = stsAsyncClient.getSessionToken()
 * // response.credentials().accessKeyId().isNotBlank() == true
 * ```
 */
suspend fun StsAsyncClient.getSessionToken(
    durationSeconds: Int = 3600,
): GetSessionTokenResponse =
    getSessionTokenAsync(durationSeconds).await()
