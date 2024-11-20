package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSmsAttributesRequest

inline fun SetSmsAttributesRequest(
    initializer: SetSmsAttributesRequest.Builder.() -> Unit,
): SetSmsAttributesRequest =
    SetSmsAttributesRequest.builder().apply(initializer).build()

fun setSmsAttributesRequestOf(
    attributes: Map<String, String>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: SetSmsAttributesRequest.Builder.() -> Unit = {},
): SetSmsAttributesRequest = SetSmsAttributesRequest {
    attributes(attributes)
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
