package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSmsAttributesRequest

/**
 * DSL 블록으로 [SetSmsAttributesRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `attributes` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = setSmsAttributesRequest {
 *     attributes(mapOf("DefaultSMSType" to "Transactional"))
 * }
 * ```
 */
inline fun setSmsAttributesRequest(
    builder: SetSmsAttributesRequest.Builder.() -> Unit,
): SetSmsAttributesRequest =
    SetSmsAttributesRequest.builder().apply(builder).build()

/**
 * SMS 속성 맵으로 [SetSmsAttributesRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [attributes] 맵은 SMS 전송 설정(예: `DefaultSMSType`, `MonthlySpendLimit`)을 포함한다.
 *
 * ```kotlin
 * val req = setSmsAttributesRequestOf(
 *     attributes = mapOf("DefaultSMSType" to "Transactional")
 * )
 * // req.attributes()["DefaultSMSType"] == "Transactional"
 * ```
 */
inline fun setSmsAttributesRequestOf(
    attributes: Map<String, String>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: SetSmsAttributesRequest.Builder.() -> Unit = {},
): SetSmsAttributesRequest = setSmsAttributesRequest {
    attributes(attributes)
    overrideConfiguration?.let { overrideConfiguration(it) }

    builder()
}
