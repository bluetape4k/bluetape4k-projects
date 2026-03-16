package io.bluetape4k.aws.kotlin.sts.model

import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 [AssumeRoleRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `roleArn`, `roleSessionName` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = assumeRoleRequest {
 *     roleArn = "arn:aws:iam::123456789012:role/MyRole"
 *     roleSessionName = "my-session"
 * }
 * ```
 */
inline fun assumeRoleRequest(
    @BuilderInference crossinline builder: AssumeRoleRequest.Builder.() -> Unit,
): AssumeRoleRequest =
    AssumeRoleRequest { builder() }

/**
 * role ARN과 세션 이름으로 [AssumeRoleRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [roleArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [sessionName]이 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = assumeRoleRequestOf(
 *     roleArn = "arn:aws:iam::123456789012:role/MyRole",
 *     sessionName = "my-session"
 * )
 * // req.roleArn == "arn:aws:iam::123456789012:role/MyRole"
 * // req.roleSessionName == "my-session"
 * ```
 */
inline fun assumeRoleRequestOf(
    roleArn: String,
    sessionName: String,
    @BuilderInference crossinline builder: AssumeRoleRequest.Builder.() -> Unit = {},
): AssumeRoleRequest {
    roleArn.requireNotBlank("roleArn")
    sessionName.requireNotBlank("sessionName")

    return assumeRoleRequest {
        this.roleArn = roleArn
        this.roleSessionName = sessionName

        builder()
    }
}
