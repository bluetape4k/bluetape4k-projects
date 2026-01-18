package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.SetSmsAttributesRequest

inline fun SetSmsAttributesRequest(
    builder: SetSmsAttributesRequest.Builder.() -> Unit,
): SetSmsAttributesRequest =
    SetSmsAttributesRequest.builder().apply(builder).build()

inline fun setSmsAttributesRequestOf(
    attributes: Map<String, String>,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: SetSmsAttributesRequest.Builder.() -> Unit = {},
): SetSmsAttributesRequest = SetSmsAttributesRequest {
    attributes(attributes)
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
