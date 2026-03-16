package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest

/**
 * DSL 블록으로 [CreatePlatformEndpointRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `platformApplicationArn`, `token` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = createPlatformEndpointRequest {
 *     platformApplicationArn("arn:aws:sns:ap-northeast-2:123456:app/GCM/my-app")
 *     token("device-token-xyz")
 * }
 * ```
 */
inline fun createPlatformEndpointRequest(
    builder: CreatePlatformEndpointRequest.Builder.() -> Unit,
): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest.builder().apply(builder).build()

/**
 * 플랫폼 애플리케이션 ARN과 디바이스 토큰으로 [CreatePlatformEndpointRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [platformApplicationArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [token]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [customUserData]가 non-null이면 엔드포인트에 사용자 데이터로 설정된다.
 *
 * ```kotlin
 * val req = createPlatformEndpointRequestOf(
 *     platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456:app/GCM/my-app",
 *     token = "device-token-xyz"
 * )
 * // req.platformApplicationArn().isNotBlank() == true
 * ```
 */
inline fun createPlatformEndpointRequestOf(
    platformApplicationArn: String,
    token: String,
    customUserData: String? = null,
    attributes: Map<String, String>? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: CreatePlatformEndpointRequest.Builder.() -> Unit = {},
): CreatePlatformEndpointRequest {
    platformApplicationArn.requireNotBlank("platformApplicationArn")
    token.requireNotBlank("token")

    return createPlatformEndpointRequest {
        platformApplicationArn(platformApplicationArn)
        token(token)
        customUserData?.run { customUserData(this) }
        attributes?.run { attributes(this) }
        overrideConfiguration?.run { overrideConfiguration(this) }

        builder()
    }
}
